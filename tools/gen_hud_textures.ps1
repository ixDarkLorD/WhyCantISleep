# Draws the sleep HUD's own panels and bars as GUI sprites, under assets/whycantisleep/textures/gui/sprites/hud/,
# each with the .mcmeta that tells the game how to stretch it (nine-slice), so HudStyle draws them at any size:
#   card.png          the message card and the sleep board: slate, like a slab of night
#   mood_medal.png, mood_track.png, mood_frame.png              the mood gauge: a gold coin and a round capsule of blocks
#   lucidity_medal.png, lucidity_track.png, lucidity_frame.png  the lucidity gauge: a rose diamond and a pointed one
#   bar_back.png      the empty track of a status bar
#   sleep_fill.png    what fills it on the sleep board: green
#   mood_fill.png, lucidity_fill.png   what fills the gauges: bright gold, and rose
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
# of it for its bar. Mood's is a round gold coin on a round-ended bar; lucidity's is a rose-edged diamond on a bar
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

# A bar, in the manner of an arcade game's: a chunky capsule with a dark outline (lit along its top, darkest along its
# foot, and casting a shadow down and to the right), cut into eight blocks, each with a
# glint on its shoulder. It is drawn in three layers so the color can stop anywhere: the back (the outline and the
# empty blocks), then the fill stretched to the value (MindStatus), then the frame (the outline again, to round the
# fill's ends off, and the block seams, glints and foot shadow, which lie over full and empty blocks alike).
# The color runs along columns 2 to 65, rows 1 to 9. Mood's ends round; lucidity's runs on into a point.
function Bar($name, $outline, $outlineLight, $outlineDark, $empty, $emptyTop, [bool]$pointed) {
    $inside = {
        param($x, $y)
        $off = [Math]::Abs($y - 5)
        $first = if ($off -eq 5) { 2 } elseif ($off -eq 4) { 1 } else { 0 }
        $last = if ($pointed) { 71 - $off } elseif ($off -eq 5) { 65 } elseif ($off -eq 4) { 66 } else { 67 }
        return $x -ge $first -and $x -le $last
    }.GetNewClosure()
    $depth = Depth 72 11 $inside
    # A pixel wider and taller than the bar, for the shadow it casts down and to the right.
    $back = New-Object System.Drawing.Bitmap 73, 12
    $frame = New-Object System.Drawing.Bitmap 73, 12
    $clear = Argb 0 0
    for ($y = 0; $y -lt 12; $y++) { for ($x = 0; $x -lt 73; $x++) {
        $d = if ($x -lt 72 -and $y -lt 11) { $depth[$x, $y] } else { 0 }
        if ($d -eq 0) {
            $cast = $x -gt 0 -and $y -gt 0 -and $depth[($x - 1), ($y - 1)] -gt 0
            $back.SetPixel($x, $y, $(if ($cast) { Argb 110 0x000000 } else { $clear })); $frame.SetPixel($x, $y, $clear); continue
        }
        # The outline can be lit along its top, but both bars keep it the outline's own tone there (the author's call:
        # any lighter and it reads as a second rim around the bar); it is darkest along its foot.
        $rim = if ($y -eq 0 -or $depth[$x, ($y - 1)] -eq 0) { Argb 255 $outlineLight } elseif ($y -eq 10 -or $depth[$x, ($y + 1)] -eq 0) { Argb 255 $outlineDark } else { Argb 255 $outline }
        # The outline, and the two columns at each end the color never reaches.
        if ($d -eq 1 -or $x -lt 2) { $back.SetPixel($x, $y, $rim); $frame.SetPixel($x, $y, $rim); continue }
        if ($x -gt 65) {
            # Past the color: more outline, which makes a round end's cap and a pointed end's solid point.
            $back.SetPixel($x, $y, (Argb 255 $outline)); $frame.SetPixel($x, $y, (Argb 255 $outline)); continue
        }
        $back.SetPixel($x, $y, $(if ($y -eq 1) { Argb 255 $emptyTop } else { Argb 255 $empty }))
        $block = ($x - 2) % 8
        $over = if ($block -eq 7) { Argb 150 $outline }
            elseif ($y -eq 2 -and $block -ge 1 -and $block -le 3) { Argb 120 0xFFFFFF }
            elseif ($y -eq 9) { Argb 45 0x000000 }
            else { $clear }
        $frame.SetPixel($x, $y, $over)
    } }
    Save $back "${name}_track" 73 12 0
    Save $frame "${name}_frame" 73 12 0
}
# The coin: circles 19, 17 and 13 across (as squared distances). The diamond: 9, 7 and 5 out along its edges.
Medal "mood_medal" 0x1E1408 0xFFE08A 0xB07A22 0x2A1C0C { param($x, $y) $x * $x + $y * $y } @(97, 76, 45)
Medal "lucidity_medal" 0x14181C 0xE9B6B8 0x8A4E7C 0x18101E { param($x, $y) [Math]::Abs($x) + [Math]::Abs($y) } @(9, 7, 5)
Bar "mood" 0x2A1608 0x2A1608 0x120A04 0x4A3414 0x38260E $false
Bar "lucidity" 0x1C1220 0x1C1220 0x0A070E 0x3A2A3E 0x2A1E30 $true
# What fills a gauge: bright, flat color, nine rows from a light shoulder down to a deep foot. It only stretches.
function GaugeFill($name, $light, $base, $shade, $deep) {
    $b = New-Object System.Drawing.Bitmap 8, 9
    for ($y = 0; $y -lt 9; $y++) { for ($x = 0; $x -lt 8; $x++) {
        $b.SetPixel($x, $y, (Argb 255 $(if ($y -eq 0) { $light } elseif ($y -le 6) { $base } elseif ($y -eq 7) { $shade } else { $deep })))
    } }
    Save $b $name 8 9 0
}
GaugeFill "mood_fill" 0xFFE27A 0xFFC02E 0xE09A1E 0xB87616
GaugeFill "lucidity_fill" 0xF4C6CC 0xD992B0 0xA86488 0x8A4E7C
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
