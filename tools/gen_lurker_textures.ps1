# Draws the Lurker's textures under assets/whycantisleep/textures/entity/lurker/, a pair for each of its kinds
# (LurkerKind.java): <kind>.png and <kind>_eyes.png. Each kind is a different creature with a model of its own
# (StalkerModel, FlickerModel, BruteModel, LeechModel in lurker/client), so each has a layout of its own below: the
# same texOffs and box sizes as its model. Change a model and its layout together.
#   <kind>.png       its skin: armor in plates, every face shaded for where it points (lit from above, dark beneath,
#                    dimmed along its edges), veins through it, horn for its horns and claws, teeth, and its eyes (an
#                    eyeball, a slit pupil and a lid each)
#   <kind>_eyes.png  what glows, drawn over the skin (LurkerRenderer's glow layer, which pulses): the eyeballs, red
#                    with a hotter heart; every vein; and whatever embers that kind carries
# The colors are the author's sunset palette (LurkerPalette.java has the same five): the Stalker night-dark, the
# Flicker the navy of a sky gone dark, the Brute a dried maroon and veined all over, the Leech pallid with a
# blood-dark belly.
# A model's sheet is 64 or 128 units square; the textures are drawn at twice that, so every model pixel is 2x2 texels.
# Coordinates below are the model's unless they say "texel".
# Run from the repo root: powershell -File tools/gen_lurker_textures.ps1
Add-Type -AssemblyName System.Drawing

$scale = 2
$clear = [System.Drawing.Color]::FromArgb(0, 0, 0, 0)

function Clamp($v) { [Math]::Max(0, [Math]::Min(255, [int]$v)) }
function Rgb($r, $g, $b) { [System.Drawing.Color]::FromArgb(255, (Clamp $r), (Clamp $g), (Clamp $b)) }
function Shade($c, $f) { Rgb ($c[0] * $f) ($c[1] * $f) ($c[2] * $f) }
function Tone($c, $f) { @(($c[0] * $f), ($c[1] * $f), ($c[2] * $f)) }

# The palette.
$abyss = @(24, 24, 26)
$navy = @(31, 45, 74)
$maroon = @(90, 36, 51)
$crimson = @(138, 10, 36)
$red = @(255, 45, 70)
# Teeth are the one pale thing on any of them: bone, with the sunset on it.
$toothTone = @(226, 204, 208)
$tooth = Rgb 226 204 208
$toothShade = Rgb 168 146 152
$gum = Shade $crimson 0.8
$pit = Rgb 2 2 4

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
                Dot ($x0 + $tx) ($y0 + $ty) (Rgb ($base[0] * $f + $n) ($base[1] * $f + $n) ($base[2] * $f + $n))
            }
        }
    }
}

# Thin wandering lines down an area, in model coordinates: veins, and every one of them glows. On the skin a vein
# is crimson; on the glowing layer it is red along its whole length, hotter at a knot now and then, with a dim red
# spilling onto the hide on either side of it so the light looks like it comes from under the skin.
function Veins($u, $v, $w, $h, $count) {
    $count = [Math]::Max(1, [int][Math]::Round($count * $kind.veins))
    $veinGlow = Rgb 160 14 22
    $knotGlow = Rgb 214 52 44
    $spill = Rgb 28 1 4
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
            Dot $tx $ty $(if ($knot) { Shade $red 0.6 } else { Shade $crimson 0.75 }) $(if ($knot) { $knotGlow } else { $veinGlow })
            # Mostly straight down, with a step to one side now and then.
            if ($random.Next(0, 3) -eq 0) { $x += $random.Next(-1, 2) }
            $y += 1
            if ($x -lt $u * $scale -or $x -ge ($u + $w) * $scale) { break }
        }
    }
}

# An ember: a lit point with a dimmer cross round it, at a texel.
function Ember($cx, $cy) {
    Dot $cx $cy (Shade $crimson 1.0) (Shade $red 0.95)
    foreach ($o in @(@(-1, 0), @(1, 0), @(0, -1), @(0, 1))) { Dot ($cx + $o[0]) ($cy + $o[1]) (Shade $crimson 0.7) (Shade $crimson 0.75) }
}

