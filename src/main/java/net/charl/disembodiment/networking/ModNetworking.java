package net.charl.disembodiment.networking;

import net.charl.disembodiment.Disembodiment;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetworking {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL =
            NetworkRegistry.ChannelBuilder
                    .named(new ResourceLocation(Disembodiment.MOD_ID, "main"))
                    .networkProtocolVersion(() -> PROTOCOL)
                    .clientAcceptedVersions(PROTOCOL::equals)
                    .serverAcceptedVersions(PROTOCOL::equals)
                    .simpleChannel();

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++,
                TimerSyncS2C.class,
                TimerSyncS2C::encode,
                TimerSyncS2C::decode,
                TimerSyncS2C::handle);
        CHANNEL.registerMessage(id++,
                StartActiveSoundS2C.class,
                StartActiveSoundS2C::encode,
                StartActiveSoundS2C::decode,
                StartActiveSoundS2C::handle);
        CHANNEL.registerMessage(id++,
                StopActiveSoundS2C.class,
                StopActiveSoundS2C::encode,
                StopActiveSoundS2C::decode,
                StopActiveSoundS2C::handle);

    }

    private ModNetworking() {} // Ensure this class isn't instantiated
}
