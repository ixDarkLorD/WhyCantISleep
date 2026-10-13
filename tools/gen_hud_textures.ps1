# Draws the sleep HUD's own panels and bars as GUI sprites, under assets/whycantisleep/textures/gui/sprites/hud/,
# each with the .mcmeta that tells the game how to stretch it (nine-slice), so HudStyle draws them at any size:
#   card.png          the message card and the sleep board: dusk plum, outlined and shaded like the bars
#   mood_medal.png, mood_track.png, mood_frame.png              the mood gauge: a gold coin and a round capsule of blocks
#   lucidity_medal.png, lucidity_track.png, lucidity_frame.png  the lucidity gauge: a rose diamond and a pointed one
#   bar_back.png      the empty track of a status bar
#   sleep_fill.png    what fills it on the sleep board: green
#   mood_fill.png, lucidity_fill.png   what fills the gauges: bright gold, and rose
# The card is 16x16 with a 4 pixel border (and a pixel of shadow); the plain track is 8x5 with a 1 pixel border; the fills just stretch.
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
                # Off the picture counts as outside. (Checked on its own: a negative index would wrap round to the far side.)
                if ($nx -lt 0 -or $ny -lt 0 -or $nx -ge $width -or $ny -ge $height) { $edge = $true; break }
                if ($depth[$nx, $ny] -lt $ring) { $edge = $true; break }
            } }
            if ($edge) { $found += , @($x, $y) }
        } }
        foreach ($f in $found) { $depth[$f[0], $f[1]] = $ring }
    }
    return , $depth
}

