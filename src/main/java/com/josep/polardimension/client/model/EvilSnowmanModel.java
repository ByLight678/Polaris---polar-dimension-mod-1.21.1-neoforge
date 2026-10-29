package com.josep.polardimension.client.model;

import com.josep.polardimension.entity.EvilSnowmanEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

/** Generated from art/evil_snowman by tools/export_evil_snowman_model.cjs. */
public final class EvilSnowmanModel extends HierarchicalModel<EvilSnowmanEntity> {
    private final ModelPart root, rig, head, rightArm, leftArm;

    public EvilSnowmanModel(ModelPart root) {
        this.root = root;
        rig = root.getChild("rig_root"); head = rig.getChild("head");
        rightArm = rig.getChild("right_arm"); leftArm = rig.getChild("left_arm");
    }

    @Override
    public ModelPart root() { return root; }

    @Override
    public void setupAnim(EvilSnowmanEntity entity, float swing, float amount, float age, float yaw, float pitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        head.yRot = Mth.clamp(yaw, -50.0F, 50.0F) * Mth.DEG_TO_RAD;
        head.xRot = pitch * Mth.DEG_TO_RAD;
        float step = Mth.sin(swing * 0.6662F) * amount;
        rig.zRot += step * 0.06F;
        rightArm.xRot += step * 0.4F; leftArm.xRot -= step * 0.4F;
        // Arms are authored sideways. Rotate toward the target when pursuing,
        // then swing the icicle's parent arm for the standard melee animation.
        if (entity.isAggressive()) {
            rightArm.yRot -= 1.1F; leftArm.yRot += 1.1F;
            rightArm.xRot -= 0.4F; leftArm.xRot -= 0.4F;
        }
        float strike = Mth.sin(attackTime * Mth.PI);
        rightArm.xRot -= strike * 1.5F;
        rightArm.zRot += strike * 0.4F;
        leftArm.zRot += Mth.sin(age * 0.09F) * 0.04F;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition rig_root = root.addOrReplaceChild("rig_root", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 24F, 0F, 0F, 0F, 0F));
        PartDefinition body = rig_root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, -12F, 0F, 0F, 0F, 0F));
        body.addOrReplaceChild("lower_snow_geo", CubeListBuilder.create().texOffs(0, 0).addBox(-6F, 0F, -6F, 12F, 12F, 12F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        body.addOrReplaceChild("upper_snow_geo", CubeListBuilder.create().texOffs(49, 0).addBox(-5F, -10F, -5F, 10F, 10F, 10F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        body.addOrReplaceChild("scarf_geo", CubeListBuilder.create().texOffs(0, 33).addBox(-6F, -10F, -6F, 12F, 2F, 12F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        body.addOrReplaceChild("scarf_tail_geo", CubeListBuilder.create().texOffs(49, 33).addBox(2F, -9F, -7F, 3F, 8F, 1F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition head = rig_root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, -22F, 0F, 0F, 0F, 0F));
        head.addOrReplaceChild("head_geo", CubeListBuilder.create().texOffs(90, 0).addBox(-4F, -8F, -4F, 8F, 8F, 8F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        head.addOrReplaceChild("carrot_geo", CubeListBuilder.create().texOffs(0, 25).addBox(-1F, -4F, -8F, 2F, 2F, 4F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        head.addOrReplaceChild("hat_brim_geo", CubeListBuilder.create().texOffs(58, 33).addBox(-5F, -9F, -5F, 10F, 1F, 10F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        head.addOrReplaceChild("hat_crown_geo", CubeListBuilder.create().texOffs(99, 33).addBox(-3F, -14F, -3F, 6F, 5F, 6F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition right_arm = rig_root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offsetAndRotation(-5F, -19F, 0F, 0F, 0F, 0F));
        right_arm.addOrReplaceChild("right_twig_geo", CubeListBuilder.create().texOffs(13, 25).addBox(-11F, 0F, -1F, 11F, 2F, 2F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        right_arm.addOrReplaceChild("right_grip_geo", CubeListBuilder.create().texOffs(40, 25).addBox(-11F, -1F, -2F, 3F, 4F, 3F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        right_arm.addOrReplaceChild("icicle_base_geo", CubeListBuilder.create().texOffs(0, 48).addBox(-11F, 1F, -3F, 3F, 6F, 3F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        right_arm.addOrReplaceChild("icicle_middle_geo", CubeListBuilder.create().texOffs(13, 48).addBox(-10.5F, 7F, -2.5F, 2F, 5F, 2F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        right_arm.addOrReplaceChild("icicle_tip_geo", CubeListBuilder.create().texOffs(22, 48).addBox(-10F, 12F, -2F, 1F, 3F, 1F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition left_arm = rig_root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offsetAndRotation(5F, -19F, 0F, 0F, 0F, 0F));
        left_arm.addOrReplaceChild("left_twig_geo", CubeListBuilder.create().texOffs(53, 25).addBox(0F, 0F, -1F, 10F, 2F, 2F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        left_arm.addOrReplaceChild("left_finger_geo", CubeListBuilder.create().texOffs(78, 25).addBox(8F, -4F, -1F, 2F, 5F, 2F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        left_arm.addOrReplaceChild("left_thumb_geo", CubeListBuilder.create().texOffs(87, 25).addBox(5F, -2F, -1F, 2F, 3F, 2F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        return LayerDefinition.create(mesh, 128, 128);
    }
}
