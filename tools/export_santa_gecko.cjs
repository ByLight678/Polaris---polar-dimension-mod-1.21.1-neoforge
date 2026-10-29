// Reproducible Blockbench -> GeckoLib/Bedrock export. Never modifies the artist's model.
const fs = require('fs');
const root = 'src/main/resources/assets/polar_dimension';
const source = JSON.parse(fs.readFileSync('art/disheveled_santa/Disheveled Santa.bbmodel', 'utf8'));
const groups = new Map(source.groups.map(g => [g.uuid, g]));
const cubes = new Map(source.elements.map(c => [c.uuid, c]));
const bones = [], used = new Set();
const rotation = r => [-r[0], -r[1], r[2]];
function visit(node, parent) {
    const group = groups.get(node.uuid);
    if (!group) throw Error('Unknown bone ' + node.uuid);
    const bone = {name: group.name, pivot: [-group.origin[0], group.origin[1], group.origin[2]]};
    if (parent) bone.parent = parent;
    if (group.rotation?.some(n => n !== 0)) bone.rotation = rotation(group.rotation);
    bone.cubes = [];
    bones.push(bone);
    for (const child of node.children) {
        if (typeof child === 'object') { visit(child, group.name); continue; }
        const c = cubes.get(child);
        if (!c || c.type !== 'cube' || !c.box_uv) throw Error('Unsupported element ' + child);
        const size = c.to.map((n, i) => n - c.from[i]);
        const uv = c.uv_offset || [c.faces.east.uv[0], c.faces.east.uv[1] - size[2]];
        const cube = {origin: [-c.to[0], c.from[1], c.from[2]], size, uv};
        if (c.rotation?.some(n => n !== 0)) {
            cube.pivot = [-c.origin[0], c.origin[1], c.origin[2]];
            cube.rotation = rotation(c.rotation);
        }
        if (c.inflate) cube.inflate = c.inflate;
        if (c.mirror_uv) cube.mirror = true;
        bone.cubes.push(cube);
        used.add(child);
    }
}
source.outliner.forEach(node => visit(node));
if (used.size !== source.elements.length) throw Error('Export would omit geometry');
const geometry = {format_version: '1.12.0', 'minecraft:geometry': [{description: {
    identifier: 'geometry.disheveled_santa', texture_width: source.resolution.width,
    texture_height: source.resolution.height, visible_bounds_width: 7, visible_bounds_height: 7,
    visible_bounds_offset: [0, 2, 0]
}, bones}]};
const animations = {};
let keyframes = 0;
for (const a of source.animations) {
    const animation = {loop: a.loop === 'loop' ? true : a.loop === 'hold' ? 'hold_on_last_frame' : false,
        animation_length: a.length, bones: {}};
    for (const [uuid, animator] of Object.entries(a.animators)) {
        if (!groups.has(uuid)) throw Error('Animated bone is missing: ' + uuid);
        const bone = {};
        for (const k of animator.keyframes || []) {
            if (k.interpolation !== 'linear' || k.data_points.length !== 1) throw Error('Unsupported interpolation');
            const vector = ['x', 'y', 'z'].map(axis => Number(k.data_points[0][axis]));
            if (!vector.every(Number.isFinite)) throw Error('Non-numeric animation value');
            const value = k.channel === 'rotation' ? rotation(vector)
                : k.channel === 'position' ? [-vector[0], vector[1], vector[2]] : vector;
            (bone[k.channel] ||= {})[String(k.time)] = value;
            keyframes++;
        }
        animation.bones[groups.get(uuid).name] = bone;
    }
    animations[a.name] = animation;
}
// Heavy alternating steps, torso sway and a counter-moving head, inspired by the warden's gait.
const wave = (axis, values, length) => Object.fromEntries(values.map((value, i) =>
    [(i * length / 4).toFixed(2), [axis === 0 ? value : 0, axis === 1 ? value : 0, axis === 2 ? value : 0]]));
animations.walk = {loop: true, animation_length: 2.4, bones: {
    right_leg: {rotation: wave(0, [0, -24, 0, 24, 0], 2.4)},
    left_leg: {rotation: wave(0, [0, 24, 0, -24, 0], 2.4)},
    body: {rotation: wave(2, [0, -4, 0, 4, 0], 2.4), position: wave(1, [0, 0.5, 0, 0.5, 0], 2.4)},
    head: {rotation: wave(2, [0, 2, 0, -2, 0], 2.4)},
    right_arm: {rotation: wave(0, [0, 8, 0, -8, 0], 2.4)},
    left_arm: {rotation: wave(0, [0, -8, 0, 8, 0], 2.4)}
}};
animations.idle = {loop: true, animation_length: 4, bones: {
    body: {position: wave(1, [0, 0.15, 0, 0.15, 0], 4)},
    head: {rotation: wave(0, [0, 1, 0, -1, 0], 4)}
}};
for (const dir of ['geo', 'animations']) fs.mkdirSync(`${root}/${dir}`, {recursive: true});
fs.writeFileSync(`${root}/geo/disheveled_santa.geo.json`, JSON.stringify(geometry, null, 2) + '\n');
fs.writeFileSync(`${root}/animations/disheveled_santa.animation.json`, JSON.stringify({format_version: '1.8.0', animations}, null, 2) + '\n');
console.log(`Santa: ${used.size} cubes, ${bones.length} bones, ${keyframes} authored keyframes, five original clips + walk/idle`);
