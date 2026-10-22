# Generates the hearing ring for blockers behind walls: textures/gui/sprites/hud/blocker_ring.png (32x32).
# A white ring with soft edges, tinted the blocker's color when drawn.
# Run from the project root: python tools/gen_ring.py
from PIL import Image

SIZE = 32
RADIUS = 12.0
THICKNESS = 2.2
SOFT = 1.2

image = Image.new('RGBA', (SIZE, SIZE))
pixels = image.load()
centre = (SIZE - 1) / 2.0
for x in range(SIZE):
    for y in range(SIZE):
        d = abs(((x - centre) ** 2 + (y - centre) ** 2) ** 0.5 - RADIUS)
        t = min(1.0, max(0.0, (d - THICKNESS / 2.0) / SOFT))
        alpha = 1.0 - t * t * (3.0 - 2.0 * t)
        pixels[x, y] = (255, 255, 255, round(alpha * 255))
image.save('common/src/main/resources/assets/whycantisleep/textures/gui/sprites/hud/blocker_ring.png')
