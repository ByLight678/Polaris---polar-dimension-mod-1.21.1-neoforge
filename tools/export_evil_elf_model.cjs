// Export the evil_elf rig to a native model, retaining all source cubes and box UVs.
const fs = require('fs');
const model = JSON.parse(fs.readFileSync('art/evil_elf/evil_christmas_elf.bbmodel','utf8'));
if (!model.meta.box_uv || model.resolution.width !== 64 || model.resolution.height !== 64) throw Error('Expected 64x64 box UVs');
const groups = new Map(model.groups.map(g => [g.uuid,g]));
const elements = new Map(model.elements.map(e => [e.uuid,e]));
const f = n => `${Number(n.toFixed(6))}F`;
const pose = (p,r) => `PartPose.offsetAndRotation(${[...p,...r].map(f).join(', ')})`;
const angle = r => r.map((n,i) => n*Math.PI/180*(i===1?1:-1));
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
        for(const face of Object.values(cube.faces))if(face.uv.some(v=>v<0||v>64))throw Error('UV outside atlas');
        const uv=cube.uv_offset||[cube.faces.east.uv[0],cube.faces.east.uv[1]-size[2]];
        const box=[cube.from[0]-pivot[0],pivot[1]-cube.to[1],cube.from[2]-pivot[2],...size];
        const cubeOffset=rotated?[pivot[0]-g[0],g[1]-pivot[1],pivot[2]-g[2]]:[0,0,0];
        lines.push(`        ${group.name}.addOrReplaceChild("${cube.name}", CubeListBuilder.create().texOffs(${uv[0]}, ${uv[1]}).addBox(${box.map(f).join(', ')}), ${pose(cubeOffset,angle(rotation))});`);
        used.add(child);
    }
}
for(const node of model.outliner)visit(node,'root',null);
if(used.size!==model.elements.length)throw Error('Export would omit geometry');
const java = `package com.josep.polardimension.client.model;

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
${lines.join('\n')}
        return LayerDefinition.create(mesh, 64, 64);
    }
}
`;
fs.writeFileSync('src/main/java/com/josep/polardimension/client/model/EvilElfModel.java', java);
console.log('Evil elf: exported', used.size, 'cubes and', groups.size, 'animated bones');

