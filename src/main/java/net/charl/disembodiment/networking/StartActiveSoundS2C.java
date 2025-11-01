package net.charl.disembodiment.networking;

import net.charl.disembodiment.client.sound.PlayerDematLoopController;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public record StartActiveSoundS2C(net.minecraft.resources.ResourceLocation soundId, float volume, int fadeInTicks) {
    public static void encode(StartActiveSoundS2C m, net.minecraft.network.FriendlyByteBuf b) {
        b.writeResourceLocation(m.soundId); b.writeFloat(m.volume); b.writeVarInt(m.fadeInTicks);
    }
    public static StartActiveSoundS2C decode(net.minecraft.network.FriendlyByteBuf b) {
        return new StartActiveSoundS2C(b.readResourceLocation(), b.readFloat(), b.readVarInt());
    }
    public static void handle(StartActiveSoundS2C m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var ev = ForgeRegistries.SOUND_EVENTS.getValue(m.soundId);
            if (ev != null) {
                PlayerDematLoopController.start(ev, m.volume, m.fadeInTicks);
                System.out.println("SHITS WORKIN. YEA " + m.soundId);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}