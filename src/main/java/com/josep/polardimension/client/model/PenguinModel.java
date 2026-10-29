package com.josep.polardimension.client.model;

import com.josep.polardimension.entity.PenguinEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

/** Generated from art/penguin by tools/export_penguin_model.cjs. */
public final class PenguinModel extends HierarchicalModel<PenguinEntity> {
    private final ModelPart root, rig, head, rightFoot, leftFoot, rightFlipper, leftFlipper;

    public PenguinModel(ModelPart root) {
        this.root = root;
        rig = root.getChild("rig_root"); head = rig.getChild("head");
        rightFoot = rig.getChild("right_foot"); leftFoot = rig.getChild("left_foot");
        rightFlipper = rig.getChild("right_flipper"); leftFlipper = rig.getChild("left_flipper");
    }

    @Override
    public ModelPart root() { return root; }

    @Override
    public void setupAnim(PenguinEntity entity, float swing, float amount, float age, float yaw, float pitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        head.yRot = Mth.clamp(yaw, -45.0F, 45.0F) * Mth.DEG_TO_RAD;
        head.xRot = pitch * Mth.DEG_TO_RAD;
        float step = Mth.cos(swing * 1.3F) * amount;
        rightFoot.xRot = step; leftFoot.xRot = -step;
        rig.zRot = step * 0.12F;
        float flap = entity.isInWater() ? Mth.sin(age * 0.5F) * 0.5F : step * 0.2F;
        rightFlipper.zRot += flap; leftFlipper.zRot -= flap;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition rig_root = root.addOrReplaceChild("rig_root", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 24F, 0F, 0F, 0F, 0F));
        PartDefinition body = rig_root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, -8F, 0F, 0F, 0F, 0F));
        body.addOrReplaceChild("body_geo", CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -4F, -3F, 8F, 10F, 6F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition head = rig_root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, -12F, 0F, 0F, 0F, 0F));
        head.addOrReplaceChild("head_geo", CubeListBuilder.create().texOffs(29, 0).addBox(-3F, -6F, -3F, 6F, 6F, 6F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        head.addOrReplaceChild("beak_geo", CubeListBuilder.create().texOffs(0, 17).addBox(-2F, -3F, -6F, 4F, 2F, 3F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition right_flipper = rig_root.addOrReplaceChild("right_flipper", CubeListBuilder.create(), PartPose.offsetAndRotation(-4F, -11F, 0F, 0F, 0F, -0.20944F));
        right_flipper.addOrReplaceChild("right_flipper_geo", CubeListBuilder.create().texOffs(15, 17).addBox(-2F, -1F, -2F, 2F, 8F, 4F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition left_flipper = rig_root.addOrReplaceChild("left_flipper", CubeListBuilder.create(), PartPose.offsetAndRotation(4F, -11F, 0F, 0F, 0F, 0.20944F));
        left_flipper.addOrReplaceChild("left_flipper_geo", CubeListBuilder.create().texOffs(15, 17).addBox(0F, -1F, -2F, 2F, 8F, 4F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition right_foot = rig_root.addOrReplaceChild("right_foot", CubeListBuilder.create(), PartPose.offsetAndRotation(-2F, -2F, 0F, 0F, 0F, 0F));
        right_foot.addOrReplaceChild("right_foot_geo", CubeListBuilder.create().texOffs(28, 17).addBox(-1.5F, 0F, -5F, 3F, 2F, 5F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition left_foot = rig_root.addOrReplaceChild("left_foot", CubeListBuilder.create(), PartPose.offsetAndRotation(2F, -2F, 0F, 0F, 0F, 0F));
        left_foot.addOrReplaceChild("left_foot_geo", CubeListBuilder.create().texOffs(28, 17).addBox(-1.5F, 0F, -5F, 3F, 2F, 5F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition tail = rig_root.addOrReplaceChild("tail", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, -4F, 3F, 0F, 0F, 0F));
        tail.addOrReplaceChild("tail_geo", CubeListBuilder.create().texOffs(0, 30).addBox(-2F, -1F, 0F, 4F, 2F, 3F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }
}
