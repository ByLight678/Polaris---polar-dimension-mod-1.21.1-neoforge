"""Rebuild the fixed, irregular North Pole plan from imported template dimensions.

Uses only Python's standard library. The stable seed makes plans reproducible;
world generation reads the bundled result, so chunk order cannot change the village.
"""
import heapq
import json
import math
import random
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TARGET = ROOT / 'src/main/resources/data/polar_dimension/structure/village'
manifest = json.loads((TARGET / 'manifest.json').read_text())
rng = random.Random(20261028)
entrances = dict(town_tree=17, toy_workshop=17, lodge=11, gift_shop=10,
                 restaurant=14, reindeer_stable=15, townhouse=10, cottage=10,
                 alpine_house=11, cranberry_cottage=10)
rotations = ['NONE', 'CLOCKWISE_90', 'CLOCKWISE_180', 'COUNTERCLOCKWISE_90']
directions = [(0, 1), (-1, 0), (0, -1), (1, 0)]


def plot(kind, x, z, rotation):
    width, _, depth = manifest['christmas_' + kind]['size']
    sx, sz = (width, depth) if rotation % 2 == 0 else (depth, width)
    ex, ez = entrances[kind], depth - 1
    ex, ez = [(ex, ez), (depth - 1 - ez, ex),
              (width - 1 - ex, depth - 1 - ez), (ez, width - 1 - ex)][rotation]
    dx, dz = directions[rotation]
    return dict(template='christmas_' + kind, x=x, z=z, width=width, depth=depth,
                surface_layer=manifest['christmas_' + kind]['surface_layer'],
                entrance_x=entrances[kind], rotation=rotations[rotation],
                sx=sx, sz=sz, entry=(x + ex, z + ez), front=(x + ex + dx * 3, z + ez + dz * 3))


def contains(p, x, z, margin=0):
    return p['x'] - margin <= x < p['x'] + p['sx'] + margin and p['z'] - margin <= z < p['z'] + p['sz'] + margin


def separated(a, b, gap=4):
    return (a['x'] + a['sx'] + gap <= b['x'] or b['x'] + b['sx'] + gap <= a['x']
            or a['z'] + a['sz'] + gap <= b['z'] or b['z'] + b['sz'] + gap <= a['z'])


plots = [plot('town_tree', -17, -17, 0), plot('toy_workshop', -17, -55, 0)]
kinds = ['reindeer_stable', 'restaurant', 'lodge', 'gift_shop', 'gift_shop']
houses = ['cottage', 'townhouse', 'alpine_house', 'cranberry_cottage'] * 5 + ['cottage', 'townhouse']
rng.shuffle(houses)
kinds += houses
for kind in kinds:
    best, score = None, float('inf')
    for _ in range(18000):
        rotation = rng.randrange(4)
        x, z = rng.randint(-104, 83), rng.randint(-104, 83)
        p = plot(kind, x, z, rotation)
        if max(abs(x), abs(z), abs(x + p['sx']), abs(z + p['sz'])) > 108:
            continue
        if not all(separated(p, old) and not contains(p, *old['front'], 1)
                   and not contains(old, *p['front'], 1) for old in plots):
            continue
        cx, cz = x + p['sx'] / 2, z + p['sz'] / 2
        dx, dz = directions[rotation]
        # Entrances generally face inward; small jitter keeps clusters staggered.
        inward = dx * cx + dz * cz
        cost = cx * cx + cz * cz + max(0, inward) * 65 + rng.random() * 650
        if cost < score:
            best, score = p, cost
    if best is None:
        raise RuntimeError('No compact non-overlapping plot for ' + kind)
    plots.append(best)

# Three-block paths grow from the plaza to the nearest unconnected entrance.
# Eight-direction routing creates diagonal lanes and bends around buildings.
blocked = {(x, z) for x in range(-114, 115) for z in range(-114, 115)
           if any(contains(p, x, z, 1) for p in plots)}
network = {(x, z) for x in range(-19, 20) for z in range(-19, 20)
           if max(abs(x), abs(z)) == 19}
paths = [list(network)]
remaining = plots[1:]
while remaining:
    goals = {p['front']: p for p in remaining}
    queue = [(0, x, z) for x, z in network]
    heapq.heapify(queue)
    costs = {point: 0 for point in network}
    previous = {}
    reached = None
    while queue:
        cost, x, z = heapq.heappop(queue)
        if cost != costs.get((x, z)):
            continue
        if (x, z) in goals:
            reached = x, z
            break
        for dx, dz in [(1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (-1, -1), (1, -1), (-1, 1)]:
            point = x + dx, z + dz
            if max(map(abs, point)) > 113 or point in blocked:
                continue
            if dx and dz and ((x + dx, z) in blocked or (x, z + dz) in blocked):
                continue
            candidate = cost + (14 if dx and dz else 10)
            if candidate < costs.get(point, float('inf')):
                costs[point] = candidate
                previous[point] = x, z
                heapq.heappush(queue, (candidate, *point))
    if reached is None:
        raise RuntimeError('Unreachable entrances: ' + str(goals))
    p = goals[reached]
    path = [reached]
    while path[-1] not in network:
        path.append(previous[path[-1]])
    # Include the short approach to the actual template edge.
    ex, ez = p['entry']
    fx, fz = p['front']
    approach = [(round(ex + (fx - ex) * t / 3), round(ez + (fz - ez) * t / 3)) for t in range(4)]
    path += approach
    paths.append(path)
    network.update(path)
    remaining = [other for other in remaining if other is not p]

# Every point is a path center; widening is shared by generation and maintenance.
centers = sorted(set(point for path in paths for point in path))
roads = {(x + dx, z + dz) for x, z in centers for dx in range(-1, 2) for dz in range(-1, 2)
         if not any(contains(p, x + dx, z + dz) for p in plots)}
# Place lights at road edges with a generous, irregular spacing.
lamps = []
edges = sorted({(x + dx, z + dz) for x, z in roads for dx, dz in directions
                if (x + dx, z + dz) not in roads
                and not any(contains(p, x + dx, z + dz, 1) for p in plots)})
rng.shuffle(edges)
for point in edges:
    if all(math.dist(point, old) >= 16 for old in lamps):
        lamps.append(point)

clean_plots = [{key: value for key, value in p.items() if key not in ('sx', 'sz', 'entry', 'front')} for p in plots]
result = dict(plots=clean_plots, path_centers=centers, lanterns=sorted(lamps))
(TARGET / 'layout.json').write_text(json.dumps(result, separators=(',', ':')) + '\n', encoding='utf-8')
print(f'{len(plots)} plots, {len(roads)} road blocks, {len(lamps)} lanterns')
print('Bounds:', min(p['x'] for p in plots), min(p['z'] for p in plots),
      max(p['x'] + p['sx'] for p in plots), max(p['z'] + p['sz'] for p in plots))
