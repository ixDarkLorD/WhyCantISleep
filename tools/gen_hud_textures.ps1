# Draws the sleep HUD's panels and bar as GUI sprites, under assets/whycantisleep/textures/gui/sprites/hud/, each with
# the .mcmeta that tells the game how to stretch it (nine-slice), so HudStyle draws them at any size. The look is flat
# and dark (the author's request, 2026-10-21): no bevels, no shadows, a hairline border and softly rounded corners.
#   card.png          a night's sleep card and the status popup: charcoal, a little see-through, with a hairline border
#   notice.png        the other message cards, the sleep board and the minigame: soft as a pillow, round-cornered
#   wash.png          the wash of color over a notice's left side, fading to the right
#   wash_fade.png     the wash's foot, fading out downwards as well
#   bar_back.png      the empty track of the sleep board's bar
#   sleep_fill.png    what fills it: green
# The card is 8x8 with a 2 pixel border, the notice 10x10 with a 3 pixel one; the track and fill just stretch. (The icon squares, dividers and dots are
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

# A flat rounded rectangle: a one pixel border with its corner pixels left out, round a flat fill.
function Panel($size, $border, $borderAlpha, $fill, $fillAlpha) {
    $b = New-Object System.Drawing.Bitmap $size, $size
    $last = $size - 1
    for ($y = 0; $y -lt $size; $y++) { for ($x = 0; $x -lt $size; $x++) {
        $edgeX = $x -eq 0 -or $x -eq $last
        $edgeY = $y -eq 0 -or $y -eq $last
        $color = if ($edgeX -and $edgeY) { Argb 0 0 }
            elseif ($edgeX -or $edgeY) { Argb $borderAlpha $border }
            else { Argb $fillAlpha $fill }
        $b.SetPixel($x, $y, $color)
    } }
    return $b
}

# The card: charcoal with a touch of blue, nearly opaque so text stays readable over any world.
Save (Panel 8 0x2E3138 255 0x111317 236) "card" 8 8 2

# The notice, soft as a pillow (the author's pick, 2026-10-22): a two pixel border with round corners (a pixel cut
# from each, and the corner inside it rounded off too), a lighter line inside its top edge, a dark fill.
function Notice($size, $border, $top, $fill, $fillAlpha) {
    $b = New-Object System.Drawing.Bitmap $size, $size
    $last = $size - 1
    for ($y = 0; $y -lt $size; $y++) { for ($x = 0; $x -lt $size; $x++) {
        # How far in from the nearest edge, across and down.
        $dx = [Math]::Min($x, $last - $x)
        $dy = [Math]::Min($y, $last - $y)
        $color = if ($dx + $dy -lt 2) { Argb 0 0 }
            elseif ($dx -lt 2 -or $dy -lt 2 -or ($dx -eq 2 -and $dy -eq 2)) { Argb 255 $border }
            elseif ($y -eq 2) { Argb 255 $top }
            else { Argb $fillAlpha $fill }
        $b.SetPixel($x, $y, $color)
    } }
    return $b
}
Save (Notice 10 0x34373F 0x24272E 0x101115 236) "notice" 10 10 3

# The notice's wash: white, fading from its left edge to nothing at its right, stretched over the card's left part and
# tinted the message's color by HudStyle.
$wash = New-Object System.Drawing.Bitmap 64, 1
for ($x = 0; $x -lt 64; $x++) {
    $alpha = [int][Math]::Round(255 * [Math]::Pow(1 - $x / 63.0, 1.6))
    $wash.SetPixel($x, 0, (Argb $alpha 0xFFFFFF))
}
Save $wash "wash" 64 1 0

# Its foot: the same fade across, fading out downwards too, stretched across under the wash so it ends softly.
$washFade = New-Object System.Drawing.Bitmap 64, 8
for ($y = 0; $y -lt 8; $y++) { for ($x = 0; $x -lt 64; $x++) {
    $alpha = [int][Math]::Round(255 * [Math]::Pow(1 - $x / 63.0, 1.6) * [Math]::Pow(1 - ($y + 0.5) / 8.0, 1.2))
    $washFade.SetPixel($x, $y, (Argb $alpha 0xFFFFFF))
} }
Save $washFade "wash_fade" 64 8 0

# The bar: a flat track and a flat fill, both stretched to any length.
function Flat($name, $rgb) {
    $b = New-Object System.Drawing.Bitmap 4, 3
    for ($y = 0; $y -lt 3; $y++) { for ($x = 0; $x -lt 4; $x++) { $b.SetPixel($x, $y, (Argb 255 $rgb)) } }
    Save $b $name 4 3 0
}
Flat "bar_back" 0x26292F
Flat "sleep_fill" 0x4ADE80
"Wrote $dir"