# A socket on a face: a pit 4 texels square with a dark hollow round it, so the eye that sits in it (a part of the
# model) sinks into the skull. ($fx, $fy) is the face's corner in texels, ($ex, $ey) the pit's corner on the face.
function Socket($fx, $fy, $ex, $ey) {
    $hollow = Shade $navy 0.45
    for ($y = 0; $y -lt 4; $y++) { for ($x = 0; $x -lt 4; $x++) { Dot ($fx + $ex + $x) ($fy + $ey + $y) $pit } }
    for ($x = -1; $x -le 4; $x++) { Dot ($fx + $ex + $x) ($fy + $ey - 1) $hollow; Dot ($fx + $ex + $x) ($fy + $ey + 4) $hollow }
    for ($y = 0; $y -le 3; $y++) { Dot ($fx + $ex - 1) ($fy + $ey + $y) $hollow; Dot ($fx + $ex + 4) ($fy + $ey + $y) $hollow }
}

# A row of teeth along a row of texels: every other texel a tooth, with a gum on the side given (-1 above, 1 below).
function Teeth($x0, $y, $count, $gumSide) {
    for ($i = 0; $i -lt $count; $i++) {
        $x = $x0 + $i * 2
        Dot $x $y $tooth
        Dot $x ($y + $gumSide) $gum
    }
}

# The inside of a mouth: an area of texels, crimson, darker towards the throat (down the area), a tongue down its middle.
function Mouth($x0, $y0, $w, $h) {
    for ($z = 0; $z -lt $h; $z++) { for ($x = 0; $x -lt $w; $x++) {
        $tongue = ([Math]::Abs($x - ($w - 1) / 2.0) -lt $w * 0.2 -and $z -lt $h * 0.75)
        Dot ($x0 + $x) ($y0 + $z) (Shade $crimson ((0.8 - $z * 0.6 / $h + $random.NextDouble() * 0.08) * $(if ($tongue) { 1.3 } else { 0.8 })))
    } }
}

# The eyes are parts of every model (LurkerModel.eyes), three boxes whose skin starts at (u, v):
# the eyeball at (u, v), 2 by 2 by 1: deep red on the skin, so it is red even in daylight, and on the glowing layer
# red all over with a hotter heart on its front face (the glow is added to the skin under it, so both are kept short
# of blue: together they must still come out red); the pupil at (u + 8, v): a slit of nothing, not on the glowing
# layer, so it stays black against the eye's light; and the lid at (u + 14, v): hide, with a darker lip along its foot.
function EyeParts($u, $v) {
    $eyeHot = Rgb 255 96 70
    $eyeRed = Rgb 255 26 30
    $eyeDeep = Rgb 112 0 8
    for ($y = $v * $scale; $y -lt ($v + 3) * $scale; $y++) { for ($x = $u * $scale; $x -lt ($u + 6) * $scale; $x++) { Dot $x $y $eyeDeep (Rgb 200 16 22) } }
    for ($y = 0; $y -lt 4; $y++) { for ($x = 0; $x -lt 4; $x++) {
        $heart = ($x -ge 1 -and $x -le 2 -and $y -ge 1 -and $y -le 2)
        Dot (($u + 1) * $scale + $x) (($v + 1) * $scale + $y) $(if ($heart) { Rgb 150 4 12 } else { $eyeDeep }) $(if ($heart) { $eyeHot } else { $eyeRed })
    } }
    for ($y = $v * $scale; $y -lt ($v + 3) * $scale; $y++) { for ($x = ($u + 8) * $scale; $x -lt ($u + 12) * $scale; $x++) { Dot $x $y (Rgb 6 3 5) } }
    for ($y = $v * $scale; $y -lt ($v + 4) * $scale; $y++) { for ($x = ($u + 14) * $scale; $x -lt ($u + 21) * $scale; $x++) {
        $n = $random.Next(-2, 3)
        $lip = if ($y -ge ($v + 3) * $scale - 1) { 0.55 } else { 0.85 }
        Dot $x $y (Rgb (($hide[0] * 0.8 + $n) * $lip) (($hide[1] * 0.8 + $n) * $lip) (($hide[2] * 0.8 + $n) * $lip))
    } }
}

