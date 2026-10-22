# Generates the sleep status button in the inventory's player preview: textures/gui/sprites/hud/status_button.png,
# status_button_hover.png and status_button_open.png (10x10). A rounded slate tile with a rim and a pale gold crescent moon.
# The hovered tile is lighter with a bright rim, the open one has a gold rim. The dot of the player's sleep trouble is
# drawn over it by the game.
# Run from the project root: python tools/gen_status_button.py
from PIL import Image

SIZE = 10
OUT = 'common/src/main/resources/assets/whycantisleep/textures/gui/sprites/hud/'
MOON = (0xF2, 0xE2, 0x9A, 255)
MOON_SHADE = (0xC9, 0xB2, 0x5E, 255)
# The crescent: the pixels in the moon's circle and out of the circle that bites it.
MOON_CENTRE, MOON_RADIUS = (4.6, 5.0), 3.4
BITE_CENTRE, BITE_RADIUS = (6.4, 3.9), 2.9


def inside(x, y, centre, radius):
    return ((x + 0.5 - centre[0]) ** 2 + (y + 0.5 - centre[1]) ** 2) ** 0.5 <= radius


def button(fill, rim, highlight):
    image = Image.new('RGBA', (SIZE, SIZE))
    pixels = image.load()
    last = SIZE - 1
    for x in range(SIZE):
        for y in range(SIZE):
            if (x in (0, last)) and (y in (0, last)):
                continue
            edge = x in (0, last) or y in (0, last)
            pixels[x, y] = rim if edge else fill
    # A lighter line along the top inside the rim, as the game's buttons have.
    for x in range(1, last):
        pixels[x, 1] = highlight
    for x in range(SIZE):
        for y in range(SIZE):
            if inside(x, y, MOON_CENTRE, MOON_RADIUS) and not inside(x, y, BITE_CENTRE, BITE_RADIUS):
                # The crescent's outer edge is shaded, so it reads round.
                outer = not inside(x, y, MOON_CENTRE, MOON_RADIUS - 1.0)
                pixels[x, y] = MOON_SHADE if outer and x < MOON_CENTRE[0] and y > MOON_CENTRE[1] else MOON
    return image


button((0x1B, 0x1E, 0x24, 230), (0x4B, 0x4F, 0x57, 255), (0x2A, 0x2E, 0x36, 255)).save(OUT + 'status_button.png')
button((0x2A, 0x2E, 0x36, 240), (0xD4, 0xD4, 0xD4, 255), (0x3A, 0x3F, 0x48, 255)).save(OUT + 'status_button_hover.png')
button((0x22, 0x25, 0x2B, 240), (0xE0, 0xB8, 0x4A, 255), (0x33, 0x37, 0x40, 255)).save(OUT + 'status_button_open.png')
