"""Render the tested village layout. Requires Pillow; run the GameTests first."""
import json
import sys
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
if '--plan' in sys.argv:
    plan = json.loads((ROOT / 'src/main/resources/data/polar_dimension/structure/village/layout.json').read_text())
    report = {'plots': [], 'roads': []}
    for p in plan['plots']:
        w, d, ex, ez = p['width'], p['depth'], p['entrance_x'], p['depth'] - 1
        r = ['NONE', 'CLOCKWISE_90', 'CLOCKWISE_180', 'COUNTERCLOCKWISE_90'].index(p['rotation'])
        ex, ez = [(ex, ez), (d - 1 - ez, ex), (w - 1 - ex, d - 1 - ez), (ez, w - 1 - ex)][r]
        report['plots'].append(dict(template=p['template'], x=p['x'], z=p['z'],
            width=d if r % 2 else w, depth=w if r % 2 else d, entry_x=p['x'] + ex, entry_z=p['z'] + ez))
    report['roads'] = list({(x + dx, z + dz) for x, z in plan['path_centers'] for dx in (-1, 0, 1) for dz in (-1, 0, 1)
        if not any(p['x'] <= x + dx < p['x'] + p['width'] and p['z'] <= z + dz < p['z'] + p['depth'] for p in report['plots'])})
else:
    report = json.loads((ROOT / 'run/polaris-tests/village-report/layout.json').read_text())
SCALE, LEFT, TOP = 4, 74, 180
image = Image.new('RGB', (1080, 1240), '#f2f5f7')
draw = ImageDraw.Draw(image)
font_path = Path('C:/Windows/Fonts/segoeui.ttf')
bold_path = Path('C:/Windows/Fonts/segoeuib.ttf')
font = lambda size, bold=False: ImageFont.truetype(str(bold_path if bold else font_path), size)
def point(x, z): return LEFT + (x + 116) * SCALE, TOP + (z + 116) * SCALE

draw.text((LEFT, 36), 'POLARIS', font=font(18, True), fill='#427078')
draw.text((LEFT, 65), 'North Pole Village', font=font(38, True), fill='#18353e')
draw.text((LEFT, 117), 'Central plaza: X 0, Z -1000  |  28 buildings + town tree  |  4 house styles', font=font(18), fill='#48606b')
draw.rounded_rectangle((LEFT - 1, TOP - 1, LEFT + 233 * SCALE + 1, TOP + 233 * SCALE + 1), radius=8, fill='#ffffff', outline='#cfdbdf')
for x, z in report['roads']:
    px, py = point(x, z)
    draw.rectangle((px, py, px + SCALE - 1, py + SCALE - 1), fill='#c7d2d8')
colors = {'cottage':'#b45c67', 'townhouse':'#825f69', 'town_tree':'#397a68', 'toy_workshop':'#ba8844',
          'lodge':'#5a7c99', 'gift_shop':'#9b739a', 'restaurant':'#b76d48', 'reindeer_stable':'#7c895c',
          'alpine_house':'#517f9a', 'cranberry_cottage':'#a64163'}
labels = {'cottage':'Cottage', 'townhouse':'Townhouse', 'town_tree':'Tree / plaza', 'toy_workshop':'Toy workshop',
          'lodge':'Lodge', 'gift_shop':'Gift shop', 'restaurant':'Restaurant', 'reindeer_stable':'Stable / paddock',
          'alpine_house':'Alpine house', 'cranberry_cottage':'Cranberry'}
for plot in report['plots']:
    kind = plot['template'].removeprefix('christmas_')
    x, y = point(plot['x'], plot['z'])
    w, h = plot['width'] * SCALE, plot['depth'] * SCALE
    draw.rounded_rectangle((x + 1, y + 1, x + w - 2, y + h - 2), radius=5, fill=colors[kind])
    draw.text((x + w / 2, y + h / 2), labels[kind], font=font(12, True), fill='white', anchor='mm')
    ex, ey = point(plot['entry_x'], plot['entry_z'])
    draw.ellipse((ex - 3, ey - 3, ex + 4, ey + 4), fill='#ffe6a7')
px, py = point(0, 0)
draw.text((px, py + 17), '(0, -1000)', font=font(11), fill='#e1f3e9', anchor='mm')
draw.line((1017, 225, 1017, 189), fill='#18353e', width=3)
draw.polygon(((1017,181),(1011,194),(1023,194)), fill='#18353e')
draw.text((1017, 156), 'N', font=font(17, True), fill='#18353e', anchor='mm')
draw.text((LEFT, 1135), 'Narrow branching paths connect mixed, staggered plots; gold markers show entrances.', font=font(17), fill='#48606b')
draw.text((LEFT, 1165), 'Terrain levels to the plaza, then blends into the surrounding snowy plains.', font=font(17), fill='#48606b')
draw.text((LEFT, 1195), 'Design preview.' if '--plan' in sys.argv else 'Layout exported from the passing server test. This is a plan view, not an in-game screenshot.', font=font(13), fill='#6c818b')
output = ROOT / 'docs/polaris-village-layout.png'
output.parent.mkdir(exist_ok=True)
image.save(output)
print(output)

