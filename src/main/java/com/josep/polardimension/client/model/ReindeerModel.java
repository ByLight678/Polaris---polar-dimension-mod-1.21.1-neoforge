package com.josep.polardimension.client.model;

import com.josep.polardimension.entity.ReindeerEntity;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

/** Generated from art/reindeer by tools/export_reindeer_model.cjs. */
public final class ReindeerModel extends HierarchicalModel<ReindeerEntity> {
    private final ModelPart root, rig, body, head, neck, tail, rightFront, leftFront, rightHind, leftHind, saddle, armor;
    private final ModelPart[] antlers;

    public ReindeerModel(ModelPart root) {
        this.root = root;
        rig = root.getChild("rig_root"); body = rig.getChild("body"); head = rig.getChild("head");
        neck = rig.getChild("neck"); tail = rig.getChild("tail"); saddle = body.getChild("saddle"); armor = body.getChild("horse_armor");
        rightFront = rig.getChild("right_front_leg"); leftFront = rig.getChild("left_front_leg");
        rightHind = rig.getChild("right_hind_leg"); leftHind = rig.getChild("left_hind_leg");
        antlers = new ModelPart[] {head.getChild("right_antler_beam_geo"), head.getChild("right_antler_branch_geo"),
                head.getChild("right_antler_tine_geo"), head.getChild("left_antler_beam_geo"),
                head.getChild("left_antler_branch_geo"), head.getChild("left_antler_tine_geo")};
    }

    @Override
    public ModelPart root() { return root; }

    @Override
    public void setupAnim(ReindeerEntity entity, float swing, float amount, float age, float yaw, float pitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        var minecraft = Minecraft.getInstance();
        boolean riderView = minecraft.player != null && minecraft.player.getVehicle() == entity
                && minecraft.options.getCameraType().isFirstPerson();
        for (ModelPart antler : antlers) antler.visible = !riderView;
        saddle.visible = entity.isSaddled();
        armor.visible = false;
        head.yRot = Mth.clamp(yaw, -30.0F, 30.0F) * Mth.DEG_TO_RAD;
        head.xRot = pitch * Mth.DEG_TO_RAD;
        neck.yRot = head.yRot * 0.35F;
        float forward = Mth.cos(swing * 0.6662F) * 1.1F * amount;
        rightFront.xRot = leftHind.xRot = forward;
        leftFront.xRot = rightHind.xRot = -forward;
        tail.zRot = Mth.sin(age * 0.12F) * 0.1F;
        float partial = age - entity.tickCount;
        float eating = entity.getEatAnim(partial);
        neck.xRot += eating * 0.7F;
        head.xRot += eating * 0.6F;
        head.y += eating * 10.0F;
        head.z -= eating * 4.0F;
        rig.xRot -= entity.getStandAnim(partial) * 0.15F;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
        boolean equipped = saddle.visible;
        saddle.visible = false;
        root.render(pose, buffer, light, overlay, color);
        saddle.visible = equipped;
        if (equipped) {
            pose.pushPose();
            rig.translateAndRotate(pose); body.translateAndRotate(pose);
            saddle.render(pose, buffer, light, overlay, 0xFF905C34);
            pose.popPose();
        }
    }

