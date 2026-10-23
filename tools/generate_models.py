"""Generate shared native Minecraft model geometry, not combination textures.

The supplied saddle and flute silhouettes are represented as colored pixel regions.
One 16x16 white shader sampler serves every region; palette changes are native tints.
Run with Python 3 from the project root. No third-party package is required.
"""
from pathlib import Path
import json
import struct
import zlib

ROWS = (
    '0000000000000000',
    '000aad00000a0a00',
    '00a122d00a0aaf0a',
    '00d2d2d000a52df0',
    '0a22111dfaecc2af',
    'a22ada12ece77c2f',
    'a2addf21e8ec7e2f',
    'a2adffa1ec88ce2f',
    'd5adffa21eeee2af',
    '0dfdfffa21122aef',
    '000ffeffeaaaaef0',
    '00000cf0fffcff00',
    '000009f00007f000',
    '000006b00004f000',
    '00000bf00003b000',
    '00000000000bf000',
)

# Flute roles: 1..3 shaded scales, 4 bright flat dots, 5 shadowed recess,
# 7 a slightly lighter recess corner, 8..9 original outline, 0 transparent.
# Slot 6 remains reserved. Coordinates and the original silhouette are retained.
FLUTE_ROWS = (
    '0000000000000000',
    '0000000000000000',
    '0000000000000000',
    '0000088808808800',
    '0000081188288390',
    '0000008288233990',
    '0000008811429900',
    '0000881112233900',
    '0088111142999000',
    '0811111239889000',
    '0815514399390000',
    '0825723900990000',
    '0092339000000000',
    '0009990000000000',
    '0000000000000000',
    '0000000000000000',
)

# Native tint indices 1..6 are horns. The selected scale mark uses 7, 8,
# and 20 (four adjoining shadows). All other original leather stays brown.
# Index 19 restores the original bright brown on the lower strap at L13.
MARK_SHADOWS = {(9, 5), (11, 6), (9, 7), (12, 7)}
# User-selected centers C14, I2, N11, converted to zero-based x/y.
# Each is a static 3x3 cross with a bright center and four pale arms.
SPARKLE_CENTERS = ((2, 13), (8, 1), (13, 10))
SPARKLES = tuple(
    (x + dx, y + dy, 16 if (dx, dy) == (0, 0) else 17)
    for x, y in SPARKLE_CENTERS
    for dx, dy in ((0, 0), (-1, 0), (1, 0), (0, -1), (0, 1))
)


def pixel_rect(x, y, width, tint, depth=8.0):
    face = {'uv': [0, 0, 16, 16], 'texture': '#white', 'tintindex': tint}
    return {
        'from': [x, 15 - y, depth - 0.05],
        'to': [x + width, 16 - y, depth + 0.05],
        'shade': False,
        'faces': {'north': dict(face), 'south': dict(face)},
    }


SWAPPED_TINTS = {1: 24, 2: 25, 3: 26, 4: 27, 5: 28, 6: 29,
                 7: 21, 8: 22, 20: 23}


def base_elements(swapped=False):
    result = []
    for y, row in enumerate(ROWS):
        x = 0
        while x < 16:
            end = x + 1
            while end < 16 and base_tint(end, y) == base_tint(x, y):
                end += 1
            tint = base_tint(x, y)
            if tint:
                if swapped:
                    tint = SWAPPED_TINTS.get(tint, tint)
                result.append(pixel_rect(x, y, end - x, tint))
            x = end
    return result


def base_tint(x, y):
    if (x, y) in MARK_SHADOWS:
        return 20
    if (x, y) == (11, 12):
        return 19
    return int(ROWS[y][x], 16)


def flute_elements():
    result = []
    for y, row in enumerate(FLUTE_ROWS):
        x = 0
        while x < 16:
            end = x + 1
            while end < 16 and row[end] == row[x]:
                end += 1
            tint = int(row[x])
            if tint:
                result.append(pixel_rect(x, y, end - x, tint))
            x = end
    return result


def geometry(elements):
    return {
        'gui_light': 'front',
        'textures': {'white': 'wyverncolors:item/white', 'particle': '#white'},
        'display': {'gui': {'rotation': [0, 0, 0], 'translation': [0, 0, 0],
                            'scale': [1, 1, 1]},
                    # Match Minecraft 1.21.11 item/generated, inherited by the
                    # active Wynncraft pack. Missing left transforms use the
                    # native right-hand fallback and left-hand mirroring.
                    'thirdperson_righthand': {
                        'rotation': [0, 0, 0], 'translation': [0, 3, 1],
                        'scale': [0.55, 0.55, 0.55]},
                    'firstperson_righthand': {
                        'rotation': [0, -90, 25], 'translation': [1.13, 3.2, 1.13],
                        'scale': [0.68, 0.68, 0.68]}},
        'elements': elements,
    }


def item_model(name, tint_count=21):
    return {
        'type': 'minecraft:model', 'model': f'wyverncolors:item/{name}',
        'tints': [{'type': 'minecraft:custom_model_data', 'index': index,
                   'default': 16777215} for index in range(tint_count)],
    }


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, separators=(',', ':')) + '\n', encoding='utf-8')


def white_sampler():
    # A constant white shader input; all artwork lives in model geometry.
    def chunk(kind, data):
        return (struct.pack('>I', len(data)) + kind + data
                + struct.pack('>I', zlib.crc32(kind + data) & 0xFFFFFFFF))
    return (b'\x89PNG\r\n\x1a\n'
            + chunk(b'IHDR', struct.pack('>IIBBBBB', 16, 16, 8, 6, 0, 0, 0))
            + chunk(b'IDAT', zlib.compress((b'\x00' + b'\xff' * 64) * 16))
            + chunk(b'IEND', b''))


def main():
    root = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/wyverncolors'
    write_json(root / 'models/item/saddle.json', geometry(base_elements()))
    write_json(root / 'models/item/saddle_swapped.json', geometry(base_elements(swapped=True)))
    write_json(root / 'models/item/sparkles.json', geometry([
        pixel_rect(x, y, 1, tint, 8.2) for x, y, tint in SPARKLES]))
    write_json(root / 'items/saddle.json', {'model': item_model('saddle')})
    write_json(root / 'items/saddle_mystic.json', {'model': {
        'type': 'minecraft:composite',
        'models': [item_model('saddle'), item_model('sparkles')],
    }})
    write_json(root / 'items/saddle_swapped.json', {'model': item_model('saddle_swapped', 30)})
    write_json(root / 'items/saddle_mystic_swapped.json', {'model': {
        'type': 'minecraft:composite',
        'models': [item_model('saddle_swapped', 30), item_model('sparkles', 30)],
    }})
    write_json(root / 'models/item/flute.json', geometry(flute_elements()))
    write_json(root / 'items/flute.json', {'model': item_model('flute', 10)})
    white = root / 'textures/item/white.png'
    white.parent.mkdir(parents=True, exist_ok=True)
    white.write_bytes(white_sampler())
    print(f'Generated shared saddle, sparkle, and flute models: '
          f'{len(base_elements())} saddle and {len(flute_elements())} flute rectangles.')


if __name__ == '__main__':
    main()
