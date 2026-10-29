"""Plan supported interior wall torches from imported village geometry."""
from pathlib import Path
from collections import deque
import json
import nbtlib as n
ROOT = Path(__file__).resolve().parents[1]
FOLDER = ROOT / 'src/main/resources/data/polar_dimension/structure/village'
STEPS = [(1,0,0),(-1,0,0),(0,1,0),(0,-1,0),(0,0,1),(0,0,-1)]
WALLS = [('east',(-1,0,0)),('west',(1,0,0)),('south',(0,0,-1)),('north',(0,0,1))]
def add(a,b): return tuple(x+y for x,y in zip(a,b))
def solid(name):
    return name.endswith(('_planks','_log','_wood','_bricks','_brick','_wool','_terracotta','_concrete')) or name in {'minecraft:stone','minecraft:bricks','minecraft:cobblestone','minecraft:deepslate','minecraft:snow_block','minecraft:packed_ice','minecraft:bookshelf','minecraft:barrel'}
def generate():
    result = {}
    for file in sorted(FOLDER.glob('*.nbt')):
        if file.stem == 'christmas_town_tree': continue
        data=n.load(file); size=tuple(map(int,data['size']))
        blocks={tuple(map(int,b['pos'])):str(data['palette'][int(b['state'])]['Name']) for b in data['blocks']}
        def state(p): return blocks.get(p,'minecraft:air')
        def inside(p): return all(0<=p[i]<size[i] for i in range(3))
        def air(p): return state(p)=='minecraft:air'
        def opaque(p): return solid(state(p))
        rooms=[]
        for p in blocks:
            if not air(p) or not air(add(p,(0,1,0))) or not opaque(add(p,(0,-1,0))): continue
            if not any(opaque(add(p,(0,dy,0))) for dy in range(2,17)): continue
            # Require enclosure in every horizontal direction; exclude open porches and scenery.
            if all(any(opaque(add(p,(dx*d,1,dz*d))) or 'glass' in state(add(p,(dx*d,1,dz*d)))
                       or '_door' in state(add(p,(dx*d,1,dz*d))) for d in range(1,13))
                   for dx,dz in [(1,0),(-1,0),(0,1),(0,-1)]): rooms.append(p)
        candidates={}
        for p in rooms:
            for dy in (1,2):
                q=add(p,(0,dy,0))
                if not air(q): continue
                for facing,offset in WALLS:
                    support=add(q,offset)
                    if opaque(support): candidates[q]=(facing,state(support)); break
        def spread(source,level):
            levels={source:level}; queue=deque([source])
            while queue:
                p=queue.popleft(); v=levels[p]-1
                if v<=0: continue
                for delta in STEPS:
                    q=add(p,delta)
                    if inside(q) and not opaque(q) and v>levels.get(q,0): levels[q]=v;queue.append(q)
            return levels
        light={}
        # Do not rely on pre-existing lanterns: new fixtures independently cover each room.
        coverage={p:spread(p,14) for p in candidates}
        plans=[]
        while True:
            dark=[p for p in rooms if light.get(p,0)<9]
            if not dark: break
            best=None; bestscore=0
            for p in candidates:
                if any(sum(abs(a-b) for a,b in zip(p,old['pos']))<3 for old in plans): continue
                levels=coverage[p]
                score=sum(max(0,min(9,levels.get(q,0))-light.get(q,0)) for q in dark)
                if score>bestscore: best=(p,levels);bestscore=score
            if best is None: break
            p,levels=best; facing,support=candidates.pop(p)
            plans.append({'pos':list(p),'facing':facing,'support':support})
            for q,v in levels.items():light[q]=max(light.get(q,0),v)
        result[file.stem]={'torches':plans,'samples':[list(p) for p in rooms]}
        print(file.stem, 'interior cells',len(rooms),'new torches',len(plans),'minimum',min((light.get(p,0) for p in rooms),default=0))
    (FOLDER/'interior_lighting.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
if __name__=='__main__':generate()
