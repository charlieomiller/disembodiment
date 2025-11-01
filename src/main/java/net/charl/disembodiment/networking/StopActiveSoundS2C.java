package net.charl.disembodiment.networking;

public record StopActiveSoundS2C(int fadeOutTicks) {
    public static void encode(StopActiveSoundS2C m, net.minecraft.network.FriendlyByteBuf b){ b.writeVarInt(m.fadeOutTicks);}
    public static StopActiveSoundS2C decode(net.minecraft.network.FriendlyByteBuf b){ return new StopActiveSoundS2C(b.readVarInt());}
    public static void handle(StopActiveSoundS2C m, java.util.function.Supplier<net.minecraftforge.network.NetworkEvent.Context> ctx){
        ctx.get().enqueueWork(() -> net.charl.disembodiment.client.sound.PlayerDematLoopController.stop(m.fadeOutTicks));
        ctx.get().setPacketHandled(true);
    }
}