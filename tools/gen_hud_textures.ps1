# Draws the sleep HUD's own panels and bars as GUI sprites, under assets/whycantisleep/textures/gui/sprites/hud/,
# each with the .mcmeta that tells the game how to stretch it (nine-slice), so HudStyle draws them at any size:
#   card.png          the message card and the sleep board: slate, like a slab of night
#   mood_medal.png, mood_track.png          the mood gauge: a gold coin and a riveted track
#   lucidity_medal.png, lucidity_track.png  the lucidity gauge: a rose diamond and a notched, pointed track
#   bar_back.png      the empty track of a status bar
#   sleep_fill.png    what fills it on the sleep board: green
#   mood_fill.png, lucidity_fill.png   what fills the gauges, taller: gold, and blush down to violet
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

# The two status gauges are shaped nothing like the card: each is a medal holding its icon, with a track running out
# of it for its bar. Mood's is a round gold coin on a riveted, round-ended track; lucidity's is a rose-edged diamond
# on a notched track that ends in a point. They are drawn at their own size, never stretched (MindStatus has the sizes).
function Medal($name, $outline, $light, $shade, $ring, $fill, $stud, [bool]$diamond) {
    $b = New-Object System.Drawing.Bitmap 19, 19
    for ($y = 0; $y -lt 19; $y++) {
        for ($x = 0; $x -lt 19; $x++) {
            # How far out from the middle the pixel is: around a circle, or along a diamond's edges.
            $d = if ($diamond) { [Math]::Abs($x - 9) + [Math]::Abs($y - 9) } else { [Math]::Sqrt(($x - 9) * ($x - 9) + ($y - 9) * ($y - 9)) }
            $lit = ($x + $y) -lt 18
            # From the outside in: outline, a broad rim lit from the top left, a dark channel, a thin inner rim lit the
            # other way (so the middle reads as sunk), and the dark field the icon sits on.
            $color = if ($d -gt 9.3) { Argb 0 0 }
                elseif ($d -gt 8.3) { Argb 255 $outline }
                elseif ($d -gt 6.6) { if ($lit) { Argb 255 $light } else { Argb 255 $shade } }
                elseif ($d -gt 5.8) { Argb 255 $ring }
                elseif ($d -gt 5.0) { if ($lit) { Argb 255 $shade } else { Argb 255 $light } }
                else { Argb 255 $fill }
            $b.SetPixel($x, $y, $color)
        }
    }
    # Four studs on the rim, at its top, bottom and sides: rivets on the coin, gems at the diamond's points.
    foreach ($p in @(@(9, 1), @(9, 17), @(1, 9), @(17, 9))) { $b.SetPixel($p[0], $p[1], (Argb 255 $stud)) }
    if (-not $diamond) { foreach ($p in @(@(9, 2), @(9, 16), @(2, 9), @(16, 9))) { $b.SetPixel($p[0], $p[1], (Argb 255 $stud)) } }
    Save $b $name 19 19 0
}
function Track($name, $outline, $light, $shade, $edge, $groove, $mark, [int]$every, [bool]$pointed) {
    $b = New-Object System.Drawing.Bitmap 68, 11
    for ($y = 0; $y -lt 11; $y++) {
        $off = [Math]::Abs($y - 5)
        # Where the row ends on the right: a point, or a rounded end.
        $last = if ($pointed) { 67 - [int]($off * 1.4) } else { if ($off -eq 5) { 64 } elseif ($off -eq 4) { 66 } else { 67 } }
        for ($x = 0; $x -lt 68; $x++) {
            $color = if ($x -gt $last) { Argb 0 0 }
                elseif ($y -eq 0 -or $y -eq 10 -or $x -eq $last) { Argb 255 $outline }
                elseif ($y -eq 1 -or $y -eq 9 -or $x -eq $last - 1) {
                    # Along the rim, a mark at even steps: a rivet on mood's, a notch on lucidity's.
                    if ($x % $every -eq 4) { Argb 255 $mark } elseif ($y -eq 1) { Argb 255 $light } else { Argb 255 $shade }
                }
                elseif ($y -eq 2 -or $y -eq 8 -or $x -ge $last - 3) { Argb 255 $edge }
                # The groove is a touch lighter along its floor, so an empty gauge still has depth.
                elseif ($y -eq 7) { Argb 255 $edge }
                else { Argb 255 $groove }
            $b.SetPixel($x, $y, $color)
        }
    }
    Save $b $name 68 11 0
}
Medal "mood_medal" 0x1E1408 0xFFE08A 0xB07A22 0x4A3210 0x2A1C0C 0xFFF6D0 $false
Medal "lucidity_medal" 0x14181C 0xE9B6B8 0x8A4E7C 0x3A2440 0x18101E 0xFFE2E8 $true
Track "mood_track" 0x1E1408 0xE8BC55 0x8C5A1C 0x241A0C 0x15100A 0xFFF2C0 9 $false
Track "lucidity_track" 0x14181C 0xCB8CAA 0x663A63 0x1E1626 0x100C16 0x14181C 7 $true

# What fills a gauge: five rows of color from a bright top edge down to a deep foot, 7 tall with a clear row above and
# below so it sits inside the track's groove.
function GaugeFill($name, $rows) {
    $b = New-Object System.Drawing.Bitmap 8, 7
    for ($y = 0; $y -lt 7; $y++) { for ($x = 0; $x -lt 8; $x++) {
        $b.SetPixel($x, $y, $(if ($y -eq 0 -or $y -eq 6) { Argb 0 0 } else { Argb 255 $rows[$y - 1] }))
    } }
    Save $b $name 8 7 0
}
GaugeFill "mood_fill" @(0xFFF3B0, 0xFFD95A, 0xFFC02E, 0xE09A1E, 0xA86C14)
GaugeFill "lucidity_fill" @(0xFFE0E4, 0xE9B6B8, 0xCB8CAA, 0xA86488, 0x744068)
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
Fill "sleep_fill" 0xA6F08C 0x55D455 0x2E8E36
"Wrote $dir"