# ---- The Stalker: four spider's legs, a ribbed chest under armor, a bladed tail, a horned head.
function Paint-Stalker {
    Box 0 0 7 5 6 $hide 3                           # head
    Box 0 11 7 2 6 (Tone $hide 0.83) 3              # jaw
    Box 0 19 8 5 8 $hide 3 "plates" 4               # chest
    Box 0 32 6 4 7 $kind.belly 3 "rings" 4          # belly: segments
    Box 28 32 2 2 5 (Tone $hide 0.84) 2 "rings" 5   # tail lengths
    Box 32 0 2 7 2 (Tone $hide 0.8) 2 "plates" 6    # upper limbs
    Box 40 0 2 15 2 (Tone $hide 0.8) 2 "plates" 6   # lower limbs
    Box 48 0 2 3 2 $bone 5                          # the ridge down its back, and the spikes on its tail
    Box 56 0 1 4 1 @(124, 12, 34) 6                 # ribs, crimson
    Box 56 6 1 3 3 $bone 5 "rings" 3                # claws and knee spurs
    Box 34 20 1 5 4 $bone 5 "rings" 3               # horns and the tail's blade
    Box 0 52 9 1 7 $kind.armor 3 "rings" 4 $true    # the armor over its shoulders
    Box 34 44 7 1 2 (Tone $hide 0.62) 2             # its brow
    Box 34 48 1 2 1 @(186, 168, 174) 6              # fangs: bone, duller than the teeth behind them
    Box 40 50 3 3 3 (Tone $hide 0.7) 3              # knee joints

    # Enough veins to map its body, few enough that the dark hide between them still reads: a limb takes two.
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
    for ($z = 0; $z -lt 7; $z++) {
        $ty = (52 + $z) * $scale
        if ($ty % 4 -eq 2) { Ember (11 * $scale + 1) $ty }
    }
    # Cracks in the knee joints, lit from inside: one on each side face (they start at (40, 53), each 3 by 3).
    for ($side = 0; $side -lt 4; $side++) { Ember ((40 + $side * 3) * $scale + 3) (53 * $scale + 3) }
    # Chest sides: the right one at (0, 27), the left one at (16, 27), each 8 wide and 5 high. The shadow each rib casts.
    foreach ($sx in @(0, 16)) { for ($r = 0; $r -lt 3; $r++) { for ($y = 1; $y -lt 5 * $scale - 1; $y++) { Dot (($sx + 2 + $r * 2) * $scale + 1) (27 * $scale + $y) (Rgb 8 8 10) } } }

    # The head's front face is at (6, 6), 7 wide and 5 high.
    $fx = 6 * $scale; $fy = 6 * $scale
    Socket $fx $fy 2 2; Socket $fx $fy 8 2
    # A gaunt cheek under each eye, and two slits for a nose between them.
    foreach ($ex in @(2, 8)) { for ($x = 0; $x -le 3; $x++) { Dot ($fx + $ex + $x) ($fy + 7) (Rgb 10 10 12) } }
    foreach ($x in @(6, 7)) { Dot ($fx + $x) ($fy + 5) $pit; Dot ($fx + $x) ($fy + 6) $pit }
    Teeth ($fx + 1) ($fy + 9) 6 -1
    # Lower teeth along the top of the jaw's front (at (6, 17)), and the inside of its mouth on the jaw's top (at (6, 11)).
    Teeth ($fx + 2) (17 * $scale) 6 1
    Mouth $fx (11 * $scale) (7 * $scale) (6 * $scale)
    EyeParts 0 46
}

# ---- The Flicker: a legless wraith. A ribbed torso, a trailing spine, a long skull with tall horns, long arms.
function Paint-Flicker {
    Box 0 0 6 8 4 $hide 3 "plates" 4                # torso: its ribcage, in bands
    Box 0 12 2 4 2 (Tone $hide 0.9) 2 "plates" 4    # spine lengths
    Box 8 12 1 4 1 (Tone $hide 0.75) 2 "plates" 3   # the thin end of the spine
    Box 20 0 5 6 6 $hide 3                          # skull
    Box 44 0 1 5 1 $bone 5 "plates" 3               # horns
    Box 0 20 4 2 5 (Tone $hide 0.83) 3              # jaw
    Box 28 20 2 9 2 (Tone $hide 0.8) 2 "plates" 6   # upper arms
    Box 36 20 2 9 2 (Tone $hide 0.8) 2 "plates" 6   # forearms
    Box 44 20 1 5 1 $bone 5 "plates" 3              # claws
    Box 48 20 1 1 5 @(124, 12, 34) 6                # ribs, crimson
    Box 48 12 1 4 2 $bone 5                         # shoulder blades and vertebrae

    Veins 0 4 20 8 6      # torso, all round
    Veins 20 6 6 6 2      # skull: one side,
    Veins 31 6 11 6 3     # the other side and the back
    Veins 28 22 8 9 2     # upper arms
    Veins 36 22 8 9 2     # forearms
    Veins 0 14 8 4 2      # spine

    # A heart: an ember in the middle of its chest (the torso's front face is at (4, 4), 6 wide and 8 high).
    Ember (7 * $scale) (7 * $scale); Ember (7 * $scale - 1) (7 * $scale + 2)
    # The skull's front face is at (26, 6), 5 wide and 6 high: sockets high on it, a long nose slit, teeth at its foot.
    $fx = 26 * $scale; $fy = 6 * $scale
    Socket $fx $fy 1 3; Socket $fx $fy 5 3
    for ($y = 8; $y -le 9; $y++) { Dot ($fx + 4) ($fy + $y) $pit; Dot ($fx + 5) ($fy + $y) $pit }
    Teeth ($fx + 1) ($fy + 11) 4 -1
    # The jaw's front face is at (5, 25), 4 wide; the inside of the mouth is its top, at (5, 20), 4 wide and 5 deep.
    Teeth (5 * $scale + 1) (25 * $scale) 3 1
    Mouth (5 * $scale) (20 * $scale) (4 * $scale) (5 * $scale)
    EyeParts 0 46
}

