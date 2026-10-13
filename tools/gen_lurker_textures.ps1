# Draws the Lurker's two textures under assets/whycantisleep/textures/entity/lurker/:
#   lurker.png       its skin: near-black hide with a grain and veins, ribs and vertebrae showing pale through it, bone
#                    for its horns, claws and ridge, a mouth full of teeth and two pits where its eyes are
#   lurker_eyes.png  only its eyes, drawn glowing over the skin (LurkerRenderer's eyes layer): a ring of cold violet
#                    light around a core left black, so each eye is a lit hole
# LurkerModel lays its parts out on a 64x64 sheet (texOffs and box sizes); the textures are drawn at twice that, so
# every model pixel is 2x2 texels. Coordinates below are the model's unless they say "texel". Change both together.
# Run from the repo root: powershell -File tools/gen_lurker_textures.ps1
Add-Type -AssemblyName System.Drawing

$scale = 2
$size = 64 * $scale
$random = New-Object System.Random 7331
$skin = New-Object System.Drawing.Bitmap $size, $size
$eyes = New-Object System.Drawing.Bitmap $size, $size
$clear = [System.Drawing.Color]::FromArgb(0, 0, 0, 0)
for ($y = 0; $y -lt $size; $y++) { for ($x = 0; $x -lt $size; $x++) { $skin.SetPixel($x, $y, $clear); $eyes.SetPixel($x, $y, $clear) } }

function Clamp($v) { [Math]::Max(0, [Math]::Min(255, [int]$v)) }
function Rgb($r, $g, $b) { [System.Drawing.Color]::FromArgb(255, (Clamp $r), (Clamp $g), (Clamp $b)) }

# A box's unwrapped area: width 2 * (depth + width), height depth + height, from its texOffs. Filled with a grain,
# darker towards the foot of the box so its underside falls away.
function Fill-Box($u, $v, $w, $h, $d, $base, $grain) {
    $x0 = $u * $scale; $y0 = $v * $scale
    $x1 = ($u + 2 * ($d + $w)) * $scale; $y1 = ($v + $d + $h) * $scale
    for ($y = $y0; $y -lt $y1; $y++) {
        for ($x = $x0; $x -lt $x1; $x++) {
            $n = $random.Next(-$grain, $grain + 1)
            $shade = ($y - $y0) * -0.22
            $skin.SetPixel($x, $y, (Rgb ($base[0] + $n + $shade) ($base[1] + $n + $shade) ($base[2] + $n + $shade + 3)))
        }
    }
}
# One texel of the skin, in texel coordinates.
function Dot($tx, $ty, $color) { if ($tx -ge 0 -and $ty -ge 0 -and $tx -lt $size -and $ty -lt $size) { $skin.SetPixel($tx, $ty, $color) } }
# A thin wandering line of a color over an area, in model coordinates: veins.
function Veins($u, $v, $w, $h, $count, $color) {
    for ($i = 0; $i -lt $count; $i++) {
        $x = ($u + $random.NextDouble() * $w) * $scale; $y = $v * $scale
        while ($y -lt ($v + $h) * $scale) {
            Dot ([int]$x) ([int]$y) $color
            $x += $random.Next(-1, 2); $y += 1
            if ($x -lt $u * $scale -or $x -ge ($u + $w) * $scale) { break }
        }
    }
}

$hide = @(18, 16, 26)
$bone = @(150, 142, 128)
$vein = Rgb 46 22 62
$pale = Rgb 74 68 86

Fill-Box 0 0 7 5 6 $hide 5        # head
Fill-Box 0 11 7 2 6 $hide 5       # jaw
Fill-Box 0 19 8 5 8 $hide 5       # chest
Fill-Box 0 32 6 4 7 @(15, 13, 22) 5   # belly
Fill-Box 28 32 2 2 5 @(13, 12, 19) 4  # tail
Fill-Box 32 0 2 7 2 @(14, 13, 20) 4   # upper limbs
Fill-Box 40 0 2 15 2 @(14, 13, 20) 4  # lower limbs
Fill-Box 48 0 2 2 2 $bone 10      # the ridge down its back
Fill-Box 48 4 1 1 3 $bone 10      # claws
Fill-Box 52 4 1 4 1 @(96, 90, 100) 8  # ribs
Fill-Box 48 10 1 1 4 $bone 10     # horns

