# Draws the Lurker's two textures under assets/whycantisleep/textures/entity/lurker/:
#   lurker.png       its skin: night-dark hide with a grain, veins of maroon through it, ribs and vertebrae showing
#                    crimson, navy horn for its horns, claws, spurs, ridge and tail blade, a mouth full of teeth,
#                    two sockets, and the eyes that sit in them (an eyeball, a slit pupil and a lid each)
#   lurker_eyes.png  what glows, drawn over the skin (LurkerRenderer's eyes layer, which pulses): the eyeballs, red
#                    with a hotter heart; and the veins and the spine, faintly, like embers held under the skin
# The colors are the author's sunset palette (LurkerPalette.java has the same five).
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
function Shade($c, $f) { Rgb ($c[0] * $f) ($c[1] * $f) ($c[2] * $f) }

# The palette, darkest to lightest.
$abyss = @(24, 24, 26)
$navy = @(31, 45, 74)
$maroon = @(90, 36, 51)
$crimson = @(138, 10, 36)
$red = @(255, 45, 70)

# A box's unwrapped area: width 2 * (depth + width), height depth + height, from its texOffs. Filled with a grain,
# darker towards the foot of the box so its underside falls away.
function Fill-Box($u, $v, $w, $h, $d, $base, $grain) {
    $x0 = $u * $scale; $y0 = $v * $scale
    $x1 = ($u + 2 * ($d + $w)) * $scale; $y1 = ($v + $d + $h) * $scale
    for ($y = $y0; $y -lt $y1; $y++) {
        for ($x = $x0; $x -lt $x1; $x++) {
            $n = $random.Next(-$grain, $grain + 1)
            $shade = ($y - $y0) * -0.2
            $skin.SetPixel($x, $y, (Rgb ($base[0] + $n + $shade) ($base[1] + $n + $shade) ($base[2] + $n + $shade)))
        }
    }
}
# One texel of the skin, in texel coordinates; with a glow, the same texel of the eyes layer too.
function Dot($tx, $ty, $color, $glow = $null) {
    if ($tx -lt 0 -or $ty -lt 0 -or $tx -ge $size -or $ty -ge $size) { return }
    $skin.SetPixel($tx, $ty, $color)
    if ($glow -ne $null) { $eyes.SetPixel($tx, $ty, $glow) }
}
# Thin wandering lines down an area, in model coordinates: veins, which also glow faintly.
function Veins($u, $v, $w, $h, $count) {
    for ($i = 0; $i -lt $count; $i++) {
        $x = ($u + $random.NextDouble() * $w) * $scale; $y = $v * $scale
        while ($y -lt ($v + $h) * $scale) {
            Dot ([int]$x) ([int]$y) (Shade $maroon 0.9) (Shade $crimson 0.55)
            $x += $random.Next(-1, 2); $y += 1
            if ($x -lt $u * $scale -or $x -ge ($u + $w) * $scale) { break }
        }
    }
}

# Horn: the navy of the palette, lifted so it stands off the hide.
$bone = @(58, 78, 118)
Fill-Box 0 0 7 5 6 $abyss 4       # head
Fill-Box 0 11 7 2 6 $abyss 4      # jaw
Fill-Box 0 19 8 5 8 $abyss 4      # chest
Fill-Box 0 32 6 4 7 @(24, 28, 40) 4   # belly, a shade towards navy
Fill-Box 28 32 2 2 5 @(20, 21, 26) 3  # tail
Fill-Box 32 0 2 7 2 @(20, 21, 25) 3   # upper limbs
Fill-Box 40 0 2 15 2 @(20, 21, 25) 3  # lower limbs
Fill-Box 48 0 2 3 2 $bone 7       # the ridge down its back
Fill-Box 48 4 1 3 3 $bone 7       # claws and knee spurs
Fill-Box 52 4 1 4 1 @(104, 14, 34) 8  # ribs, crimson
Fill-Box 48 10 1 5 4 $bone 7      # horns and the tail's blade

Veins 0 19 32 13 16
Veins 0 32 26 11 9
Veins 0 0 26 11 8
Veins 40 2 8 15 7
Veins 32 2 8 7 4
Veins 28 34 14 5 4

$pale = Shade $crimson 0.75
$spark = Shade $red 0.7
# Chest top face is at (depth, 0) from its texOffs: (8, 19), 8 wide and 8 deep. A row of vertebrae down its middle,
# every other one lit.
for ($z = 0; $z -lt 8; $z++) {
    $lit = if ($z % 2 -eq 0) { $spark } else { $null }
    Dot ((8 + 4) * $scale - 1) ((19 + $z) * $scale) $pale $lit
    Dot ((8 + 4) * $scale) ((19 + $z) * $scale) $pale $lit
}
# Chest sides: the right one at (0, 27), the left one at (16, 27), each 8 wide and 5 high. Ribs showing through.
foreach ($sx in @(0, 16)) { for ($r = 0; $r -lt 4; $r++) { for ($y = 0; $y -lt 5 * $scale - 2; $y++) { Dot (($sx + 1 + $r * 2) * $scale) (27 * $scale + 1 + $y) $pale } } }