# ---- The Brute: a hulk on its knuckles. A barrel torso plated down the back, great arms and fists, short legs.
function Paint-Brute {
    Box 0 0 14 12 10 $hide 4 "plates" 6             # torso
    Box 48 0 10 5 8 $kind.belly 3 "plates" 4        # hips and belly
    Box 84 0 10 2 8 $kind.armor 3 "rings" 4 $true   # the plates down its back
    Box 84 12 2 4 2 $bone 5 "plates" 3              # the spikes through them
    Box 0 24 7 6 6 $hide 3                          # head
    Box 26 24 7 2 6 (Tone $hide 0.83) 3             # jaw
    Box 52 24 1 3 1 @(196, 178, 184) 6              # tusks
    Box 56 24 2 2 4 $bone 5 "rings" 3               # horns
    Box 72 24 7 1 2 (Tone $hide 0.62) 2             # brow
    Box 0 40 5 8 5 (Tone $hide 0.9) 3 "plates" 6    # upper arms
    Box 20 40 6 9 6 (Tone $hide 0.85) 3 "plates" 6  # forearms
    Box 44 40 7 4 7 (Tone $hide 0.7) 3              # fists
    Box 72 40 5 7 5 (Tone $hide 0.85) 3 "plates" 5  # legs
    Box 92 40 6 2 7 (Tone $hide 0.7) 3              # feet
    Box 0 70 6 3 6 $kind.armor 3 "rings" 4 $true    # shoulder plates

    Veins 0 10 48 12 10   # torso, all round
    Veins 10 0 14 10 3    # the top of its torso, between the plates
    Veins 48 8 36 5 5     # belly
    Veins 0 30 6 6 2      # head: one side,
    Veins 13 30 13 6 3    # the other side and the back
    Veins 0 45 20 8 4     # upper arms
    Veins 20 46 24 9 5    # forearms
    Veins 72 45 20 7 3    # legs
    Veins 44 47 28 4 3    # fists: its knuckles are lit

    # Embers down the middle of its back plates (their top face is at (92, 0), 10 wide and 8 deep).
    for ($z = 1; $z -lt 8; $z += 2) { Ember (97 * $scale) ($z * $scale) }
    # A furnace in its chest: embers in a ring on the torso's front face (at (10, 10), 14 wide and 12 high).
    foreach ($o in @(@(0, -3), @(3, 0), @(0, 3), @(-3, 0), @(0, 0))) { Ember (17 * $scale + $o[0]) (15 * $scale + $o[1]) }
    # The head's front face is at (6, 30), 7 wide and 6 high: small eyes set wide, a broad nose, teeth at its foot.
    $fx = 6 * $scale; $fy = 30 * $scale
    Socket $fx $fy 2 3; Socket $fx $fy 8 3
    foreach ($x in @(5, 6, 7, 8)) { Dot ($fx + $x) ($fy + 8) $pit }
    Teeth ($fx + 1) ($fy + 11) 6 -1
    # The jaw's front face is at (32, 30); the inside of the mouth is its top, at (32, 24), 7 wide and 6 deep.
    Teeth (32 * $scale + 2) (30 * $scale) 6 1
    Mouth (32 * $scale) (24 * $scale) (7 * $scale) (6 * $scale)
    EyeParts 0 60
}

