package net.charl.disembodiment.block.entity;

import net.charl.disembodiment.Disembodiment;
import net.charl.disembodiment.config.ModConfigs;
import net.charl.disembodiment.networking.ModNetworking;
import net.charl.disembodiment.networking.StartActiveSoundS2C;
import net.charl.disembodiment.networking.StopActiveSoundS2C;
import net.charl.disembodiment.networking.TimerSyncS2C;
import net.charl.disembodiment.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.*;

public class DematerializerBlockEntity extends BlockEntity {
    private static final int DEFAULT_TICKS_PER_SEC = 20;

    private static class PlayerTimer {
        int bufferTicks; // amount of ticks before player is dematerialized after shilling Inkor
        int mainTicks; // amount of ticks player is dematerialized for
        int mainTotal; // total ticks at the start of ACTIVE phase
        boolean active; // false = buffer countdown, true = main countdown
        @Nullable PlayerRestoreConditions restore; // pos, gamemode, etc to load whenever dematerialization ends
    }

    private final Map<UUID, PlayerTimer> timers = new HashMap<>();

    public DematerializerBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.DEMATERIALIZER_BE.get(), pPos, pBlockState);
    }

    public boolean canAcceptFuel(UUID playerId) {
        PlayerTimer t = timers.get(playerId);
        return t == null || !t.active;
    }

    public void addInkorForPlayer(UUID playerId) {
        PlayerTimer t = timers.computeIfAbsent(playerId, id -> new PlayerTimer());

        int ticksToAddToMain = ModConfigs.SERVER.secondsPerInkor.get() * DEFAULT_TICKS_PER_SEC;
        if (ticksToAddToMain > 0) { t.mainTicks += ticksToAddToMain; }

        int ticksToAddToBuffer = ModConfigs.SERVER.secondsBeforeDematerialization.get() * DEFAULT_TICKS_PER_SEC;
        if (ticksToAddToBuffer > 0) { t.bufferTicks = ticksToAddToBuffer; }

        setChanged();
    }

    public boolean hasAnyActivePlayers() {
        for (var e : timers.values()) {
            if (e.active) {
                return true;
            }
        }
        return false;
    }

    public void onBroken(ServerLevel sl) {
        List<UUID> dematerializedPlayers = new ArrayList<>();
        for (Map.Entry<UUID, PlayerTimer> e : timers.entrySet()) {
            if (e.getValue().active) { dematerializedPlayers.add(e.getKey()); }
        }

        boolean kill = ModConfigs.SERVER.killDematerializedOnBreak.get();
        for (UUID id : dematerializedPlayers) {
            ServerPlayer sp = sl.getServer().getPlayerList().getPlayer(id);
            if (sp == null) { continue; }
            if (kill) { killDematerialized(sp, sl); }
            else {
                PlayerTimer t = timers.get(id);
                if (t != null && t.restore != null) {
                    t.restore.restore(sp, sl.getServer()); // what an unfortunate naming scheme
                }
            }
            ModNetworking.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp),
                    new TimerSyncS2C(TimerSyncS2C.Phase.NONE, 0, 0)
            );
            timers.remove(id);
        }

        List<UUID> bufferPlayers = new ArrayList<>();
        for (Map.Entry<UUID, PlayerTimer> e : timers.entrySet()) {
            if (e.getValue().bufferTicks > 0 && !e.getValue().active) { bufferPlayers.add(e.getKey()); }
        }
        for (UUID id : bufferPlayers) {
            ServerPlayer sp = sl.getServer().getPlayerList().getPlayer(id);
            if (sp == null) { continue; }
            ModNetworking.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp),
                    new TimerSyncS2C(TimerSyncS2C.Phase.NONE, 0, 0)
            );
            Holder<SoundEvent> sound = ModSounds.DEMATERIALIZER_START_BUFFER.getHolder().orElseThrow(); // Need to use Holder<SoundEvent> for ClientSoundPacket constructor
            ResourceLocation soundId = sound.value().getLocation();
            sp.connection.send(new ClientboundStopSoundPacket(soundId, SoundSource.BLOCKS)); // Avoids overlapping buffer noise
            timers.remove(id);
        }

        setChanged();
    }

    private static final ResourceKey<DamageType> DEMATERIALIZED_KEY =
            ResourceKey.create(Registries.DAMAGE_TYPE,
                    new ResourceLocation(Disembodiment.MOD_ID, "dematerialized"));

    public void killDematerialized(ServerPlayer sp, ServerLevel sl) {
        // Set player to survival so they can actually take damage
        sp.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        sp.connection.send(new ClientboundGameEventPacket(
                ClientboundGameEventPacket.CHANGE_GAME_MODE,
                GameType.SURVIVAL.getId()
        ));
        sp.onUpdateAbilities();

        // Build custom damage source (purely for custom death message)
        Holder<DamageType> type = sl.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(DEMATERIALIZED_KEY);

        // Apply max damage in hopes that it kills the player (sp.kill doesn't allow for custom death messages :() (funny face)
        DamageSource dmgSrc = new DamageSource(type);
        sp.hurt(dmgSrc, Float.MAX_VALUE);System.out.println("KILLING " + sp.getName());
    }

    // hot mama this tick logic is a mess. i see two major improvements that COULD happen
    // 1) helper functions. it is silly how many things i write out multiple times for no earthly reason
    // 2) theoretically i think this logic could all be ran once every second instead of once every tick
    // another thing that has come up later. to avoid issues that come with the chunk containing the dematerializer unloading,
    // i really ought to have made all the ticking and timing logic using some separated global ticker instead of sitting inside the block entity
    // it isn't a big enough deal for me to want to bother fixing, but maybe if i revisit this mod i'll rewrite using a diff system from the ground up
    public void tick(Level pLevel, BlockPos pPos, BlockState pState) {
        if (pLevel.isClientSide) { return; }
        if (timers.isEmpty()) { return; }

        boolean changed = false;
        Iterator<Map.Entry<UUID, PlayerTimer>> it = timers.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<UUID, PlayerTimer> e = it.next();
            PlayerTimer t = e.getValue();
            UUID playerId = e.getKey();

            if (!t.active) { // Buffer State
                if (t.bufferTicks > 0) {
                    if (t.bufferTicks == ModConfigs.SERVER.secondsBeforeDematerialization.get() * DEFAULT_TICKS_PER_SEC) {
                        System.out.println("BUFFER STARTED");
                        if (pLevel instanceof ServerLevel sl) { // Pattern matching casts sl equal to pLevel as type ServerLevel
                            ServerPlayer sp = sl.getServer().getPlayerList().getPlayer(playerId);
                            if (sp != null) {
                                // Send timer update to player for HUD update
                                int total = ModConfigs.SERVER.secondsBeforeDematerialization.get() * DEFAULT_TICKS_PER_SEC;
                                ModNetworking.CHANNEL.send(
                                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp),
                                        new TimerSyncS2C(TimerSyncS2C.Phase.BUFFER, t.bufferTicks, total)
                                );
                                // Sound player appropriately
                                Holder<SoundEvent> sound = ModSounds.DEMATERIALIZER_START_BUFFER.getHolder().orElseThrow(); // Need to use Holder<SoundEvent> for ClientSoundPacket constructor
                                ResourceLocation soundId = sound.value().getLocation();
                                sp.connection.send(new ClientboundStopSoundPacket(soundId, SoundSource.BLOCKS)); // Avoids overlapping buffer noise
                                sp.connection.send(
                                        new ClientboundSoundPacket(
                                                sound, SoundSource.BLOCKS, pPos.getX(), pPos.getY(), pPos.getZ(), 1f, 1f, 0)
                                );
                            }
                        }
                    }
                    t.bufferTicks--;
                    changed = true;
                    if (t.bufferTicks <= 0 && t.mainTicks > 0) {
                        t.active = true;
                        changed = true;
                        if (t.mainTotal <= 0) {
                            t.mainTotal = t.mainTicks;
                        }
                        // trigger start effects here
                        if (pLevel instanceof ServerLevel sl) { // Pattern matching casts sl equal to pLevel as type ServerLevel
                            ServerPlayer sp = sl.getServer().getPlayerList().getPlayer(playerId);
                            if (sp != null) {
                                // Capture player's pos, dimension, etc. Then switch to spectator
                                t.restore = PlayerRestoreConditions.capture(sp);
                                sp.stopRiding(); // Going into spectator while riding boat/horse/whatnot weird crud
                                if (sp.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) {
                                    sp.gameMode.changeGameModeForPlayer(GameType.SPECTATOR); // Just this isn't enough

                                    sp.connection.send(new ClientboundGameEventPacket( // So we need this
                                            ClientboundGameEventPacket.CHANGE_GAME_MODE,
                                            GameType.SPECTATOR.getId()
                                    ));

                                    sp.onUpdateAbilities(); // And this to ensure that the player is ACTUALLY in spectator

                                    // Sound player appropriately
                                    var snd = ModSounds.PLAYER_DEMATERIALIZATION_LOOP.getId(); // ResourceLocation of your .ogg SoundEvent
                                    ModNetworking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                                            new StartActiveSoundS2C(snd, 1f, 20)); // 1s fade-in (20 ticks)
                                }
                                //System.out.println("Server thinks " + sp.getGameProfile().getName() + " + is now " + sp.gameMode.getGameModeForPlayer());
                                // Send timer update to player for HUD update
                                t.mainTotal = t.mainTicks;
                                ModNetworking.CHANNEL.send(
                                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp),
                                        new TimerSyncS2C(TimerSyncS2C.Phase.ACTIVE, t.mainTicks, t.mainTotal)
                                );
                            }
                        }
                    } //else { System.out.println(playerId + " HAS " + t.bufferTicks + " BUFFER TICKS REMAINING"); }
                } else {
                    if (t.mainTicks <= 0) { it.remove(); changed = true; } // clean up entries that have no remaining buffer or main ticks
                }
            }
            else if (t.active) { // Dematerialization State
                if (t.mainTicks > 0) {
                    t.mainTicks--;
                    changed = true;
                    if (pLevel instanceof ServerLevel sl && t.mainTicks % DEFAULT_TICKS_PER_SEC == 0) { // Only send once per second instead of per tick
                        ServerPlayer sp = sl.getServer().getPlayerList().getPlayer(playerId);
                        if (sp != null) {
                            ModNetworking.CHANNEL.send(
                                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp),
                                    new TimerSyncS2C(TimerSyncS2C.Phase.ACTIVE, t.mainTicks, t.mainTotal)
                            );
                        }
                    }

                    if (t.mainTicks == 0) {
                        t.active = false;
                        changed = true;
                        if (pLevel instanceof ServerLevel sl) {
                            ServerPlayer sp = sl.getServer().getPlayerList().getPlayer(playerId);
                            if (sp != null) {
                                ModNetworking.CHANNEL.send(
                                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp),
                                        new TimerSyncS2C(TimerSyncS2C.Phase.NONE, -0, 0)
                                );
                            }
                        }


                        it.remove();
                        // trigger end effects here
                        if (pLevel instanceof ServerLevel sl) { // Pattern matching casts sl equal to pLevel as type ServerLevel
                            ServerPlayer sp = sl.getServer().getPlayerList().getPlayer(playerId);
                            if (sp != null) {
                                t.restore.restore(sp, sl.getServer()); // t.restore is the PlayerRestoreConditions in the PlayerTimer, restore() reverts everything

                                ModNetworking.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp),
                                        new StopActiveSoundS2C(20)); // 1s fade-out
                            }
                        }
                        t.restore = null;
                    } else { System.out.println(playerId + " HAS " + t.mainTicks + " MAIN TICKS REMAINING"); }
                } else { t.active = false; it.remove(); changed = true; } // Should never be called
            }
        }

        if (changed) setChanged();
    }

    // junk for persistence

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        super.saveAdditional(pTag);
        ListTag list = new ListTag();

        for (Map.Entry<UUID, PlayerTimer> e : timers.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Player", e.getKey());
            entry.putInt("Buffer", e.getValue().bufferTicks);
            entry.putInt("Main", e.getValue().mainTicks);
            entry.putInt("Total", e.getValue().mainTotal);
            entry.putBoolean("Active", e.getValue().active);
            if (e.getValue().restore != null) {
                entry.put("Restore", e.getValue().restore.toTag());
            }
            list.add(entry);
        }
        pTag.put("PlayerTimers", list);
    }

    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        timers.clear();

        if (pTag.contains("PlayerTimers", Tag.TAG_LIST)) {
            ListTag list = pTag.getList("PlayerTimers", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                PlayerTimer pt = new PlayerTimer();
                pt.bufferTicks  = Math.max(0, entry.getInt("Buffer"));
                pt.mainTicks    = Math.max(0, entry.getInt("Main"));
                pt.mainTotal    = entry.contains("Total") ? entry.getInt("Total") : pt.mainTicks;
                pt.active       = entry.getBoolean("Active");
                if (entry.contains("Restore")) {
                    pt.restore  = PlayerRestoreConditions.fromTag(entry.getCompound("Restore"));
                }
                UUID id = entry.getUUID("Player");

                if (pt.bufferTicks > 0 || pt.mainTicks > 0) { timers.put(id, pt); }
            }
        }
    }


}