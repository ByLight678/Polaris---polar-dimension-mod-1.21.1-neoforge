"""Import the two wilderness templates as 1.21.1 copies; source files stay untouched."""
import argparse
import json
from pathlib import Path
import nbtlib as n

ROOT = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--source', required=True, type=Path)
args = parser.parse_args()
target = ROOT / 'src/main/resources/data/polar_dimension/structure/wilderness'
target.mkdir(parents=True, exist_ok=True)
manifest = {}
for name, surface in [('abandoned_snowy_campsite', 2), ('snowy_igloo', 1)]:
    data = n.load(args.source / (name + '.nbt'))
    version = int(data['DataVersion'])
    palette = data['palette']
    blocks = {tuple(map(int, b['pos'])): b for b in data['blocks']}
    repairs = 0
    def state_at(pos):
        return palette[int(blocks[pos]['state'])] if pos in blocks else {'Name': 'minecraft:air'}
    def put(pos, identifier, properties=None):
        state = n.Compound(Name=n.String(identifier))
        if properties:
            state['Properties'] = n.Compound({key: n.String(value) for key, value in properties.items()})
        palette.append(state)
        blocks[pos] = n.Compound(pos=n.List[n.Int](list(pos)), state=n.Int(len(palette) - 1))
    for (x, y, z), block in list(blocks.items()):
        state = palette[int(block['state'])]
        identifier = str(state['Name'])
        if identifier.endswith('_carpet') and str(state_at((x, y - 1, z))['Name']) == 'minecraft:air':
            put((x, y, z), identifier.removesuffix('_carpet') + '_wool')
            repairs += 1
        if identifier == 'minecraft:lantern' and str(state.get('Properties', {}).get('hanging')) == 'true':
            anchor = next((yy for yy in range(y + 1, int(data['size'][1]))
                           if str(state_at((x, yy, z))['Name']) != 'minecraft:air'), None)
            if anchor is not None:
                for yy in range(y + 1, anchor):
                    put((x, yy, z), 'minecraft:chain', {'axis': 'y'})
                    repairs += 1
            else:
                put((x, y, z), 'minecraft:lantern', {'hanging': 'false'})
                for yy in range(y - 1, surface, -1):
                    if str(state_at((x, yy, z))['Name']) != 'minecraft:air':
                        break
                    put((x, yy, z), 'minecraft:spruce_fence')
                    repairs += 1
    data['blocks'] = n.List[n.Compound](list(blocks.values()))
    data['DataVersion'] = n.Int(3955)
    data.save(target / (name + '.nbt'), gzipped=True)
    manifest[name] = dict(size=list(map(int, data['size'])), surface_layer=surface,
                          source_data_version=version, support_repairs=repairs)
(target / 'manifest.json').write_text(json.dumps(manifest, indent=2) + '\n', encoding='utf-8')
print(json.dumps(manifest, indent=2))
