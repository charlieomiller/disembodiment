package net.charl.disembodiment.client.model;

import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;

public final class DematCrystalModel {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new net.minecraft.resources.ResourceLocation("disembodiment", "demat_crystal"), "main");

    public static LayerDefinition createBodyLayer(int texW, int texH) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Matches vanilla layout: glass at (0,0), cube at (32,0) when tex is 64×32
        root.addOrReplaceChild("glass",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4, -4, -4, 8, 8, 8),
                PartPose.ZERO);

        root.addOrReplaceChild("cube",
                CubeListBuilder.create().texOffs(texW / 2, 0).addBox(-4, -4, -4, 8, 8, 8),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, texW, texH);
    }

    private DematCrystalModel() {}
}