    public void renderArmor(PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
        pose.pushPose();
        rig.translateAndRotate(pose); body.translateAndRotate(pose);
        // Keep vanilla horse-armor body UVs; stretch the barding to this model's longer torso.
        pose.scale(1.01F, 1.01F, 24.0F / 22.0F);
        armor.visible = true;
        armor.render(pose, buffer, light, overlay, color);
        armor.visible = false;
        pose.popPose();
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition rig_root = root.addOrReplaceChild("rig_root", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 24F, 0F, 0F, 0F, 0F));
        PartDefinition body = rig_root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, -20F, 0F, 0F, 0F, 0F));
        body.addOrReplaceChild("body_geo", CubeListBuilder.create().texOffs(0, 0).addBox(-5F, -4F, -12F, 10F, 10F, 24F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition neck = rig_root.addOrReplaceChild("neck", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, -23F, -10F, 0F, 0F, 0F));
        neck.addOrReplaceChild("neck_geo", CubeListBuilder.create().texOffs(69, 0).addBox(-3F, -12F, -4F, 6F, 14F, 6F), PartPose.offsetAndRotation(0F, 0F, 0F, 0.349066F, 0F, 0F));
        PartDefinition head = rig_root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, -32F, -15F, 0F, 0F, 0F));
        head.addOrReplaceChild("head_geo", CubeListBuilder.create().texOffs(94, 0).addBox(-3.5F, -5F, -5F, 7F, 7F, 8F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        head.addOrReplaceChild("muzzle_geo", CubeListBuilder.create().texOffs(0, 35).addBox(-3F, -1F, -10F, 6F, 4F, 7F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        head.addOrReplaceChild("right_ear_geo", CubeListBuilder.create().texOffs(55, 35).addBox(-4F, -1F, -1F, 4F, 2F, 3F), PartPose.offsetAndRotation(-4F, -3F, 1F, 0F, 0F, 0.314159F));
        head.addOrReplaceChild("right_antler_beam_geo", CubeListBuilder.create().texOffs(70, 35).addBox(-1F, -11F, -1F, 2F, 11F, 2F), PartPose.offsetAndRotation(-3F, -4F, 0F, 0F, 0F, -0.349066F));
        head.addOrReplaceChild("right_antler_branch_geo", CubeListBuilder.create().texOffs(79, 35).addBox(-10F, -11F, -1F, 6F, 2F, 2F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        head.addOrReplaceChild("right_antler_tine_geo", CubeListBuilder.create().texOffs(96, 35).addBox(-1F, -6F, -1F, 2F, 6F, 2F), PartPose.offsetAndRotation(-9F, -10F, 0F, 0F, 0F, -0.20944F));
        head.addOrReplaceChild("right_brow_tine_geo", CubeListBuilder.create().texOffs(105, 35).addBox(-4F, -7F, -6F, 2F, 2F, 6F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        head.addOrReplaceChild("left_ear_geo", CubeListBuilder.create().texOffs(55, 35).addBox(0F, -1F, -1F, 4F, 2F, 3F), PartPose.offsetAndRotation(4F, -3F, 1F, 0F, 0F, -0.314159F));
        head.addOrReplaceChild("left_antler_beam_geo", CubeListBuilder.create().texOffs(70, 35).addBox(-1F, -11F, -1F, 2F, 11F, 2F), PartPose.offsetAndRotation(3F, -4F, 0F, 0F, 0F, 0.349066F));
        head.addOrReplaceChild("left_antler_branch_geo", CubeListBuilder.create().texOffs(79, 35).addBox(4F, -11F, -1F, 6F, 2F, 2F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        head.addOrReplaceChild("left_antler_tine_geo", CubeListBuilder.create().texOffs(96, 35).addBox(-1F, -6F, -1F, 2F, 6F, 2F), PartPose.offsetAndRotation(9F, -10F, 0F, 0F, 0F, 0.20944F));
        head.addOrReplaceChild("left_brow_tine_geo", CubeListBuilder.create().texOffs(105, 35).addBox(2F, -7F, -6F, 2F, 2F, 6F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition right_front_leg = rig_root.addOrReplaceChild("right_front_leg", CubeListBuilder.create(), PartPose.offsetAndRotation(-3.5F, -14F, -9F, 0F, 0F, 0F));
        right_front_leg.addOrReplaceChild("right_front_leg_geo", CubeListBuilder.create().texOffs(27, 35).addBox(-2F, -1F, -2F, 4F, 15F, 4F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition left_front_leg = rig_root.addOrReplaceChild("left_front_leg", CubeListBuilder.create(), PartPose.offsetAndRotation(3.5F, -14F, -9F, 0F, 0F, 0F));
        left_front_leg.addOrReplaceChild("left_front_leg_geo", CubeListBuilder.create().texOffs(27, 35).addBox(-2F, -1F, -2F, 4F, 15F, 4F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition right_hind_leg = rig_root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create(), PartPose.offsetAndRotation(-3.5F, -14F, 9F, 0F, 0F, 0F));
        right_hind_leg.addOrReplaceChild("right_hind_leg_geo", CubeListBuilder.create().texOffs(27, 35).addBox(-2F, -1F, -2F, 4F, 15F, 4F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition left_hind_leg = rig_root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create(), PartPose.offsetAndRotation(3.5F, -14F, 9F, 0F, 0F, 0F));
        left_hind_leg.addOrReplaceChild("left_hind_leg_geo", CubeListBuilder.create().texOffs(27, 35).addBox(-2F, -1F, -2F, 4F, 15F, 4F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition tail = rig_root.addOrReplaceChild("tail", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, -22F, 12F, 0F, 0F, 0F));
        tail.addOrReplaceChild("tail_geo", CubeListBuilder.create().texOffs(44, 35).addBox(-1F, -2F, -1F, 2F, 8F, 3F), PartPose.offsetAndRotation(0F, 0F, 0F, 0.436332F, 0F, 0F));
        body.addOrReplaceChild("horse_armor", CubeListBuilder.create().texOffs(0, 32)
                .addBox(-5.0F, -4.0F, -11.0F, 10.0F, 10.0F, 22.0F, new CubeDeformation(0.12F)), PartPose.ZERO);
        body.addOrReplaceChild("saddle", CubeListBuilder.create().texOffs(0, 64)
                .addBox(-5.5F, -6.0F, -5.0F, 11.0F, 2.0F, 12.0F)
                .addBox(-5.5F, -8.0F, 5.0F, 11.0F, 2.0F, 2.0F)
                .addBox(-5.5F, -7.0F, -5.0F, 11.0F, 1.0F, 2.0F)
                .addBox(-6.0F, -4.0F, 0.0F, 1.0F, 9.0F, 2.0F)
                .addBox(5.0F, -4.0F, 0.0F, 1.0F, 9.0F, 2.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 128, 128);
    }
}
