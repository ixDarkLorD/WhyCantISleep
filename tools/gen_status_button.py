# Generates the sleep status button in the inventory's player preview, 8x8: status_button.png, status_button_hover.png
# and status_button_open.png under textures/gui/sprites/hud/, their tired twins status_button_tired*.png, shown while
# the player is restless (or after a poor night), and their sad ones status_button_sad*.png, shown when their sleep is
# awful (sleepless, or an awful last night). A dark tile with a round, sleeping moon face,
# drawn pixel by pixel and lit from the top left: a highlight along its upper left edge, its gold body, a mid shade and
# a darker one toward the lower right, softened corners, closed eyes, a touch of blush and a smile. The sad one
# has its eyes higher, a tear and a frown, in the same gold; the tired one dark bags under its eyes in
# place of the blush. Its outermost ring is left clear: the game
# draws the rim there, in the color of the player's sleep trouble. The hovered tile and moon are a step lighter, the
# open ones lighter still.
# Run from the project root: python tools/gen_status_button.py
from PIL import Image

SIZE = 8
OUT = 'common/src/main/resources/assets/whycantisleep/textures/gui/sprites/hud/'
# The 6x6 inside the rim, rows top to bottom. H highlight, M body, m mid shade, d dark shade, c a softened corner,
# e eyes and mouth, b blush, g bags under the eyes, t a tear, a space the tile.
FACE = ["cHHMMc",
        "HHMMMm",
        "HeMMem",
        "MbMMbd",
        "meMMed",
        "cmeemc"]
TIRED = ["cHHMMc",
         "HHMMMm",
         "HeMMem",
         "MgMMgd",
         "mMMMmd",
         "cmeemc"]
SAD = ["cHHMMc",
       "HeMMem",
       "HtMMMm",
       "MMeeMd",
       "meMMed",
       "cmddmc"]
BASE = {
    'H': (0xFF, 0xF6, 0xCF),
    'M': (0xF2, 0xE2, 0x9A),
    'm': (0xD6, 0xBF, 0x6A),
    'd': (0xA8, 0x8A, 0x3E),
    'e': (0x5A, 0x46, 0x1E),
    'b': (0xF0, 0xA9, 0x8A),
    't': (0x7A, 0xB4, 0xF0),
    'g': (0x9C, 0x86, 0x6E),
}
TILES = [((0x16, 0x19, 0x1F), 235, 0.0, ''), ((0x24, 0x28, 0x30), 245, 0.12, '_hover'), ((0x2C, 0x31, 0x3B), 250, 0.22, '_open')]


def mix(a, b, t):
    return tuple(round(x + (y - x) * t) for x, y in zip(a, b))


def button(face, tile, tile_alpha, lighten):
    colors = {k: v if k in ('e', 't') else mix(v, (0xFF, 0xFF, 0xFF), lighten) for k, v in BASE.items()}
    # A softened corner: halfway between the tile and the moon's shade there.
    colors['c'] = mix(tile, colors['m'], 0.45)
    image = Image.new('RGBA', (SIZE, SIZE))
    pixels = image.load()
    for y, row in enumerate(face, 1):
        for x, c in enumerate(row, 1):
            pixels[x, y] = colors[c] + (255,) if c in colors else tile + (tile_alpha,)
    return image


for tile, alpha, lighten, suffix in TILES:
    button(FACE, tile, alpha, lighten).save(OUT + 'status_button' + suffix + '.png')
    button(TIRED, tile, alpha, lighten).save(OUT + 'status_button_tired' + suffix + '.png')
    button(SAD, tile, alpha, lighten).save(OUT + 'status_button_sad' + suffix + '.png')
