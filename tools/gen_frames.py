# Generates the HUD's two frames as pixel art, inspired by the heartbeat minigame's band (tools/gen_minigame_fx.py):
# lit along the top by a bright rim with a softer row under it, a lighter top stepping down to the body, a darker
# bottom with a shadowed rim, and a shadow cast under it; grey rather than the band's embers. Every step between tones is one clean row of checkerboard, so there are no stray pixels.
#   notice.png  the message cards and the bedtime panel: moonlit, a pale rim, square with notched corners
#   card.png    the sleep quality card and the status popup: dusk smoke, its top and bottom edges wavy like the band's
#               smoke
# Both grey (monochrome), tinted a little towards each panel's own color when drawn (HudStyle.panel).
# Both are SIZE square nine-slices with a BORDER border (written to each .mcmeta), so the middle tile is 16 across and
# the waves, 16 and 8 long, join up however far it repeats; all the shading keeps to the border, as the middle rows
# are repeated to make a frame taller and must be flat. HudStyle.NOTICE_BORDER and CARD_BORDER say how far in a
# wash must keep to stay under the rims.
# Run from the project root: python tools/gen_frames.py (replaces what tools/gen_hud_textures.ps1 made for these two)
import json
import math

from PIL import Image

OUT = 'common/src/main/resources/assets/whycantisleep/textures/gui/sprites/hud/'
SIZE, BORDER = 28, 6
SHADOW = (0, 0, 0, 140)

# Grey, so HudStyle can tint each panel a little towards its message's color (the wash's) as it draws it.
MOONLIT = {
    'rim': (214, 214, 214, 250), 'soft': (112, 112, 112, 240), 'top': (40, 40, 40, 236), 'body': (18, 18, 18, 236),
    'deep': (9, 9, 9, 240), 'shadow_rim': (4, 4, 4, 245), 'wave': lambda x: 0,
}
DUSK = {
    'rim': (196, 196, 196, 245), 'soft': (98, 98, 98, 235), 'top': (36, 36, 36, 236), 'body': (20, 20, 20, 236),
    'deep': (10, 10, 10, 240), 'shadow_rim': (4, 4, 4, 245),
    'wave': lambda x: round(0.6 * math.sin(2 * math.pi * x / 16) + 0.4 * math.sin(2 * math.pi * x / 8 + 1.3)),
}


def checker(x, y):
    return (x + y) % 2 == 0


def frame(style):
    image = Image.new('RGBA', (SIZE, SIZE))
    pixels = image.load()
    wave = style['wave']
    for x in range(SIZE):
        # The corners notched: two pixels in at the outer column, one at the next.
        inset = 2 if x in (0, SIZE - 1) else 1 if x in (1, SIZE - 2) else 0
        top = 1 + wave(x) + inset
        bottom = SIZE - 3 - wave(x + 5) - inset
        side = x in (0, SIZE - 1)
        for y in range(top, bottom + 1):
            depth, rise = y - top, bottom - y
            if side:
                color = style['soft'] if depth < 3 else style['shadow_rim']
            elif depth == 0:
                color = style['rim']
            elif depth == 1:
                color = style['soft']
            elif depth == 2:
                color = style['top']
            elif depth == 3:
                color = style['top'] if checker(x, y) else style['body']
            elif rise == 0:
                color = style['shadow_rim']
            elif rise == 1:
                color = style['deep']
            elif rise == 2:
                color = style['deep'] if checker(x, y) else style['body']
            else:
                color = style['body']
            # The rows the game repeats to make a frame taller are one flat tone, or they would stripe it.
            if BORDER <= y < SIZE - BORDER:
                color = style['shadow_rim'] if side else style['body']
            pixels[x, y] = color
        # Its shadow: one checkered row under it.
        if bottom + 1 < SIZE and checker(x, bottom + 1) and not side:
            pixels[x, bottom + 1] = SHADOW
    return image


def save(name, style):
    frame(style).save(OUT + name + '.png')
    meta = {'gui': {'scaling': {'type': 'nine_slice', 'width': SIZE, 'height': SIZE, 'border': BORDER}}}
    with open(OUT + name + '.png.mcmeta', 'w', encoding='utf-8', newline='\n') as file:
        json.dump(meta, file, indent=2)
        file.write('\n')


save('notice', MOONLIT)
save('card', DUSK)
