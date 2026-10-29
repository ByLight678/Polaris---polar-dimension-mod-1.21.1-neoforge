// Export the evil_snowman rig to a native model, retaining all source cubes and box UVs.
const fs = require('fs');
const model = JSON.parse(fs.readFileSync('art/evil_snowman/evil_snowman.bbmodel','utf8'));
if (!model.meta.box_uv || model.resolution.width !== 128 || model.resolution.height !== 128) throw Error('Expected 128x128 box UVs');
const groups = new Map(model.groups.map(g => [g.uuid,g]));
const elements = new Map(model.elements.map(e => [e.uuid,e]));
const f = n => `${Number(n.toFixed(6))}F`;
const pose = (p,r) => `PartPose.offsetAndRotation(${[...p,...r].map(f).join(', ')})`;
const angle = r => r.map((n,i) => n*Math.PI/180*(i===2?1:-1));
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
const java = `package com.josep.polardimension.client.model;

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
${lines.join('\n')}
        return LayerDefinition.create(mesh, 128, 128);
    }
}
`;
fs.writeFileSync('src/main/java/com/josep/polardimension/client/model/EvilSnowmanModel.java', java);
console.log('Exported', used.size, 'cubes and', groups.size, 'bones with validated 128x128 UVs');

