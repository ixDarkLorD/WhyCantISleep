# Draws the Lurker's two textures under assets/whycantisleep/textures/entity/lurker/:
#   lurker.png       its skin: night-dark armor in overlapping plates, every face shaded for where it points (lit from
#                    above, dark beneath, dimmed along its edges), veins of crimson through it, ribs showing crimson,
#                    navy horn for its horns, claws, spurs, ridge and tail blade, a mouth full of teeth, two sockets,
#                    and the eyes that sit in them (an eyeball, a slit pupil and a lid each)
#   lurker_eyes.png  what glows, drawn over the skin (LurkerRenderer's glow layer, which pulses): the eyeballs, red
#                    with a hotter heart; every vein, red; the embers down its back and in the cracks of its knees
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
# Horn: the navy of the palette, lifted so it stands off the hide.
$bone = @(58, 78, 118)

# One texel of the skin, in texel coordinates, and the same texel of the glowing layer: lit with a glow, and put
# out without one, so a detail painted over a vein (a tooth, a socket) does not go on glowing like the vein.
function Dot($tx, $ty, $color, $glow = $null) {
    if ($tx -lt 0 -or $ty -lt 0 -or $tx -ge $size -or $ty -ge $size) { return }
    $skin.SetPixel($tx, $ty, $color)
    $eyes.SetPixel($tx, $ty, $(if ($glow -ne $null) { $glow } else { $clear }))
}

# A box's six faces, unwrapped from its texOffs (u, v) for a box w wide, h high and d deep. Each face is painted for
# where it points: the top catches the light, the sides less, the back less still and the underside almost none; it
# darkens a little down its height and along its edges, where one face meets the next; and it carries a grain.
# $pattern lays armor over that:
#   plates  seams across each face's height, a dark line with a lit lip under it: overlapping plates, or the bands
#           of a limb
#   rings   seams across the box's depth instead: the lengths of a tail or a horn
# and $ridge lights a line down the middle of the top and front, where the plates meet.
function Box($u, $v, $w, $h, $d, $base, $grain, $pattern = "", $period = 4, [bool]$ridge = $false) {
    # Each face: its place and size in model pixels, how lit it is, and which way the box's depth runs across it
    # ("y" down the face, "x" along it, "" not at all).
    $faces = @(
        @(($u + $d), $v, $w, $d, 1.28, "y", $true),
        @(($u + $d + $w), $v, $w, $d, 0.5, "y", $false),
        @($u, ($v + $d), $d, $h, 0.8, "x", $false),
        @(($u + $d), ($v + $d), $w, $h, 1.0, "", $true),
        @(($u + $d + $w), ($v + $d), $d, $h, 0.8, "x", $false),
        @(($u + $d + $w + $d), ($v + $d), $w, $h, 0.66, "", $false)
    )
    foreach ($face in $faces) {
        $x0 = $face[0] * $scale; $y0 = $face[1] * $scale
        $fw = [int]($face[2] * $scale); $fh = [int]($face[3] * $scale)
        $top = ($face[5] -eq "y")
        for ($ty = 0; $ty -lt $fh; $ty++) {
            for ($tx = 0; $tx -lt $fw; $tx++) {
                $f = $face[4]
                # Tops round off towards their sides; every other face darkens towards its foot.
                if ($top) { $f *= 1.0 - 0.14 * [Math]::Abs($tx - ($fw - 1) / 2.0) / [Math]::Max(1.0, $fw / 2.0) }
                else { $f *= 1.0 - 0.2 * $ty / [Math]::Max(1, $fh - 1) }
                if ($fw -ge 4 -and $fh -ge 4) {
                    $border = [Math]::Min([Math]::Min($tx, $ty), [Math]::Min($fw - 1 - $tx, $fh - 1 - $ty))
                    if ($border -eq 0) { $f *= 0.74 } elseif ($border -eq 1) { $f *= 0.9 }
                }
                $along = -1
                if ($pattern -eq "plates") { $along = $ty }
                elseif ($pattern -eq "rings") { if ($face[5] -eq "y") { $along = $ty } elseif ($face[5] -eq "x") { $along = $tx } }
                if ($along -ge 0) {
                    $m = $along % $period
                    if ($m -eq 0) { $f *= 0.55 } elseif ($m -eq 1) { $f *= 1.28 }
                }
                if ($ridge -and $face[6] -and [Math]::Abs($tx - ($fw - 1) / 2.0) -lt 1.0) { $f *= 1.3 }
                $n = $random.Next(-$grain, $grain + 1)
                $skin.SetPixel($x0 + $tx, $y0 + $ty, (Rgb ($base[0] * $f + $n) ($base[1] * $f + $n) ($base[2] * $f + $n)))
            }
        }
    }
}

