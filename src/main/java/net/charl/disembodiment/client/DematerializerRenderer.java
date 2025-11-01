package net.charl.disembodiment.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.charl.disembodiment.block.entity.DematerializerBlockEntity;
import net.charl.disembodiment.client.model.DematCrystalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

public class DematerializerRenderer implements BlockEntityRenderer<DematerializerBlockEntity> {
    //private static final ResourceLocation TEX = new ResourceLocation("disembodiment", "textures/entity/demat_crystal.png");
    private static final ResourceLocation DEMAT_CRYSTAL_TEX =
            new ResourceLocation("disembodiment", "textures/entity/demat_crystal.png");
    private static final RenderType TYPE = RenderType.entityCutoutNoCull(DEMAT_CRYSTAL_TEX);

    // Parts baked from our layer
    private final ModelPart glass;
    private final ModelPart cube;

    public DematerializerRenderer(BlockEntityRendererProvider.Context ctx) {
        ModelPart root = ctx.bakeLayer(DematCrystalModel.LAYER);
        this.glass = root.getChild("glass");
        this.cube  = root.getChild("cube");
    }

    @Override
    public void render(DematerializerBlockEntity be, float partialTicks, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        // Only show when at least one player is ACTIVE (adjust if you want BUFFER visible too)
        //if (!be.hasAnyActivePlayers()) return; // add a simple helper in your BE

        ps.pushPose();

        // Position to the top-center of your 12px-tall block
        final float relativeOrigin = 21f / 16.0f;
        ps.translate(0.5, relativeOrigin, 0.5);

        // Tiny bob like vanilla crystal
        long gt = be.getLevel() != null ? be.getLevel().getGameTime() : 0L;
        float t = gt + partialTicks;
        float bob = bobOffset(t); // ~[-0.4, 0.0] like vanilla (shifted down a bit)
        ps.translate(0.0, 0.25 + bob * 0.25, 0.0);

        // Spin continuously
        float spin = (t * 3.0f) % 360.0f;
        ps.mulPose(Axis.YP.rotationDegrees(spin));

        // --- scale it down so it’s a cute topper ---
        // Vanilla scales UP by 2.0 then 0.875 twice; we’ll start small.
        float s = 0.65f; // try 0.45–0.6 to taste
        ps.scale(s, s, s);

        // Render passes (match EndCrystalRenderer’s transforms, minus base)
        var vc = buf.getBuffer(TYPE);

        // First shell with a diagonal tilt
        ps.pushPose();
        ps.mulPose(new Quaternionf().setAngleAxis((float)Math.PI / 3f, SIN_45, 0.0f, SIN_45));
        this.glass.render(ps, vc, light, OverlayTexture.NO_OVERLAY);

        // Second shell, slightly smaller, spin again
        ps.scale(0.875f, 0.875f, 0.875f);
        ps.mulPose(new Quaternionf().setAngleAxis((float)Math.PI / 3f, SIN_45, 0.0f, SIN_45));
        ps.mulPose(Axis.YP.rotationDegrees(spin));
        this.glass.render(ps, vc, light, OverlayTexture.NO_OVERLAY);

        // Inner cube
        ps.scale(0.875f, 0.875f, 0.875f);
        ps.mulPose(new Quaternionf().setAngleAxis((float)Math.PI / 3f, SIN_45, 0.0f, SIN_45));
        ps.mulPose(Axis.YP.rotationDegrees(spin));
        this.cube.render(ps, vc, light, OverlayTexture.NO_OVERLAY);

        ps.popPose();
        ps.popPose();
    }

    private static final float SIN_45 = (float)Math.sin(Math.PI / 4.0);

    private static float bobOffset(float f) {
        // Copied from EndCrystalRenderer.getY (same “breathing” curve)
        float s = Mth.sin(f * 0.2F) / 2.0F + 0.5F;
        s = (s * s + s) * 0.25F;
        return s - 1.4F; // ~-1.4..-1.0; we scale it down above
    }

    @Override public boolean shouldRenderOffScreen(DematerializerBlockEntity be) { return false; }
}
