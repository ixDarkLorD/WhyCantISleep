# Draws the Lurker's two 64x64 textures under assets/whycantisleep/textures/entity/lurker/:
#   lurker.png       its skin: near-black with a faint grain, a mouth of pale teeth, hollow sockets, a pale ridge
#   lurker_eyes.png  only its eyes, drawn glowing over the skin (LurkerRenderer's eyes layer)
# The parts' places on the sheet are LurkerModel's (texOffs and box sizes); change both together.
# Run from the repo root: powershell -File tools/gen_lurker_textures.ps1
Add-Type -AssemblyName System.Drawing

$size = 64
$random = New-Object System.Random 7331
$skin = New-Object System.Drawing.Bitmap $size, $size
$eyes = New-Object System.Drawing.Bitmap $size, $size
$clear = [System.Drawing.Color]::FromArgb(0, 0, 0, 0)
for ($y = 0; $y -lt $size; $y++) { for ($x = 0; $x -lt $size; $x++) { $skin.SetPixel($x, $y, $clear); $eyes.SetPixel($x, $y, $clear) } }

# A box's unwrapped area on the sheet: width 2 * (depth + width), height depth + height, from its texOffs.
function Fill-Box($u, $v, $w, $h, $d, $base, $grain) {
    for ($y = $v; $y -lt $v + $d + $h; $y++) {
        for ($x = $u; $x -lt $u + 2 * ($d + $w); $x++) {
            $n = $random.Next(-$grain, $grain + 1)
            # Darker towards the bottom of each box, so the underside falls away.
            $shade = [int](($y - $v) * -0.35)
            $r = [Math]::Max(0, [Math]::Min(255, $base[0] + $n + $shade))
            $g = [Math]::Max(0, [Math]::Min(255, $base[1] + $n + $shade))
            $b = [Math]::Max(0, [Math]::Min(255, $base[2] + $n + $shade + 2))
            $skin.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, $r, $g, $b))
        }
    }
}

$hide = @(20, 18, 28)
Fill-Box 0 0 7 7 6 $hide 5       # head
Fill-Box 0 14 8 5 14 $hide 5     # body
Fill-Box 0 34 2 11 2 @(14, 13, 20) 4   # limbs
Fill-Box 10 34 2 2 2 @(70, 66, 80) 8   # the ridge down its back, pale like bone

# The head's front face starts at (depth, depth) = (6, 6) and is 7 by 7.
$fx = 6; $fy = 6
$socket = [System.Drawing.Color]::FromArgb(255, 2, 2, 4)
foreach ($p in @(@(1, 2), @(2, 2), @(4, 2), @(5, 2), @(1, 3), @(2, 3), @(4, 3), @(5, 3))) { $skin.SetPixel($fx + $p[0], $fy + $p[1], $socket) }
# A mouth across the face: a dark gap with uneven teeth.
for ($x = 1; $x -le 5; $x++) {
    $skin.SetPixel($fx + $x, $fy + 5, $socket)
    if ($x % 2 -eq 1) { $skin.SetPixel($fx + $x, $fy + 5, [System.Drawing.Color]::FromArgb(255, 150, 144, 132)) }
}

# The eyes: a bright core beside a dimmer pixel in each socket.
$bright = [System.Drawing.Color]::FromArgb(255, 255, 214, 190)
$glow = [System.Drawing.Color]::FromArgb(255, 255, 60, 36)
$eyes.SetPixel($fx + 1, $fy + 2, $glow); $eyes.SetPixel($fx + 2, $fy + 2, $bright)
$eyes.SetPixel($fx + 4, $fy + 2, $bright); $eyes.SetPixel($fx + 5, $fy + 2, $glow)

$dir = Join-Path (Get-Location) "common\src\main\resources\assets\whycantisleep\textures\entity\lurker"
New-Item -ItemType Directory -Force $dir | Out-Null
$skin.Save((Join-Path $dir "lurker.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$eyes.Save((Join-Path $dir "lurker_eyes.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$skin.Dispose(); $eyes.Dispose()
"Wrote $dir"