# Thin wandering lines down an area, in model coordinates: veins, and every one of them glows. On the skin a vein
# is crimson; on the glowing layer it is red along its whole length, hotter at a knot now and then, with a dim red
# spilling onto the hide on either side of it so the light looks like it comes from under the skin.
function Veins($u, $v, $w, $h, $count) {
    $veinGlow = Rgb 235 24 34
    $knotGlow = Rgb 255 92 70
    $spill = Rgb 46 2 6
    for ($i = 0; $i -lt $count; $i++) {
        $x = ($u + 0.5 + $random.NextDouble() * ($w - 1)) * $scale; $y = $v * $scale
        while ($y -lt ($v + $h) * $scale) {
            $tx = [int]$x; $ty = [int]$y
            $knot = $random.Next(0, 8) -eq 0
            foreach ($side in @(-1, 1)) {
                $sx = $tx + $side
                # The spill only ever lights the hide: it leaves a neighboring vein's own light alone.
                if ($sx -ge $u * $scale -and $sx -lt ($u + $w) * $scale -and $eyes.GetPixel($sx, $ty).A -eq 0) { $eyes.SetPixel($sx, $ty, $spill) }
            }
            Dot $tx $ty $(if ($knot) { Shade $red 0.8 } else { Shade $crimson 0.95 }) $(if ($knot) { $knotGlow } else { $veinGlow })
            # Mostly straight down, with a step to one side now and then.
            if ($random.Next(0, 3) -eq 0) { $x += $random.Next(-1, 2) }
            $y += 1
            if ($x -lt $u * $scale -or $x -ge ($u + $w) * $scale) { break }
        }
    }
}

Box 0 0 7 5 6 @(29, 29, 33) 3                  # head: the hide, lifted a little so its shading shows
Box 0 11 7 2 6 @(24, 24, 27) 3                  # jaw
Box 0 19 8 5 8 @(29, 29, 33) 3 "plates" 4      # chest
Box 0 32 6 4 7 @(26, 30, 44) 3 "rings" 4        # belly: segments, a shade towards navy
Box 28 32 2 2 5 @(24, 25, 31) 2 "rings" 5       # tail lengths
Box 32 0 2 7 2 @(23, 24, 29) 2 "plates" 6       # upper limbs
Box 40 0 2 15 2 @(23, 24, 29) 2 "plates" 6      # lower limbs
Box 48 0 2 3 2 $bone 5                          # the ridge down its back, and the spikes on its tail
Box 56 0 1 4 1 @(124, 12, 34) 6                 # ribs, crimson
Box 56 6 1 3 3 $bone 5 "rings" 3                # claws and knee spurs
Box 34 20 1 5 4 $bone 5 "rings" 3               # horns and the tail's blade
Box 0 52 9 1 7 @(34, 36, 46) 3 "rings" 4 $true  # the armor over its shoulders
Box 34 44 7 1 2 @(18, 18, 21) 2                 # its brow
Box 34 48 1 2 1 @(186, 168, 174) 6              # fangs: bone, duller than the teeth behind them
Box 40 50 3 3 3 @(20, 20, 24) 3                 # knee joints

# Enough of them to map its body, few enough that the dark hide between them still reads: a limb takes two.
Veins 0 27 32 5 8     # chest sides, front and back
Veins 8 19 8 8 2      # chest top, where it shows round the armor
Veins 7 52 9 7 3      # across the shoulder armor
Veins 0 39 26 4 5     # belly sides
Veins 7 32 6 7 2      # belly top
Veins 0 6 6 5 2       # head: one side,
Veins 13 6 13 5 3     # the other side and the back (its face is left clear),
Veins 6 0 7 6 2       # and the top of its skull
Veins 40 2 8 15 2     # lower limbs
Veins 32 2 8 7 1      # upper limbs
Veins 28 37 14 2 2    # tail