# ---- The Leech: a crawler. A blunt head and five segments, each smaller than the last, with little legs and fins.
function Paint-Leech {
    Box 0 0 6 5 5 $hide 3                           # head
    Box 22 0 1 1 3 $bone 5                          # mandibles
    Box 32 0 1 2 1 $bone 5                          # the fins on its back
    Box 36 0 1 4 1 (Tone $hide 0.7) 2 "plates" 4    # legs
    Box 0 10 7 6 5 $hide 3 "rings" 5                # the segments, largest first: each a ring of hide
    Box 0 21 6 5 5 (Tone $hide 0.95) 3 "rings" 5
    Box 0 31 5 4 5 (Tone $hide 0.9) 3 "rings" 5
    Box 24 10 4 3 5 (Tone $hide 0.85) 3 "rings" 5
    Box 24 18 2 2 5 (Tone $hide 0.8) 2 "rings" 5
    Box 24 25 1 1 3 $bone 5                         # stinger

    # Its underside is the blood it has drunk: the bottom face of each segment, and a band along the foot of its sides.
    foreach ($seg in @(@(0, 10, 7, 6), @(0, 21, 6, 5), @(0, 31, 5, 4), @(24, 10, 4, 3))) {
        $u = $seg[0]; $v = $seg[1]; $w = $seg[2]; $h = $seg[3]
        for ($y = $v * $scale; $y -lt ($v + 5) * $scale; $y++) { for ($x = ($u + 5 + $w) * $scale; $x -lt ($u + 5 + 2 * $w) * $scale; $x++) {
            Dot $x $y (Shade $kind.belly (0.7 + $random.NextDouble() * 0.2)) (Rgb 60 4 10)
        } }
        for ($x = $u * $scale; $x -lt ($u + 10 + 2 * $w) * $scale; $x++) {
            Dot $x (($v + 5 + $h) * $scale - 1) (Shade $kind.belly 0.8) (Rgb 70 5 12)
            Dot $x (($v + 5 + $h) * $scale - 2) (Shade $kind.belly 0.6)
        }
    }
    Veins 0 15 24 5 5     # first segment
    Veins 0 26 22 4 4     # second
    Veins 0 36 20 3 3     # third
    Veins 24 15 18 2 2    # fourth
    Veins 0 5 5 5 1       # head: one side,
    Veins 11 5 11 5 2     # the other side and the back

    # The head's front face is at (5, 5), 6 wide and 5 high: eyes high on it and, below them, a round mouth: a pit
    # ringed with teeth.
    $fx = 5 * $scale; $fy = 5 * $scale
    Socket $fx $fy 1 1; Socket $fx $fy 7 1
    for ($y = 6; $y -le 9; $y++) { for ($x = 4; $x -le 7; $x++) {
        $rim = ($x -eq 4 -or $x -eq 7 -or $y -eq 6 -or $y -eq 9)
        Dot ($fx + $x) ($fy + $y) $(if ($rim) { $tooth } else { Shade $crimson 0.5 })
    } }
    foreach ($c in @(@(4, 6), @(7, 6), @(4, 9), @(7, 9))) { Dot ($fx + $c[0]) ($fy + $c[1]) $gum }
    EyeParts 0 46
}

# Each kind: its sheet's size in model units, its hide, its belly, its armor, its horn, how thickly it is veined.
$kinds = @(
    @{ name = "stalker"; units = 64; hide = @(29, 29, 33); belly = @(26, 30, 44); armor = @(34, 36, 46); horn = @(58, 78, 118); veins = 1.0 },
    @{ name = "flicker"; units = 64; hide = @(22, 30, 54); belly = @(16, 22, 40); armor = @(28, 40, 70); horn = @(96, 118, 160); veins = 0.8 },
    @{ name = "brute"; units = 128; hide = @(48, 25, 31); belly = @(36, 19, 25); armor = @(66, 29, 37); horn = @(44, 48, 62); veins = 1.3 },
    @{ name = "leech"; units = 64; hide = @(60, 50, 56); belly = @(120, 18, 36); armor = @(70, 58, 64); horn = @(122, 106, 114); veins = 1.0 }
)
$dir = Join-Path (Get-Location) "common\src\main\resources\assets\whycantisleep\textures\entity\lurker"
New-Item -ItemType Directory -Force $dir | Out-Null
foreach ($kind in $kinds) {
    $size = $kind.units * $scale
    $hide = $kind.hide
    $bone = $kind.horn
    $random = New-Object System.Random 7331
    $skin = New-Object System.Drawing.Bitmap $size, $size
    $eyes = New-Object System.Drawing.Bitmap $size, $size
    for ($y = 0; $y -lt $size; $y++) { for ($x = 0; $x -lt $size; $x++) { $skin.SetPixel($x, $y, $clear); $eyes.SetPixel($x, $y, $clear) } }
    & "Paint-$($kind.name)"
    $skin.Save((Join-Path $dir "$($kind.name).png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $eyes.Save((Join-Path $dir "$($kind.name)_eyes.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $skin.Dispose(); $eyes.Dispose()
}
"Wrote $dir"
