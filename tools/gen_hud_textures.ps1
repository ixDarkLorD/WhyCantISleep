# Draws the sleep HUD's own panels and bars as GUI sprites, under assets/whycantisleep/textures/gui/sprites/hud/,
# each with the .mcmeta that tells the game how to stretch it (nine-slice), so HudStyle draws them at any size:
#   card.png          the message card and the sleep board: slate, like a slab of night
#   mood_medal.png, mood_track.png          the mood gauge: a gold coin and a round-ended track
#   lucidity_medal.png, lucidity_track.png  the lucidity gauge: a rose diamond and a pointed track
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
# of it for its bar. Mood's is a round gold coin on a round-ended track; lucidity's is a rose-edged diamond on a track
# that ends in a point. They are drawn at their own size, never stretched (MindStatus has the sizes).
# Both are built the same clean way: a shape, then its rings counted in from the edge, each ring one flat tone.

# For each pixel of a shape, how many pixels in from its edge it is (1 on the edge itself, 0 outside).
function Depth($width, $height, $inside) {
    $depth = New-Object 'int[,]' $width, $height
    for ($y = 0; $y -lt $height; $y++) { for ($x = 0; $x -lt $width; $x++) { if (& $inside $x $y) { $depth[$x, $y] = 99 } } }
    for ($ring = 1; $ring -le 12; $ring++) {
        $found = @()
        for ($y = 0; $y -lt $height; $y++) { for ($x = 0; $x -lt $width; $x++) {
            if ($depth[$x, $y] -ne 99) { continue }
            $edge = $false
            for ($dy = -1; $dy -le 1 -and -not $edge; $dy++) { for ($dx = -1; $dx -le 1; $dx++) {
                $nx = $x + $dx; $ny = $y + $dy
                if ($nx -lt 0 -or $ny -lt 0 -or $nx -ge $width -or $ny -ge $height -or $depth[$nx, $ny] -lt $ring -and $depth[$nx, $ny] -ne 99) { $edge = $true; break }
            } }
            if ($edge) { $found += , @($x, $y) }
        } }
        foreach ($f in $found) { $depth[$f[0], $f[1]] = $ring }
    }
    return , $depth
}

# A medal: three shapes one inside the next, each a clean one of its kind: the outline, the rim lit from the top left,
# and the dark field its icon sits on. $far measures a pixel's distance from the middle; the three limits are how far
# out the whole medal, its rim and its field reach.
function Medal($name, $outline, $light, $shade, $fill, $far, $limits) {
    $b = New-Object System.Drawing.Bitmap 19, 19
    for ($y = 0; $y -lt 19; $y++) { for ($x = 0; $x -lt 19; $x++) {
        $d = & $far ($x - 9) ($y - 9)
        $color = if ($d -gt $limits[0]) { Argb 0 0 }
            elseif ($d -gt $limits[1]) { Argb 255 $outline }
            elseif ($d -gt $limits[2]) { if (($x + $y) -lt 18) { Argb 255 $light } else { Argb 255 $shade } }
            else { Argb 255 $fill }
        $b.SetPixel($x, $y, $color)
    } }
    Save $b $name 19 19 0
}

# A track: an outline, a rim light above and dark below, a dark lip, and the groove the fill runs along (rows 3 to 7).
function Track($name, $outline, $light, $shade, $lip, $groove, [bool]$pointed) {
    $inside = {
        param($x, $y)
        $off = [Math]::Abs($y - 5)
        # Where the row ends on the right: a point, or a square end with its corners cut.
        $last = if ($pointed) { 67 - $off } elseif ($off -eq 5) { 66 } else { 67 }
        return $x -le $last
    }.GetNewClosure()
    $depth = Depth 68 11 $inside
    $b = New-Object System.Drawing.Bitmap 68, 11
    for ($y = 0; $y -lt 11; $y++) { for ($x = 0; $x -lt 68; $x++) {
        $d = $depth[$x, $y]
        $color = if ($d -eq 0) { Argb 0 0 }
            elseif ($d -eq 1) { Argb 255 $outline }
            elseif ($d -eq 2) { if ($y -lt 5) { Argb 255 $light } else { Argb 255 $shade } }
            elseif ($d -eq 3) { Argb 255 $lip }
            else { Argb 255 $groove }
        $b.SetPixel($x, $y, $color)
    } }
    Save $b $name 68 11 0
}
# The coin: circles 19, 17 and 13 across (as squared distances). The diamond: 9, 7 and 5 out along its edges.
Medal "mood_medal" 0x1E1408 0xFFE08A 0xB07A22 0x2A1C0C { param($x, $y) $x * $x + $y * $y } @(97, 76, 45)
Medal "lucidity_medal" 0x14181C 0xE9B6B8 0x8A4E7C 0x18101E { param($x, $y) [Math]::Abs($x) + [Math]::Abs($y) } @(9, 7, 5)
Track "mood_track" 0x1E1408 0xE8BC55 0x8C5A1C 0x241A0C 0x15100A $false
Track "lucidity_track" 0x14181C 0xCB8CAA 0x663A63 0x1E1626 0x100C16 $true

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