# Embers down the middle of the shoulder armor (its top face is at (7, 52), 9 wide and 7 deep), one to a plate.
$ember = Shade $crimson 0.9
for ($z = 0; $z -lt 7; $z++) {
    $ty = (52 + $z) * $scale
    if ($ty % 4 -ne 2) { continue }
    Dot (11 * $scale + 1) $ty $ember (Shade $red 0.85)
    Dot (11 * $scale) $ty $ember (Shade $crimson 0.8)
    Dot (11 * $scale + 2) $ty $ember (Shade $crimson 0.8)
}
# Cracks in the knee joints, lit from inside: a cross on each side face (they start at (40, 53), each 3 by 3).
for ($side = 0; $side -lt 4; $side++) {
    $cx = (40 + $side * 3) * $scale + 3; $cy = 53 * $scale + 3
    Dot $cx $cy (Shade $crimson 1.0) (Shade $red 0.95)
    foreach ($o in @(@(-1, 0), @(1, 0), @(0, -1), @(0, 1))) { Dot ($cx + $o[0]) ($cy + $o[1]) (Shade $crimson 0.7) (Shade $crimson 0.75) }
    Dot ($cx - 2) ($cy - 1) (Shade $maroon 0.8); Dot ($cx + 2) ($cy + 1) (Shade $maroon 0.8)
}
# Chest sides: the right one at (0, 27), the left one at (16, 27), each 8 wide and 5 high. The shadow each rib casts.
foreach ($sx in @(0, 16)) { for ($r = 0; $r -lt 3; $r++) { for ($y = 1; $y -lt 5 * $scale - 1; $y++) { Dot (($sx + 2 + $r * 2) * $scale + 1) (27 * $scale + $y) (Rgb 8 8 10) } } }

# The head's front face is at (depth, depth) from its texOffs: model (6, 6), 7 wide and 5 high, so texels (12, 12) on.
$fx = 6 * $scale; $fy = 6 * $scale
$pit = Rgb 2 2 4
$hollow = Shade $navy 0.45
# Each socket is a pit 4 texels square, a model pixel in from each side and a model pixel down; the eye sits in it.
foreach ($ex in @(2, 8)) {
    for ($y = 0; $y -lt 4; $y++) { for ($x = 0; $x -lt 4; $x++) { Dot ($fx + $ex + $x) ($fy + 2 + $y) $pit } }
    # A dark hollow around the pit, so the eye sinks into the skull, and a deeper shadow under it: a gaunt cheek.
    for ($x = -1; $x -le 4; $x++) { Dot ($fx + $ex + $x) ($fy + 1) $hollow; Dot ($fx + $ex + $x) ($fy + 6) $hollow }
    for ($y = 2; $y -le 5; $y++) { Dot ($fx + $ex - 1) ($fy + $y) $hollow; Dot ($fx + $ex + 4) ($fy + $y) $hollow }
    for ($x = 0; $x -le 3; $x++) { Dot ($fx + $ex + $x) ($fy + 7) (Rgb 10 10 12) }
}
# Two slits for a nose, between and below the eyes.
Dot ($fx + 6) ($fy + 5) $pit; Dot ($fx + 6) ($fy + 6) $pit
Dot ($fx + 7) ($fy + 5) $pit; Dot ($fx + 7) ($fy + 6) $pit
# Upper teeth along the foot of the head's face, each with a gum above it; lower teeth along the top of the jaw's
# (the jaw's front is at (6, 17)). Teeth are the one pale thing on it: bone, with the sunset on it.
$tooth = Rgb 226 204 208
$toothShade = Rgb 168 146 152
$gum = Shade $crimson 0.8
for ($x = 1; $x -lt 13; $x += 2) { Dot ($fx + $x) ($fy + 9) $tooth; Dot ($fx + $x) ($fy + 8) $gum; Dot ($fx + $x + 1) ($fy + 9) (Rgb 6 4 6) }
$jy = 17 * $scale
for ($x = 2; $x -lt 13; $x += 2) { Dot ($fx + $x) $jy $tooth; Dot ($fx + $x) ($jy + 1) $toothShade; Dot ($fx + $x) ($jy + 2) $gum }
# The inside of the mouth: the jaw's top face, at (6, 11), 7 wide and 6 deep. Crimson, darker towards the throat, with
# a tongue lying down the middle of it.
for ($z = 0; $z -lt 6 * $scale; $z++) { for ($x = 0; $x -lt 7 * $scale; $x++) {
    $tongue = ([Math]::Abs($x - 6.5) -lt 2.5 -and $z -lt 9)
    Dot ($fx + $x) (11 * $scale + $z) (Shade $crimson ((0.8 - $z * 0.05 + $random.NextDouble() * 0.08) * $(if ($tongue) { 1.3 } else { 0.8 })))
} }

# The eyes are parts of the model (LurkerModel.eye), each with a place of its own on the sheet. A box at texOffs
# (u, v), w wide, h high and d deep, has its front face at (u + d, v + d).
# The eyeball, at (0, 46), 2 by 2 by 1: deep red on the skin, so it is red even in daylight; and on the glowing layer
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
