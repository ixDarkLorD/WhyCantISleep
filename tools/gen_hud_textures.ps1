# Draws the sleep HUD's own panels and bars as GUI sprites, under assets/whycantisleep/textures/gui/sprites/hud/,
# each with the .mcmeta that tells the game how to stretch it (nine-slice), so HudStyle draws them at any size:
#   card.png          the message card and the sleep board: slate, like a slab of night
#   mood_panel.png    the mood status: warm wood with a gold rim
#   sanity_panel.png  the sanity status: storm plum with a rose rim (the Lurker's palette)
#   bar_back.png      the empty track of a status bar
#   mood_fill.png, sanity_fill.png, sleep_fill.png   what fills it: gold, rose to violet, green
# Panels are 16x16 with a 4 pixel border; the track is 8x5 with a 1 pixel border; the fills just stretch.
# Run from the repo root: powershell -File tools/gen_hud_textures.ps1
Add-Type -AssemblyName System.Drawing

$dir = Join-Path (Get-Location) "common\src\main\resources\assets\whycantisleep\textures\gui\sprites\hud"
New-Item -ItemType Directory -Force $dir | Out-Null
$utf8 = New-Object Text.UTF8Encoding $false
function Argb($a, $rgb) { [System.Drawing.Color]::FromArgb($a, ($rgb -shr 16) -band 255, ($rgb -shr 8) -band 255, $rgb -band 255) }
function Save($bitmap, $name, $width, $height, $border) {
    $bitmap.Save((Join-Path $dir "$name.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $bitmap.Dispose()
    # Without a border the sprite simply stretches, which is the game's default and needs no .mcmeta.
    if ($border -le 0) { return }
    $meta = "{`n  `"gui`": {`n    `"scaling`": {`n      `"type`": `"nine_slice`",`n      `"width`": $width,`n      `"height`": $height,`n      `"border`": $border`n    }`n  }`n}`n"
    [IO.File]::WriteAllText((Join-Path $dir "$name.png.mcmeta"), $meta, $utf8)
}

# A panel: a dark outline with its corners cut, a rim lit from the top left, a second inner line, and a fill that
# lets a little of the world through.
function Panel($name, $outline, $light, $shade, $inner, $fill, $fillAlpha) {
    $b = New-Object System.Drawing.Bitmap 16, 16
    for ($y = 0; $y -lt 16; $y++) {
        for ($x = 0; $x -lt 16; $x++) {
            $edge = [Math]::Min([Math]::Min($x, $y), [Math]::Min(15 - $x, 15 - $y))
            $corner = (($x -eq 0 -or $x -eq 15) -and ($y -eq 0 -or $y -eq 15))
            $color = if ($corner) { Argb 0 0 }
                elseif ($edge -eq 0) { Argb 255 $outline }
                elseif ($edge -eq 1) { if ($x -eq 1 -or $y -eq 1) { Argb 255 $light } else { Argb 255 $shade } }
                elseif ($edge -eq 2) { Argb 255 $inner }
                else { Argb $fillAlpha $fill }
            $b.SetPixel($x, $y, $color)
        }
    }
    # The rim's two meeting corners take the middle tone, as the game's own bevels do.
    $b.SetPixel(14, 1, (Argb 255 $inner)); $b.SetPixel(1, 14, (Argb 255 $inner))
    Save $b $name 16 16 4
}

Panel "card" 0x0B0C10 0x6A6E84 0x30323E 0x1A1B24 0x14151C 232
Panel "mood_panel" 0x1E1408 0xE8BC55 0x8C5A1C 0x4A3216 0x2E2010 236
Panel "sanity_panel" 0x14181C 0xCB8CAA 0x663A63 0x3A2A3E 0x20172A 236

# A bar's empty track: an outline and a dark groove.
$b = New-Object System.Drawing.Bitmap 8, 5
for ($y = 0; $y -lt 5; $y++) { for ($x = 0; $x -lt 8; $x++) {
    $edge = ($x -eq 0 -or $x -eq 7 -or $y -eq 0 -or $y -eq 4)
    $b.SetPixel($x, $y, $(if ($edge) { Argb 255 0x0B0C10 } elseif ($y -eq 1) { Argb 255 0x1A1B22 } else { Argb 255 0x2A2B36 }))
} }
Save $b "bar_back" 8 5 1

# What fills a bar: three rows of color, lightest on top, inside the track's outline.
function Fill($name, $top, $middle, $bottom) {
    $b = New-Object System.Drawing.Bitmap 8, 5
    for ($y = 0; $y -lt 5; $y++) { for ($x = 0; $x -lt 8; $x++) {
        $color = if ($y -eq 0 -or $y -eq 4) { Argb 0 0 } elseif ($y -eq 1) { Argb 255 $top } elseif ($y -eq 2) { Argb 255 $middle } else { Argb 255 $bottom }
        $b.SetPixel($x, $y, $color)
    } }
    Save $b $name 8 5 0
}
Fill "mood_fill" 0xFFE98A 0xFFC83C 0xC2851C
Fill "sanity_fill" 0xE9B6B8 0xCB8CAA 0x8A4E7C
Fill "sleep_fill" 0xA6F08C 0x55D455 0x2E8E36
"Wrote $dir"
