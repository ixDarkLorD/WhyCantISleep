# Draws the Dark Between's textures (the palette is LurkerPalette.java's):
#   textures/item/dream_lens.png                  the Dream Lens: a lens of rose glass in a dark rim, a slit of red
#                                                  in it like the Lurkers' eyes, on a short handle
# (The Window in the sky has no texture of its own: its frame is drawn by shaders/sky/dream_window.fsh.)
# Run from the repo root: powershell -File tools/gen_between_textures.ps1
Add-Type -AssemblyName System.Drawing

$assets = Join-Path (Get-Location) "common\src\main\resources\assets\whycantisleep\textures"
New-Item -ItemType Directory -Force (Join-Path $assets "item") | Out-Null
$random = New-Object System.Random 4242
$clear = [System.Drawing.Color]::FromArgb(0, 0, 0, 0)
function Clamp($v) { [Math]::Max(0, [Math]::Min(255, [int]$v)) }
function Rgb($r, $g, $b, $a = 255) { [System.Drawing.Color]::FromArgb((Clamp $a), (Clamp $r), (Clamp $g), (Clamp $b)) }

# ---- The Dream Lens, 16 by 16.
$b = New-Object System.Drawing.Bitmap 16, 16
for ($y = 0; $y -lt 16; $y++) { for ($x = 0; $x -lt 16; $x++) { $b.SetPixel($x, $y, $clear) } }
$cx = 6.5; $cy = 6.5
for ($y = 0; $y -lt 16; $y++) { for ($x = 0; $x -lt 16; $x++) {
    $d = [Math]::Sqrt(($x - $cx) * ($x - $cx) + ($y - $cy) * ($y - $cy))
    $lit = ($x + $y) -lt 13
    if ($d -le 6.4 -and $d -gt 5.3) { $b.SetPixel($x, $y, $(if ($lit) { Rgb 70 84 120 } else { Rgb 24 30 46 })) }
    elseif ($d -le 5.3 -and $d -gt 4.4) { $b.SetPixel($x, $y, $(if ($lit) { Rgb 120 56 90 } else { Rgb 58 28 46 })) }
    elseif ($d -le 4.4) {
        # The glass: rose at its top left fading to deep violet, a red slit down its middle.
        $t = (($x - $cx) + ($y - $cy)) / 9.0 + 0.5
        $color = Rgb (220 - 150 * $t) (150 - 120 * $t) (176 - 100 * $t)
        if ([Math]::Abs($x - $cx) -lt 0.8 -and [Math]::Abs($y - $cy) -lt 3.0) { $color = Rgb 255 40 52 }
        if ([Math]::Abs($x - $cx) -lt 0.8 -and [Math]::Abs($y - $cy) -lt 1.0) { $color = Rgb 255 150 130 }
        $b.SetPixel($x, $y, $color)
    }
} }
# A glint, and the handle off its lower right.
$b.SetPixel(4, 4, (Rgb 255 236 240)); $b.SetPixel(5, 4, (Rgb 250 200 214)); $b.SetPixel(4, 5, (Rgb 250 200 214))
foreach ($i in 0..3) { $b.SetPixel(11 + $i, 11 + $i, (Rgb 70 84 120)); if (12 + $i -lt 16) { $b.SetPixel(12 + $i, 11 + $i, (Rgb 24 30 46)) } }
$b.SetPixel(15, 15, (Rgb 138 10 36))
$b.Save((Join-Path $assets "item\dream_lens.png"), [System.Drawing.Imaging.ImageFormat]::Png); $b.Dispose()

"Wrote $assets"
