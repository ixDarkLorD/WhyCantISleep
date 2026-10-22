# Writes assets/whycantisleep/sounds.json from the .ogg files in assets/whycantisleep/sounds/.
# Each of the mod's sounds plays its own files when there are any, and one of the game's sounds until then, so a
# sound can be replaced by dropping a file in and running this again. WCISSounds.java names the same sounds.
#
# A sound's files: "<name>.ogg", or several variants "<name>1.ogg", "<name>2.ogg" ... (also "<name>_1.ogg"), of which
# the game plays one at random. A dotted name such as "a.b" looks in the "a" folder for "b*.ogg".
# Run from the repo root: powershell -File tools/gen_sounds_json.ps1
$assets = Join-Path (Get-Location) "common\src\main\resources\assets\whycantisleep"
$folder = Join-Path $assets "sounds"

# name = the subtitle shown for it ("" for none), then the game's sounds that stand in for it.
$sounds = [ordered]@{
    "blocked"        = @("subtitles.whycantisleep.blocked", "minecraft:entity.warden.heartbeat")
    "heartbeat.beat"       = @("subtitles.whycantisleep.heartbeat.beat", "minecraft:entity.warden.heartbeat")
    "heartbeat.calm"       = @("", "minecraft:block.amethyst_block.chime")
    "heartbeat.click"      = @("", "minecraft:ui.button.click")
    "heartbeat.gasp"       = @("subtitles.whycantisleep.heartbeat.gasp", "minecraft:entity.player.hurt_drown")
    "heartbeat.heart_lost" = @("subtitles.whycantisleep.heartbeat.heart_lost", "minecraft:entity.item.break")
    "notice"         = @("", "minecraft:ui.toast.in")
    "nudge"          = @("subtitles.whycantisleep.nudge", "minecraft:block.note_block.bell")
    "warning"        = @("subtitles.whycantisleep.warning", "minecraft:block.note_block.bass")
}

$lines = @("{")
$names = @($sounds.Keys)
for ($i = 0; $i -lt $names.Count; $i++) {
    $name = $names[$i]
    $subtitle = $sounds[$name][0]
    $relative = $name.Replace(".", "/")
    $dir = Join-Path $folder (Split-Path $relative -Parent)
    $leaf = Split-Path $relative -Leaf
    $own = @()
    if (Test-Path $dir) {
        $own = @(Get-ChildItem $dir -Filter "*.ogg" -File | Where-Object { $_.BaseName -match "^$([regex]::Escape($leaf))(_?\d+)?$" } | Sort-Object Name)
    }
    $entries = @()
    if ($own.Count -gt 0) {
        $prefix = if ($relative.Contains("/")) { (Split-Path $relative -Parent).Replace("\", "/") + "/" } else { "" }
        foreach ($file in $own) { $entries += "      `"whycantisleep:$prefix$($file.BaseName)`"" }
        Write-Host ("{0,-22} {1} file(s) of its own" -f $name, $own.Count)
    } else {
        for ($s = 1; $s -lt $sounds[$name].Count; $s++) { $entries += "      { `"name`": `"$($sounds[$name][$s])`", `"type`": `"event`" }" }
        Write-Host ("{0,-22} the game's {1}" -f $name, (($sounds[$name] | Select-Object -Skip 1) -join ", "))
    }
    $lines += "  `"$name`": {"
    if ($subtitle -ne "") { $lines += "    `"subtitle`": `"$subtitle`"," }
    $lines += "    `"sounds`": ["
    $lines += ($entries -join ",`n")
    $lines += "    ]"
    $lines += "  }" + $(if ($i -lt $names.Count - 1) { "," } else { "" })
}
$lines += "}"
[IO.File]::WriteAllText((Join-Path $assets "sounds.json"), (($lines -join "`n") + "`n"), (New-Object Text.UTF8Encoding $false))
"Wrote sounds.json"
