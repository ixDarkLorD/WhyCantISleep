# Draws the sleep HUD's panels and bar as GUI sprites, under assets/whycantisleep/textures/gui/sprites/hud/, each with
# the .mcmeta that tells the game how to stretch it (nine-slice), so HudStyle draws them at any size. The look is flat
# and dark (the author's request, 2026-10-21): no bevels, no shadows, a hairline border and softly rounded corners.
#   wash.png          the wash of color over a notice's left side, fading to the right
#   wash_fade.png     the wash's foot, fading out downwards as well
#   wash_fade_top.png its head, fading in from the top the same way
#   bar_back.png      the empty track of the sleep board's bar
#   sleep_fill.png    what fills it: green
# The frames (card.png, notice.png) are tools/gen_frames.py's now; the track and fill just stretch. (The icon squares, dividers and dots are
# small flat fills in HudStyle, tinted per message.)
# Run from the repo root: powershell -File tools/gen_hud_textures.ps1
Add-Type -AssemblyName System.Drawing

$dir = Join-Path (Get-Location) "common\src\main\resources\assets\whycantisleep\textures\gui\sprites\hud"
New-Item -ItemType Directory -Force $dir | Out-Null
$utf8 = New-Object Text.UTF8Encoding $false
function Argb($a, $rgb) { [System.Drawing.Color]::FromArgb($a, ($rgb -shr 16) -band 255, ($rgb -shr 8) -band 255, $rgb -band 255) }
function Save($bitmap, $name, $width, $height, $border) {
    $bitmap.Save((Join-Path $dir "$name.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $bitmap.Dispose()
    $path = Join-Path $dir "$name.png.mcmeta"
    # Without a border the sprite simply stretches, which is the game's default and needs no .mcmeta.
    if ($border -le 0) {
        if (Test-Path $path) { Remove-Item $path }
        return
    }
    $meta = "{`n  `"gui`": {`n    `"scaling`": {`n      `"type`": `"nine_slice`",`n      `"width`": $width,`n      `"height`": $height,`n      `"border`": $border`n    }`n  }`n}`n"
    [IO.File]::WriteAllText($path, $meta, $utf8)
}

# The notice's wash: white, fading in over its first few pixels from the left edge and then out to nothing at its
# right, stretched over the card's left part and tinted the message's color by HudStyle.
function WashAcross($x) {
    $in = [Math]::Min(1.0, ($x + 0.5) / 6.0)
    return $in * $in * (3 - 2 * $in) * [Math]::Pow(1 - $x / 63.0, 1.6)
}
$wash = New-Object System.Drawing.Bitmap 64, 1
for ($x = 0; $x -lt 64; $x++) {
    $alpha = [int][Math]::Round(255 * (WashAcross $x))
    $wash.SetPixel($x, 0, (Argb $alpha 0xFFFFFF))
}
Save $wash "wash" 64 1 0

# Its foot: the same fade across, fading out downwards too, stretched across under the wash so it ends softly; and
# its head, the foot upside down, so it begins as softly.
$washFade = New-Object System.Drawing.Bitmap 64, 8
$washFadeTop = New-Object System.Drawing.Bitmap 64, 8
for ($y = 0; $y -lt 8; $y++) { for ($x = 0; $x -lt 64; $x++) {
    $alpha = [int][Math]::Round(255 * (WashAcross $x) * [Math]::Pow(1 - ($y + 0.5) / 8.0, 1.2))
    $washFade.SetPixel($x, $y, (Argb $alpha 0xFFFFFF))
    $washFadeTop.SetPixel($x, 7 - $y, (Argb $alpha 0xFFFFFF))
} }
Save $washFade "wash_fade" 64 8 0
Save $washFadeTop "wash_fade_top" 64 8 0

# The bar: a flat track and a flat fill, both stretched to any length.
function Flat($name, $rgb) {
    $b = New-Object System.Drawing.Bitmap 4, 3
    for ($y = 0; $y -lt 3; $y++) { for ($x = 0; $x -lt 4; $x++) { $b.SetPixel($x, $y, (Argb 255 $rgb)) } }
    Save $b $name 4 3 0
}
Flat "bar_back" 0x26292F
Flat "sleep_fill" 0x4ADE80
"Wrote $dir"
