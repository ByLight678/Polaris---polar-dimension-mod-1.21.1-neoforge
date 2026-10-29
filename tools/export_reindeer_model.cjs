// Export the nested reindeer rig to a native model, retaining all source cubes and box UVs.
const fs = require('fs');
const model = JSON.parse(fs.readFileSync('art/reindeer/reindeer.bbmodel','utf8'));
if (!model.meta.box_uv || model.resolution.width !== 128 || model.resolution.height !== 128) throw Error('Expected 128x128 box UVs');
const groups = new Map(model.groups.map(g => [g.uuid,g]));
const elements = new Map(model.elements.map(e => [e.uuid,e]));
const f = n => `${Number(n.toFixed(6))}F`;
const pose = (p,r) => `PartPose.offsetAndRotation(${[...p,...r].map(f).join(', ')})`;
// Reflect Blockbench's upward Y into Minecraft's downward Y. X/Z rotations reverse;
// Y rotation does not. Keeping Z's sign would tilt the antlers inward across the face.
const angle = r => r.map((n,i) => n*Math.PI/180*(i===1?1:-1));
// Validate every antler corner after export, rather than just checking the pivot.
for (const cube of model.elements.filter(e => e.name.includes('antler'))) {
    const rotation = cube.rotation || [0,0,0];
    if (rotation[0] || rotation[1]) throw Error('Update antler clearance validation for multi-axis rotations');
    const a = angle(rotation)[2], pivot = cube.origin || [0,0,0];
    for (const x of [cube.from[0],cube.to[0]]) for (const y of [cube.from[1],cube.to[1]]) {
        const exportedX = pivot[0] + (x-pivot[0])*Math.cos(a) - (pivot[1]-y)*Math.sin(a);
        if (cube.name.startsWith('right_') ? exportedX >= -1.5 : exportedX <= 1.5) {
            throw Error('Antler crosses the central viewing corridor: ' + cube.name);
        }
    }
}
const lines = [], used = new Set();
function visit(node,parentName,parentOrigin) {
    const group=groups.get(node.uuid); if(!group)throw Error('Missing bone');
    const g=group.origin;
    const offset=parentOrigin ? [g[0]-parentOrigin[0],parentOrigin[1]-g[1],g[2]-parentOrigin[2]] : [g[0],24-g[1],g[2]];
    lines.push(`        PartDefinition ${group.name} = ${parentName}.addOrReplaceChild("${group.name}", CubeListBuilder.create(), ${pose(offset,angle(group.rotation||[0,0,0]))});`);
    for(const child of node.children) {
        if(typeof child==='object') {visit(child,group.name,g);continue;}
        const cube=elements.get(child); if(!cube||cube.type!=='cube'||!cube.box_uv)throw Error('Unsupported element');
        const rotation=cube.rotation||[0,0,0],rotated=rotation.some(v=>v!==0),pivot=rotated?cube.origin:g;
        const size=cube.to.map((v,i)=>v-cube.from[i]);
        for(const face of Object.values(cube.faces))if(face.uv.some(v=>v<0||v>128))throw Error('UV outside atlas');
        const uv=cube.uv_offset||[cube.faces.east.uv[0],cube.faces.east.uv[1]-size[2]];
        const box=[cube.from[0]-pivot[0],pivot[1]-cube.to[1],cube.from[2]-pivot[2],...size];
        const cubeOffset=rotated?[pivot[0]-g[0],g[1]-pivot[1],pivot[2]-g[2]]:[0,0,0];
        lines.push(`        ${group.name}.addOrReplaceChild("${cube.name}", CubeListBuilder.create().texOffs(${uv[0]}, ${uv[1]}).addBox(${box.map(f).join(', ')}), ${pose(cubeOffset,angle(rotation))});`);
        used.add(child);
    }
}
for(const node of model.outliner)visit(node,'root',null);
if(used.size!==model.elements.length)throw Error('Export would omit geometry');
lines.push(`        body.addOrReplaceChild("horse_armor", CubeListBuilder.create().texOffs(0, 32)
                .addBox(-5.0F, -4.0F, -11.0F, 10.0F, 10.0F, 22.0F, new CubeDeformation(0.12F)), PartPose.ZERO);
        body.addOrReplaceChild("saddle", CubeListBuilder.create().texOffs(0, 64)
                .addBox(-5.5F, -6.0F, -5.0F, 11.0F, 2.0F, 12.0F)
                .addBox(-5.5F, -8.0F, 5.0F, 11.0F, 2.0F, 2.0F)
                .addBox(-5.5F, -7.0F, -5.0F, 11.0F, 1.0F, 2.0F)
                .addBox(-6.0F, -4.0F, 0.0F, 1.0F, 9.0F, 2.0F)
                .addBox(5.0F, -4.0F, 0.0F, 1.0F, 9.0F, 2.0F), PartPose.ZERO);`);
const java=`package com.josep.polardimension.client.model;

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
${lines.join('\n')}
        return LayerDefinition.create(mesh, 128, 128);
    }
}
`;
fs.writeFileSync('src/main/java/com/josep/polardimension/client/model/ReindeerModel.java',java);
console.log(`Reindeer: exported ${used.size} source cubes, ${groups.size} bones, plus a conditional saddle`);

