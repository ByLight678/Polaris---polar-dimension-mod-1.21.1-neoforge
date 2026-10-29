"""Import 1.21.5 village assets as validated 1.21.1 copies; originals are never edited.

Requires nbtlib (python -m pip install nbtlib). Run with --source <structures-folder>.
"""
import argparse
import json
from pathlib import Path
import nbtlib as n

ROOT = Path(__file__).resolve().parents[1]
TARGET = ROOT / 'src/main/resources/data/polar_dimension/structure/village'


def compound(**values):
    return n.Compound(values)


def entity(identifier, x, y, z, **extra):
    return compound(pos=n.List[n.Double]([x, y, z]), blockPos=n.List[n.Int]([int(x), int(y), int(z)]),
                    nbt=compound(id=n.String(identifier if ':' in identifier else 'minecraft:' + identifier), PersistenceRequired=n.Byte(1), **extra))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', required=True, type=Path)
    args = parser.parse_args()
    TARGET.mkdir(parents=True, exist_ok=True)
    manifest = {}
    for source in sorted(args.source.glob('christmas_*.nbt')):
        data = n.load(source)
        version = int(data.get('DataVersion', 0))
        palette = data['palette']
        blocks = {tuple(map(int, block['pos'])): block for block in data['blocks']}
        if source.stem == 'christmas_cottage':
            for x in (8, 13):
                for z, part in ((8, 'foot'), (9, 'head')):
                    state = compound(Name=n.String('minecraft:red_bed'), Properties=compound(
                        facing=n.String('south'), part=n.String(part), occupied=n.String('false')))
                    palette.append(state)
                    blocks[x, 3, z] = compound(pos=n.List[n.Int]([x, 3, z]), state=n.Int(len(palette) - 1))
            data['blocks'] = n.List[n.Compound](list(blocks.values()))
        entities = n.List[n.Compound]([])
        for block in data['blocks']:
            state = palette[int(block['state'])]
            if str(state['Name']).endswith('_bed') and str(state.get('Properties', {}).get('part')) == 'head':
                x, y, z = map(int, block['pos'])
                variety = 'female_christmas_elf' if len(entities) % 2 else 'christmas_elf'
                entities.append(entity('polar_dimension:' + variety, x + .5, y + .6, z + .5,
                                       VillageResident=n.Byte(1)))
        if source.stem == 'christmas_town_tree':
            for x in (8, 26):
                entities.append(entity('iron_golem', x + .5, 2., 17.5, PlayerCreated=n.Byte(0)))
            palette.append(compound(Name=n.String('minecraft:bell'), Properties=compound(
                attachment=n.String('floor'), facing=n.String('south'), powered=n.String('false'))))
            blocks[17, 2, 32] = compound(pos=n.List[n.Int]([17, 2, 32]), state=n.Int(len(palette) - 1))
            data['blocks'] = n.List[n.Compound](list(blocks.values()))
        if source.stem == 'christmas_reindeer_stable':
            for x, z in ((7, 7), (23, 7), (7, 21), (23, 21)):
                entities.append(entity('polar_dimension:reindeer', x + .5, 3., z + .5, Age=n.Int(0), Tame=n.Byte(0)))
        repairs = 0
        def state_at(pos):
            block = blocks.get(pos)
            return palette[int(block['state'])] if block is not None else {'Name': 'minecraft:air'}
        def set_state(pos, state):
            palette.append(state)
            blocks[pos] = compound(pos=n.List[n.Int](list(pos)), state=n.Int(len(palette) - 1))
        for (x, y, z), block in list(blocks.items()):
            state = palette[int(block['state'])]
            name = str(state['Name'])
            if name.endswith('_carpet') and str(state_at((x, y - 1, z))['Name']) == 'minecraft:air':
                set_state((x, y, z), compound(Name=n.String(name.removesuffix('_carpet') + '_wool')))
                repairs += 1
            if name == 'minecraft:lantern' and str(state.get('Properties', {}).get('hanging')) == 'true':
                anchor = next((yy for yy in range(y + 1, min(int(data['size'][1]), y + 10))
                               if str(state_at((x, yy, z))['Name']) != 'minecraft:air'), None)
                if anchor is not None:
                    for yy in range(y + 1, anchor):
                        set_state((x, yy, z), compound(Name=n.String('minecraft:chain'), Properties=compound(axis=n.String('y'))))
                        repairs += 1
                else:
                    set_state((x, y, z), compound(Name=n.String('minecraft:lantern'), Properties=compound(hanging=n.String('false'))))
                    for yy in range(y - 1, -1, -1):
                        if str(state_at((x, yy, z))['Name']) != 'minecraft:air':
                            break
                        set_state((x, yy, z), compound(Name=n.String('minecraft:spruce_fence')))
                        repairs += 1
        data['blocks'] = n.List[n.Compound](list(blocks.values()))
        data['entities'] = entities
        # These assets use existing 1.21.1 states. Integration tests validate every palette state.
        data['DataVersion'] = n.Int(3955)
        data.save(TARGET / source.name, gzipped=True)
        manifest[source.stem] = dict(size=list(map(int, data['size'])), source_data_version=version,
            data_version=3955, surface_layer=7 if source.stem == 'christmas_toy_workshop' else 1,
            blocks=len(data['blocks']), entities=len(entities), support_repairs=repairs)
    if len(manifest) != 10:
        raise SystemExit(f'Expected ten templates; imported {len(manifest)}')
    (TARGET / 'manifest.json').write_text(json.dumps(manifest, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(manifest, indent=2))
    from plan_interior_lighting import generate
    generate()


if __name__ == '__main__':
    main()



