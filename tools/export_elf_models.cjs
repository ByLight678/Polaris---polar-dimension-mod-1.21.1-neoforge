// Convert the supplied six-bone, box-UV Blockbench models to native 1.21.1 model layers.
// Originals are retained in art/elves; generated Java contains the runtime walk/look animation.
const fs = require('fs');
const names = ['christmas_elf', 'female_christmas_elf'];
const f = n => `${Number(n.toFixed(6))}F`;
const pose = (offset, rotation) => `PartPose.offsetAndRotation(${[...offset, ...rotation].map(f).join(', ')})`;
function layer(name, method) {
    const model = JSON.parse(fs.readFileSync(`art/elves/${name}.bbmodel`, 'utf8'));
    if (!model.meta.box_uv || model.resolution.width !== 64 || model.resolution.height !== 64) throw Error('Expected a 64x64 box-UV model');
    const groups = new Map(model.groups.map(g => [g.uuid, g]));
    const cubes = new Map(model.elements.map(c => [c.uuid, c]));
    const lines = [`    public static LayerDefinition ${method}() {`, '        MeshDefinition mesh = new MeshDefinition();', '        PartDefinition root = mesh.getRoot();'];
    const emitted = new Set();
    for (const node of model.outliner) {
        const group = groups.get(node.uuid);
        if (!group || !['head','body','left_arm','right_arm','left_leg','right_leg'].includes(group.name)) throw Error('Unsupported bone hierarchy');
        const g = group.origin;
        const rotation = (group.rotation || [0,0,0]).map((r,i) => r * Math.PI / 180 * (i === 2 ? 1 : -1));
        lines.push(`        PartDefinition ${group.name} = root.addOrReplaceChild("${group.name}", CubeListBuilder.create(), ${pose([g[0],24-g[1],g[2]], rotation)});`);
        for (const id of node.children) {
            const cube = cubes.get(id);
            if (!cube || cube.type !== 'cube' || !cube.box_uv || cube.export === false) throw Error('Unsupported cube: ' + id);
            const origin = cube.origin || g;
            const rot = cube.rotation || [0,0,0];
            const rotated = rot.some(v => v !== 0);
            const pivot = rotated ? origin : g;
            const size = cube.to.map((value,i) => value - cube.from[i]);
            if (size.some(v => v <= 0)) throw Error('Invalid cube size');
            for (const face of Object.values(cube.faces)) if (face.uv.some(v => v < 0 || v > 64)) throw Error('UV outside atlas');
            const uv = cube.uv_offset || [cube.faces.east.uv[0], cube.faces.east.uv[1] - size[2]];
            const box = [cube.from[0]-pivot[0], pivot[1]-cube.to[1], cube.from[2]-pivot[2], ...size];
            const builder = `CubeListBuilder.create().texOffs(${uv[0]}, ${uv[1]})${cube.mirror_uv ? '.mirror()' : ''}.addBox(${box.map(f).join(', ')})`;
            const offset = rotated ? [origin[0]-g[0],g[1]-origin[1],origin[2]-g[2]] : [0,0,0];
            const angles = rot.map((r,i) => r * Math.PI / 180 * (i === 2 ? 1 : -1));
            lines.push(`        ${group.name}.addOrReplaceChild("${cube.name}", ${builder}, ${pose(offset,angles)});`);
            emitted.add(id);
        }
    }
    if (emitted.size !== model.elements.length || model.groups.length !== 6) throw Error('Export would skip geometry');
    lines.push('        return LayerDefinition.create(mesh, 64, 64);','    }');
    console.log(`${name}: exported ${emitted.size} cubes and six animated bones`);
    return lines.join('\n');
}
const java = `package com.josep.polardimension.client.model;

import com.josep.polardimension.entity.ElfEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** Generated from art/elves by tools/export_elf_models.cjs. */
public final class ElfModel extends HierarchicalModel<ElfEntity> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public ElfModel(ModelPart root) {
        this.root = root;
        head = root.getChild("head");
        rightArm = root.getChild("right_arm"); leftArm = root.getChild("left_arm");
        rightLeg = root.getChild("right_leg"); leftLeg = root.getChild("left_leg");
    }

    @Override
    public ModelPart root() { return root; }

    @Override
    public void setupAnim(ElfEntity entity, float limbSwing, float amount, float age, float yaw, float pitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        head.yRot = yaw * Mth.DEG_TO_RAD;
        head.xRot = pitch * Mth.DEG_TO_RAD;
        rightLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * amount;
        leftLeg.xRot = Mth.cos(limbSwing * 0.6662F + Mth.PI) * 1.4F * amount;
        rightArm.xRot = leftLeg.xRot * 0.75F;
        leftArm.xRot = rightLeg.xRot * 0.75F;
        rightArm.zRot += Mth.cos(age * 0.09F) * 0.03F + 0.03F;
        leftArm.zRot -= Mth.cos(age * 0.09F) * 0.03F + 0.03F;
    }

${layer(names[0], 'createMaleLayer')}

${layer(names[1], 'createFemaleLayer')}
}
`;
fs.writeFileSync('src/main/java/com/josep/polardimension/client/model/ElfModel.java',java);
