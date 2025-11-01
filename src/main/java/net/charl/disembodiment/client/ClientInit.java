package net.charl.disembodiment.client;

import net.charl.disembodiment.Disembodiment;
import net.charl.disembodiment.block.entity.ModBlockEntities;
import net.charl.disembodiment.client.model.DematCrystalModel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Disembodiment.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientInit {

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions e) {
        e.registerLayerDefinition(DematCrystalModel.LAYER, () -> DematCrystalModel.createBodyLayer(64, 32));
    }

    @SubscribeEvent
    public static void registerBERs(EntityRenderersEvent.RegisterRenderers e) {
        e.registerBlockEntityRenderer(ModBlockEntities.DEMATERIALIZER_BE.get(), DematerializerRenderer::new);
    }

    private ClientInit() {}
}
