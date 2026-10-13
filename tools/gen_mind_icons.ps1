# Draws assets/whycantisleep/textures/gui/mind_icons.png: the 9x9 icons of the mood and sanity status, in the manner
# of the game's hearts and hunger. Two rows of four on a 36x18 sheet (MindIcons.java has the same places):
#   row 0, mood:    cheerful, content/flat, gloomy, miserable   (a face)
#   row 1, sanity:  steady, uneasy, shaken, breaking            (an eye)
# Run from the repo root: powershell -File tools/gen_mind_icons.ps1
Add-Type -AssemblyName System.Drawing

function Rgb($r, $g, $b) { [System.Drawing.Color]::FromArgb(255, $r, $g, $b) }
$clear = [System.Drawing.Color]::FromArgb(0, 0, 0, 0)
$outline = Rgb 20 20 28

# Each icon: nine rows of nine letters, and what each letter paints ("." nothing, "k" the outline).
function Paint($bitmap, $column, $row, $rows, $colors) {
    for ($y = 0; $y -lt 9; $y++) {
        for ($x = 0; $x -lt 9; $x++) {
            $letter = [string]$rows[$y][$x]
            $color = if ($letter -eq ".") { $clear } elseif ($letter -eq "k") { $outline } else { $colors[$letter] }
            $bitmap.SetPixel($column * 9 + $x, $row * 9 + $y, $color)
        }
    }
}

$bitmap = New-Object System.Drawing.Bitmap 36, 18

# Faces: "a" the face, "h" its lit upper edge. The mouth is drawn in the outline color.
$smile = @("..kkkkk..", ".khhhhhk.", "kaakakaak", "kaakakaak", "kaaaaaaak", "kakaaakak", "kaakkkaak", ".kaaaaak.", "..kkkkk..")
$flat  = @("..kkkkk..", ".khhhhhk.", "kaakakaak", "kaakakaak", "kaaaaaaak", "kaaaaaaak", "kaakkkaak", ".kaaaaak.", "..kkkkk..")
$frown = @("..kkkkk..", ".khhhhhk.", "kaaaaaaak", "kaakakaak", "kaaaaaaak", "kaakkkaak", "kakaaakak", ".kaaaaak.", "..kkkkk..")
$weep  = @("..kkkkk..", ".khhhhhk.", "kakaaakak", "kaakakaak", "kaataaaak", "kaakkkaak", "kakaaakak", ".kaaaaak.", "..kkkkk..")
Paint $bitmap 0 0 $smile @{ a = (Rgb 255 213 74); h = (Rgb 255 236 150) }
Paint $bitmap 1 0 $flat  @{ a = (Rgb 222 196 110); h = (Rgb 240 222 160) }
Paint $bitmap 2 0 $frown @{ a = (Rgb 150 158 190); h = (Rgb 184 190 214) }
Paint $bitmap 3 0 $weep  @{ a = (Rgb 104 112 156); h = (Rgb 140 148 186); t = (Rgb 120 200 255) }

# Eyes: "w" the white, "i" the iris, "p" the pupil. The colors run down the Lurker's palette as sanity goes.
$open   = @(".........", "..kkkkk..", ".kwwwwwk.", "kwwiiiwwk", "kwwipiwwk", "kwwiiiwwk", ".kwwwwwk.", "..kkkkk..", ".........")
$narrow = @(".........", ".........", "..kkkkk..", "kkwiiiwkk", "kwwipiwwk", "kkwiiiwkk", "..kkkkk..", ".........", ".........")
$crack  = @(".........", "..kkkkk..", ".kwwkwwk.", "kwwikiwwk", "kwwipkwwk", "kwwiikwwk", ".kwwwkwk.", "..kkkkk..", ".........")
$void   = @(".........", "..kkkkk..", ".kwwwwwk.", "kwwiiiwwk", "kwwipiwwk", "kwwiiiwwk", ".kwwwwwk.", "..kkkkk..", ".........")
Paint $bitmap 0 1 $open   @{ w = (Rgb 240 240 244); i = (Rgb 203 140 170); p = (Rgb 20 24 28) }
Paint $bitmap 1 1 $narrow @{ w = (Rgb 214 206 214); i = (Rgb 168 100 136); p = (Rgb 20 24 28) }
Paint $bitmap 2 1 $crack  @{ w = (Rgb 170 150 172); i = (Rgb 102 58 99); p = (Rgb 20 24 28) }
# Breaking: the eye turned inside out, a lit ring around a void.
Paint $bitmap 3 1 $void   @{ w = (Rgb 58 42 62); i = (Rgb 233 182 184); p = (Rgb 2 2 4) }

$out = Join-Path (Get-Location) "common\src\main\resources\assets\whycantisleep\textures\gui\mind_icons.png"
$bitmap.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
$bitmap.Dispose()
"Wrote $out"
