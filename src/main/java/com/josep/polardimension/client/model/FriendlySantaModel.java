package com.josep.polardimension.client.model;

import com.josep.polardimension.entity.FriendlySantaEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

/** Generated from art/giant_santa by tools/export_friendly_santa_model.cjs. */
public final class FriendlySantaModel extends HierarchicalModel<FriendlySantaEntity> {
    private final ModelPart root, head, rightArm, leftArm, rightLeg, leftLeg;

    public FriendlySantaModel(ModelPart root) {
        this.root = root; head = root.getChild("rig_root").getChild("head");
        rightArm = root.getChild("rig_root").getChild("right_arm"); leftArm = root.getChild("rig_root").getChild("left_arm");
        rightLeg = root.getChild("rig_root").getChild("right_leg"); leftLeg = root.getChild("rig_root").getChild("left_leg");
    }

    @Override public ModelPart root() { return root; }

    @Override
    public void setupAnim(FriendlySantaEntity entity, float swing, float amount, float age, float yaw, float pitch) {
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
        PartDefinition rig_root = root.addOrReplaceChild("rig_root", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 24F, 0F, 0F, 0F, 0F));
        PartDefinition body = rig_root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, -30F, 0F, 0F, 0F, 0F));
        body.addOrReplaceChild("coat_geo", CubeListBuilder.create().texOffs(0, 0).addBox(-10F, -6F, -6F, 20F, 20F, 12F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition head = rig_root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, -36F, 0F, 0F, 0F, 0F));
        head.addOrReplaceChild("head_geo", CubeListBuilder.create().texOffs(0, 33).addBox(-6F, -10F, -6F, 12F, 10F, 12F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        head.addOrReplaceChild("beard_upper_geo", CubeListBuilder.create().texOffs(79, 73).addBox(-6F, -4F, -8F, 12F, 5F, 3F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        head.addOrReplaceChild("beard_middle_geo", CubeListBuilder.create().texOffs(0, 85).addBox(-5F, 1F, -8F, 10F, 3F, 3F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        head.addOrReplaceChild("beard_tip_geo", CubeListBuilder.create().texOffs(27, 85).addBox(-3F, 4F, -8F, 6F, 2F, 3F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        head.addOrReplaceChild("nose_geo", CubeListBuilder.create().texOffs(46, 85).addBox(-1F, -6F, -9F, 2F, 2F, 3F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        head.addOrReplaceChild("hat_brim_geo", CubeListBuilder.create().texOffs(0, 56).addBox(-7F, -12F, -7F, 14F, 2F, 14F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        head.addOrReplaceChild("hat_base_geo", CubeListBuilder.create().texOffs(57, 56).addBox(-6F, -15F, -6F, 12F, 3F, 12F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        head.addOrReplaceChild("hat_tip_geo", CubeListBuilder.create().texOffs(54, 73).addBox(-3F, -4F, -3F, 6F, 4F, 6F), PartPose.offsetAndRotation(0F, -14F, 0F, 0F, 0F, 0.436332F));
        head.addOrReplaceChild("hat_pompom_geo", CubeListBuilder.create().texOffs(110, 73).addBox(3F, -19F, -2F, 4F, 4F, 4F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition right_arm = rig_root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offsetAndRotation(-13F, -34F, 0F, 0F, 0F, 0F));
        right_arm.addOrReplaceChild("right_sleeve_geo", CubeListBuilder.create().texOffs(65, 0).addBox(-3F, -2F, -4F, 6F, 17F, 8F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        right_arm.addOrReplaceChild("right_cuff_geo", CubeListBuilder.create().texOffs(0, 73).addBox(-3F, 15F, -4F, 6F, 3F, 8F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        right_arm.addOrReplaceChild("right_hand_geo", CubeListBuilder.create().texOffs(29, 73).addBox(-3F, 18F, -3F, 6F, 4F, 6F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition left_arm = rig_root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offsetAndRotation(13F, -34F, 0F, 0F, 0F, 0F));
        left_arm.addOrReplaceChild("left_sleeve_geo", CubeListBuilder.create().texOffs(65, 0).addBox(-3F, -2F, -4F, 6F, 17F, 8F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        left_arm.addOrReplaceChild("left_cuff_geo", CubeListBuilder.create().texOffs(0, 73).addBox(-3F, 15F, -4F, 6F, 3F, 8F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        left_arm.addOrReplaceChild("left_hand_geo", CubeListBuilder.create().texOffs(29, 73).addBox(-3F, 18F, -3F, 6F, 4F, 6F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition right_leg = rig_root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offsetAndRotation(-5F, -16F, 0F, 0F, 0F, 0F));
        right_leg.addOrReplaceChild("right_leg_geo", CubeListBuilder.create().texOffs(94, 0).addBox(-4F, -1F, -4F, 8F, 11F, 8F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        right_leg.addOrReplaceChild("right_boot_geo", CubeListBuilder.create().texOffs(49, 33).addBox(-4F, 10F, -7F, 8F, 6F, 12F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition left_leg = rig_root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offsetAndRotation(5F, -16F, 0F, 0F, 0F, 0F));
        left_leg.addOrReplaceChild("left_leg_geo", CubeListBuilder.create().texOffs(94, 0).addBox(-4F, -1F, -4F, 8F, 11F, 8F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        left_leg.addOrReplaceChild("left_boot_geo", CubeListBuilder.create().texOffs(49, 33).addBox(-4F, 10F, -7F, 8F, 6F, 12F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        return LayerDefinition.create(mesh, 128, 128);
    }
}