# The head's front face is at (depth, depth) from its texOffs: model (6, 6), 7 wide and 5 high, so texels (12, 12) on.
$fx = 6 * $scale; $fy = 6 * $scale
$pit = Rgb 2 2 4
$hollow = Shade $navy 0.45
# Each socket is a pit 4 texels square, a model pixel in from each side and a model pixel down; the eye sits in it.
foreach ($ex in @(2, 8)) {
    for ($y = 0; $y -lt 4; $y++) { for ($x = 0; $x -lt 4; $x++) { Dot ($fx + $ex + $x) ($fy + 2 + $y) $pit } }
    # A dark hollow around the pit, so the eye sinks into the skull.
    for ($x = -1; $x -le 4; $x++) { Dot ($fx + $ex + $x) ($fy + 1) $hollow; Dot ($fx + $ex + $x) ($fy + 6) $hollow }
    for ($y = 2; $y -le 5; $y++) { Dot ($fx + $ex - 1) ($fy + $y) $hollow; Dot ($fx + $ex + 4) ($fy + $y) $hollow }
}
# Upper teeth along the foot of the head's face, lower teeth along the top of the jaw's (the jaw's front is at (6, 17)).
# Teeth are the one pale thing on it: bone, with the sunset on it.
$tooth = Rgb 226 204 208
$root = Shade $crimson 0.9
for ($x = 1; $x -lt 13; $x += 2) { Dot ($fx + $x) ($fy + 9) $tooth; Dot ($fx + $x) ($fy + 8) $root }
$jy = 17 * $scale
for ($x = 2; $x -lt 13; $x += 2) { Dot ($fx + $x) $jy $tooth; Dot ($fx + $x) ($jy + 1) $root }
# The inside of the mouth: the jaw's top face, at (6, 11), 7 wide and 6 deep. Crimson, darker towards the throat.
for ($z = 0; $z -lt 6 * $scale; $z++) { for ($x = 0; $x -lt 7 * $scale; $x++) { Dot ($fx + $x) (11 * $scale + $z) (Shade $crimson (0.8 - $z * 0.05 + $random.NextDouble() * 0.08)) } }

# The eyes are parts of the model (LurkerModel.eye), each with a place of its own on the sheet. A box at texOffs
# (u, v), w wide, h high and d deep, has its front face at (u + d, v + d).
# The eyeball, at (0, 46), 2 by 2 by 1: crimson on the skin, so it is red even in daylight; and on the glowing layer
# red all over with a hotter heart on its front face.
# The glow is added to the skin under it, so both are kept short of blue: together they must still come out red.
$eyeHot = Rgb 255 96 70
$eyeRed = Rgb 255 26 30
$eyeDeep = Rgb 112 0 8
for ($y = 46 * $scale; $y -lt 49 * $scale; $y++) { for ($x = 0; $x -lt 6 * $scale; $x++) {
    Dot $x $y $eyeDeep (Rgb 200 16 22)
} }
for ($y = 0; $y -lt 4; $y++) { for ($x = 0; $x -lt 4; $x++) {
    $heart = ($x -ge 1 -and $x -le 2 -and $y -ge 1 -and $y -le 2)
    Dot (1 * $scale + $x) (47 * $scale + $y) $(if ($heart) { Rgb 150 4 12 } else { $eyeDeep }) $(if ($heart) { $eyeHot } else { $eyeRed })
} }
# The pupil, at (8, 46): a slit of nothing. It is not on the glowing layer, so it stays black against the eye's light.
for ($y = 46 * $scale; $y -lt 49 * $scale; $y++) { for ($x = 8 * $scale; $x -lt 12 * $scale; $x++) { Dot $x $y (Rgb 6 3 5) } }
# The lid, at (14, 46): hide, a shade darker than the head's, with a darker lip along its foot.
for ($y = 46 * $scale; $y -lt 50 * $scale; $y++) { for ($x = 14 * $scale; $x -lt 21 * $scale; $x++) {
    $n = $random.Next(-2, 3)
    $lip = if ($y -ge 49 * $scale - 1) { 0.55 } else { 0.85 }
    Dot $x $y (Rgb (($abyss[0] + $n) * $lip) (($abyss[1] + $n) * $lip) (($abyss[2] + $n) * $lip))
} }

$dir = Join-Path (Get-Location) "common\src\main\resources\assets\whycantisleep\textures\entity\lurker"
New-Item -ItemType Directory -Force $dir | Out-Null
$skin.Save((Join-Path $dir "lurker.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$eyes.Save((Join-Path $dir "lurker_eyes.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$skin.Dispose(); $eyes.Dispose()
"Wrote $dir"