# The card is made the way the bars and medals are: a chunky dark outline with rounded corners, darkest along its
# foot; a rim lit from the top left; a sunk, nearly solid middle with a shadow under the lit side of the rim; and a
# shadow cast down and to the right, for which the picture is a pixel wider and taller than the card.
function Card($name, $outline, $outlineDark, $light, $shade, $shadow, $fill, $fillAlpha) {
    $inside = {
        param($x, $y)
        $off = [Math]::Min($y, 15 - $y)
        $cut = if ($off -eq 0) { 2 } elseif ($off -eq 1) { 1 } else { 0 }
        return $x -ge $cut -and $x -le 15 - $cut
    }
    $depth = Depth 16 16 $inside
    $b = New-Object System.Drawing.Bitmap 17, 17
    for ($y = 0; $y -lt 17; $y++) { for ($x = 0; $x -lt 17; $x++) {
        $d = if ($x -lt 16 -and $y -lt 16) { $depth[$x, $y] } else { 0 }
        $lit = ($x + $y) -lt 15
        $color = if ($d -eq 0) { if ($x -gt 0 -and $y -gt 0 -and $depth[($x - 1), ($y - 1)] -gt 0) { Argb 110 0x000000 } else { Argb 0 0 } }
            elseif ($d -eq 1) { if ($lit) { Argb 255 $outline } else { Argb 255 $outlineDark } }
            elseif ($d -eq 2) { if ($lit) { Argb 255 $light } else { Argb 255 $shade } }
            elseif ($d -eq 3 -and $lit) { Argb $fillAlpha $shadow }
            else { Argb $fillAlpha $fill }
        $b.SetPixel($x, $y, $color)
    } }
    $b.Save((Join-Path $dir "$name.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $b.Dispose()
    # The corners keep their size and the sides stretch; the right and bottom borders carry the shadow's pixel too.
    $meta = "{`n  `"gui`": {`n    `"scaling`": {`n      `"type`": `"nine_slice`",`n      `"width`": 17,`n      `"height`": 17,`n      `"border`": {`n        `"left`": 4,`n        `"top`": 4,`n        `"right`": 5,`n        `"bottom`": 5`n      }`n    }`n  }`n}`n"
    [IO.File]::WriteAllText((Join-Path $dir "$name.png.mcmeta"), $meta, $utf8)
}

# Dusk plum, from the same palette as the lucidity bar and the Lurker.
Card "card" 0x1C1220 0x0A070E 0x7A5A84 0x3A2A3E 0x140E1A 0x221828 244

# The two status gauges are shaped nothing like the card: each is a medal holding its icon, with a track running out
# of it for its bar. Mood's is a round gold coin on a round-ended bar; lucidity's is a rose-edged diamond on a bar
# that ends in a point. They are drawn at their own size, never stretched (MindStatus has the sizes).
# Both are built the same clean way: a shape, then its rings counted in from the edge, each ring one flat tone.

# A medal: three shapes one inside the next, each a clean one of its kind: the outline, the rim, and the sunk field
# its icon sits on. $far measures a pixel's distance from the middle; the limits are how far out the whole medal, its
# rim, its field, and the field's flat middle reach.
# The light comes from the top left. The rim runs through four tones round its turn, from a highlight facing the
# light to a shade facing away; the field, being sunk, is shaded the other way just inside the rim (a shadow under
# the lit side, a little light caught on the far side); the outline is darkest along the foot; and the whole medal
# casts a shadow down and to the right, for which the picture is a pixel wider and taller than the medal.
function Medal($name, $outline, $outlineDark, $rim, $field, $far, $limits) {
    $b = New-Object System.Drawing.Bitmap 20, 20
    for ($y = 0; $y -lt 20; $y++) { for ($x = 0; $x -lt 20; $x++) {
        $dx = $x - 9; $dy = $y - 9
        $d = & $far $dx $dy
        if ($d -gt $limits[0]) {
            $cast = (& $far ($dx - 1) ($dy - 1)) -le $limits[0]
            $b.SetPixel($x, $y, $(if ($cast) { Argb 110 0x000000 } else { Argb 0 0 })); continue
        }
        # How squarely the pixel faces the light: 1 towards it, -1 away.
        $length = [Math]::Sqrt($dx * $dx + $dy * $dy)
        $facing = if ($length -eq 0) { 0 } else { -($dx + $dy) / ($length * 1.4142) }
        $color = if ($d -gt $limits[1]) { if ($facing -lt -0.5) { $outlineDark } else { $outline } }
            elseif ($d -gt $limits[2]) { if ($facing -gt 0.8) { $rim[0] } elseif ($facing -gt 0.2) { $rim[1] } elseif ($facing -gt -0.5) { $rim[2] } else { $rim[3] } }
            elseif ($d -gt $limits[3]) { if ($facing -gt 0.2) { $field[0] } elseif ($facing -lt -0.5) { $field[2] } else { $field[1] } }
            else { $field[1] }
        $b.SetPixel($x, $y, (Argb 255 $color))
    } }
    Save $b $name 20 20 0
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
# The coin: circles 19, 17, 13 and 11 across (as squared distances). The diamond: 9, 7, 5 and 4 out along its edges.
# Rim tones run highlight, light, middle, shade; field tones run shadow, floor, caught light.
Medal "mood_medal" 0x1E1408 0x0E0802 @(0xFFF6CC, 0xFFD966, 0xD9A63C, 0x8E5E18) @(0x160E04, 0x2A1C0C, 0x43300F) { param($x, $y) $x * $x + $y * $y } @(97, 76, 45, 32)
Medal "lucidity_medal" 0x14181C 0x08090C @(0xFFEAEE, 0xE9B6B8, 0xCB8CAA, 0x7A4470) @(0x0C0812, 0x18101E, 0x32203A) { param($x, $y) [Math]::Abs($x) + [Math]::Abs($y) } @(9, 7, 5, 4)
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
    $b.SetPixel($x, $y, $(if ($edge) { Argb 255 $(if ($y -eq 4) { 0x0A070E } else { 0x1C1220 }) } elseif ($y -eq 1) { Argb 255 0x2A1E30 } else { Argb 255 0x3A2A3E }))
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
