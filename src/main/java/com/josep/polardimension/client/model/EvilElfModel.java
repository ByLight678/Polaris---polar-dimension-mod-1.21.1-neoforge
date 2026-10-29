package com.josep.polardimension.client.model;

import com.josep.polardimension.entity.EvilElfEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

/** Generated from art/evil_elf by tools/export_evil_elf_model.cjs. */
public final class EvilElfModel extends HierarchicalModel<EvilElfEntity> {
    private final ModelPart root, head, rightArm, leftArm, rightLeg, leftLeg;

    public EvilElfModel(ModelPart root) {
        this.root = root; head = root.getChild("head");
        rightArm = root.getChild("right_arm"); leftArm = root.getChild("left_arm");
        rightLeg = root.getChild("right_leg"); leftLeg = root.getChild("left_leg");
    }

    @Override public ModelPart root() { return root; }

    @Override
    public void setupAnim(EvilElfEntity entity, float swing, float amount, float age, float yaw, float pitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        head.yRot = yaw * Mth.DEG_TO_RAD; head.xRot = pitch * Mth.DEG_TO_RAD;
        rightLeg.xRot = Mth.cos(swing * 0.6662F) * 1.4F * amount;
        leftLeg.xRot = Mth.cos(swing * 0.6662F + Mth.PI) * 1.4F * amount;
        rightArm.xRot = leftLeg.xRot * 0.75F; leftArm.xRot = rightLeg.xRot * 0.75F;
        if (entity.isAggressive()) {
            rightArm.xRot = -1.1F + leftLeg.xRot * 0.2F;
            leftArm.xRot = -1.1F + rightLeg.xRot * 0.2F;
        }
        rightArm.xRot -= Mth.sin(attackTime * Mth.PI) * 1.3F;
        rightArm.zRot += Mth.cos(age * 0.09F) * 0.03F;
        leftArm.zRot -= Mth.cos(age * 0.09F) * 0.03F;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 10F, 0F, 0F, 0F, 0F));
        body.addOrReplaceChild("tunic_geo", CubeListBuilder.create().texOffs(25, 0).addBox(-3F, 0F, -2F, 6F, 7F, 4F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 10F, 0F, 0F, 0F, 0F));
        head.addOrReplaceChild("head_geo", CubeListBuilder.create().texOffs(0, 0).addBox(-3F, -6F, -3F, 6F, 6F, 6F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        head.addOrReplaceChild("right_ear_geo", CubeListBuilder.create().texOffs(29, 13).addBox(-2F, -1F, -1F, 2F, 2F, 1F), PartPose.offsetAndRotation(-3F, -3F, 0F, 0F, 0F, 0.349066F));
        head.addOrReplaceChild("left_ear_geo", CubeListBuilder.create().texOffs(29, 13).addBox(0F, -1F, -1F, 2F, 2F, 1F), PartPose.offsetAndRotation(3F, -3F, 0F, 0F, 0F, -0.349066F));
        head.addOrReplaceChild("hat_brim_geo", CubeListBuilder.create().texOffs(0, 24).addBox(-4F, -7F, -3.5F, 8F, 1F, 7F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        head.addOrReplaceChild("hat_base_geo", CubeListBuilder.create().texOffs(31, 24).addBox(-3F, -9F, -2.5F, 6F, 2F, 5F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        head.addOrReplaceChild("hat_middle_geo", CubeListBuilder.create().texOffs(0, 33).addBox(-2F, -11F, -1.5F, 4F, 2F, 3F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        head.addOrReplaceChild("hat_tip_geo", CubeListBuilder.create().texOffs(15, 33).addBox(-1F, -3F, -1F, 2F, 3F, 2F), PartPose.offsetAndRotation(0F, -10F, 0F, 0F, 0F, 0.610865F));
        head.addOrReplaceChild("hat_bell_geo", CubeListBuilder.create().texOffs(24, 33).addBox(1F, -13F, -1F, 2F, 2F, 2F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offsetAndRotation(-4.5F, 11F, 0F, 0F, 0F, 0F));
        right_arm.addOrReplaceChild("right_arm_geo", CubeListBuilder.create().texOffs(46, 0).addBox(-1.5F, -1F, -1.5F, 3F, 8F, 3F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offsetAndRotation(4.5F, 11F, 0F, 0F, 0F, 0F));
        left_arm.addOrReplaceChild("left_arm_geo", CubeListBuilder.create().texOffs(46, 0).addBox(-1.5F, -1F, -1.5F, 3F, 8F, 3F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offsetAndRotation(-1.5F, 17F, 0F, 0F, 0F, 0F));
        right_leg.addOrReplaceChild("right_leg_geo", CubeListBuilder.create().texOffs(0, 13).addBox(-1.5F, 0F, -1.5F, 3F, 7F, 3F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        right_leg.addOrReplaceChild("right_boot_geo", CubeListBuilder.create().texOffs(13, 13).addBox(-1.5F, 5F, -3F, 3F, 2F, 4.5F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offsetAndRotation(1.5F, 17F, 0F, 0F, 0F, 0F));
        left_leg.addOrReplaceChild("left_leg_geo", CubeListBuilder.create().texOffs(0, 13).addBox(-1.5F, 0F, -1.5F, 3F, 7F, 3F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        left_leg.addOrReplaceChild("left_boot_geo", CubeListBuilder.create().texOffs(13, 13).addBox(-1.5F, 5F, -3F, 3F, 2F, 4.5F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }
}
