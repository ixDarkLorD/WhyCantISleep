# Draws assets/whycantisleep/textures/gui/blocker_arrow.png: two 16x16 arrowheads pointing right, side by side.
# Frame 0 (solid) is for a monster outside the view, frame 1 (hollow) for one in view but behind blocks.
# The arrow is white and grey so the HUD can tint it with the monster's highlight color; BlockerArrows shares the sizes.
# Run from the repo root: powershell -File tools/gen_arrow_texture.ps1
Add-Type -AssemblyName System.Drawing

$size = 16
# The arrowhead by rows: first and last column of each row. A notch at the back makes it read as a pointer, not a triangle.
$rows = @{ 2 = @(2, 4); 3 = @(2, 6); 4 = @(3, 8); 5 = @(4, 10); 6 = @(5, 12); 7 = @(6, 14)
           8 = @(6, 14); 9 = @(5, 12); 10 = @(4, 10); 11 = @(3, 8); 12 = @(2, 6); 13 = @(2, 4) }
$body = New-Object 'System.Collections.Generic.HashSet[string]'
foreach ($y in $rows.Keys) { for ($x = $rows[$y][0]; $x -le $rows[$y][1]; $x++) { [void]$body.Add("$x,$y") } }
$sides = @(@(1, 0), @(-1, 0), @(0, 1), @(0, -1))
function Touches($x, $y, [bool]$inside) {
    foreach ($o in $sides) { if ($body.Contains("$($x + $o[0]),$($y + $o[1])") -eq $inside) { return $true } }
    return $false
}

$bitmap = New-Object System.Drawing.Bitmap ($size * 2), $size
$clear = [System.Drawing.Color]::FromArgb(0, 0, 0, 0)
$outline = [System.Drawing.Color]::FromArgb(255, 20, 20, 28)
for ($y = 0; $y -lt $size; $y++) {
    for ($x = 0; $x -lt $size; $x++) {
        $solid = $clear
        $hollow = $clear
        if ($body.Contains("$x,$y")) {
            $edge = Touches $x $y $false
            # Lit from above: a bright rim along the upper edge, the lower half a shade darker.
            $shade = if ($edge -and $y -lt 8) { 255 } elseif ($y -lt 8) { 232 } elseif ($edge) { 168 } else { 198 }
            $solid = [System.Drawing.Color]::FromArgb(255, $shade, $shade, $shade)
            if ($edge) { $hollow = [System.Drawing.Color]::FromArgb(255, $(if ($y -lt 8) { 255 } else { 205 }), $(if ($y -lt 8) { 255 } else { 205 }), $(if ($y -lt 8) { 255 } else { 205 })) }
            else { $hollow = [System.Drawing.Color]::FromArgb(110, 20, 20, 28) }
        } elseif (Touches $x $y $true) {
            $solid = $outline
            $hollow = $outline
        }
        $bitmap.SetPixel($x, $y, $solid)
        $bitmap.SetPixel($x + $size, $y, $hollow)
    }
}

$out = Join-Path (Get-Location) "common\src\main\resources\assets\whycantisleep\textures\gui\blocker_arrow.png"
$bitmap.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
$bitmap.Dispose()
"Wrote $out"