# Veins over the hide, and the bones under it: vertebrae along the top of the chest and belly, ribs down their sides.
Veins 0 19 32 13 14 $vein
Veins 0 32 26 11 8 $vein
Veins 0 0 26 11 7 $vein
Veins 40 2 8 15 6 $vein
# Chest top face is at (depth, 0) from its texOffs: (8, 19), 8 wide and 8 deep. A row of vertebrae down its middle.
for ($z = 0; $z -lt 8; $z++) { Dot ((8 + 4) * $scale - 1) ((19 + $z) * $scale) $pale; Dot ((8 + 4) * $scale) ((19 + $z) * $scale) $pale }
# Chest sides: the right one at (0, 19 + 8), the left one at (8 + 8, 19 + 8), each 8 wide and 5 high. Ribs showing through.
foreach ($sx in @(0, 16)) { for ($r = 0; $r -lt 4; $r++) { for ($y = 0; $y -lt 5 * $scale - 2; $y++) { Dot (($sx + 1 + $r * 2) * $scale) ((27) * $scale + 1 + $y) $pale } } }

# The head's front face is at (depth, depth) from its texOffs: model (6, 6), 7 wide and 5 high, so texels (12, 12) on.
$fx = 6 * $scale; $fy = 6 * $scale
$pit = Rgb 1 1 3
# Each eye is a pit 4 texels square; they sit a model pixel in from each side, a model pixel down.
foreach ($ex in @(2, 8)) {
    for ($y = 0; $y -lt 4; $y++) { for ($x = 0; $x -lt 4; $x++) { Dot ($fx + $ex + $x) ($fy + 2 + $y) $pit } }
    # A dark hollow around the pit, so the eye sinks into the skull.
    for ($x = -1; $x -le 4; $x++) { Dot ($fx + $ex + $x) ($fy + 1) (Rgb 8 7 12); Dot ($fx + $ex + $x) ($fy + 6) (Rgb 8 7 12) }
    for ($y = 2; $y -le 5; $y++) { Dot ($fx + $ex - 1) ($fy + $y) (Rgb 8 7 12); Dot ($fx + $ex + 4) ($fy + $y) (Rgb 8 7 12) }
}
# Upper teeth along the foot of the head's face, lower teeth along the top of the jaw's (the jaw's front is at (6, 17)).
$tooth = Rgb 196 188 170
for ($x = 1; $x -lt 13; $x += 2) { Dot ($fx + $x) ($fy + 9) $tooth; Dot ($fx + $x) ($fy + 8) (Rgb 150 142 128) }
$jy = 17 * $scale
for ($x = 2; $x -lt 13; $x += 2) { Dot ($fx + $x) $jy $tooth; Dot ($fx + $x) ($jy + 1) (Rgb 150 142 128) }
# The inside of the mouth: the jaw's top face, at (6, 11), 7 wide and 6 deep. Dark red, darker towards the throat.
for ($z = 0; $z -lt 6 * $scale; $z++) { for ($x = 0; $x -lt 7 * $scale; $x++) { Dot ($fx + $x) (11 * $scale + $z) (Rgb (70 - $z * 4 + $random.Next(-6, 7)) 8 14) } }

# The eyes: a ring of cold light around a core left unlit. The ring is brightest at its inner corners.
$rim = [System.Drawing.Color]::FromArgb(255, 132, 78, 255)
$hot = [System.Drawing.Color]::FromArgb(255, 226, 206, 255)
foreach ($ex in @(2, 8)) {
    for ($y = 0; $y -lt 4; $y++) {
        for ($x = 0; $x -lt 4; $x++) {
            $edge = ($x -eq 0 -or $x -eq 3 -or $y -eq 0 -or $y -eq 3)
            $corner = (($x -eq 0 -or $x -eq 3) -and ($y -eq 0 -or $y -eq 3))
            if ($corner) { continue }
            if ($edge) { $eyes.SetPixel($fx + $ex + $x, $fy + 2 + $y, $(if ($y -eq 0 -or $x -eq 0) { $hot } else { $rim })) }
        }
    }
}

$dir = Join-Path (Get-Location) "common\src\main\resources\assets\whycantisleep\textures\entity\lurker"
New-Item -ItemType Directory -Force $dir | Out-Null
$skin.Save((Join-Path $dir "lurker.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$eyes.Save((Join-Path $dir "lurker_eyes.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$skin.Dispose(); $eyes.Dispose()
"Wrote $dir"
