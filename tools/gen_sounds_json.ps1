# Writes assets/whycantisleep/sounds.json from the .ogg files in assets/whycantisleep/sounds/.
# Each of the mod's sounds plays its own files when there are any, and one of the game's sounds until then, so a
# sound can be replaced by dropping a file in and running this again. WCISSounds.java names the same sounds.
#
# A sound's files: "<name>.ogg", or several variants "<name>1.ogg", "<name>2.ogg" ... (also "<name>_1.ogg"), of which
# the game plays one at random. A sound named "lurker.hurt" looks in the "lurker" folder for "hurt*.ogg".
# Run from the repo root: powershell -File tools/gen_sounds_json.ps1
$assets = Join-Path (Get-Location) "common\src\main\resources\assets\whycantisleep"
$folder = Join-Path $assets "sounds"

# name = the subtitle shown for it ("" for none), then the game's sounds that stand in for it.
$sounds = [ordered]@{
    "blocked"        = @("subtitles.whycantisleep.blocked", "minecraft:entity.warden.heartbeat")
    "lurker.ambient" = @("subtitles.whycantisleep.lurker.ambient", "minecraft:entity.warden.nearby_close")
    "lurker.death"   = @("subtitles.whycantisleep.lurker.death", "minecraft:entity.phantom.death")
    "lurker.emerge"  = @("subtitles.whycantisleep.lurker.emerge", "minecraft:entity.warden.emerge")
    "lurker.hurt"    = @("subtitles.whycantisleep.lurker.hurt", "minecraft:entity.phantom.hurt")
    "lurker.lure"    = @("subtitles.whycantisleep.lurker.lure", "minecraft:entity.warden.listening_angry")
    "lurker.notice"  = @("subtitles.whycantisleep.lurker.notice", "minecraft:entity.warden.roar")
    "lurker.vanish"  = @("subtitles.whycantisleep.lurker.vanish", "minecraft:entity.enderman.teleport")
    "notice"         = @("", "minecraft:ui.toast.in")
    "nudge"          = @("subtitles.whycantisleep.nudge", "minecraft:block.note_block.bell")
    "scare"          = @("subtitles.whycantisleep.scare", "minecraft:entity.enderman.stare", "minecraft:block.sculk_shrieker.shriek")
    "warning"        = @("subtitles.whycantisleep.warning", "minecraft:block.note_block.bass")
    "whisper"        = @("subtitles.whycantisleep.whisper", "minecraft:particle.soul_escape")
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
        Write-Host ("{0,-16} {1} file(s) of its own" -f $name, $own.Count)
    } else {
        for ($s = 1; $s -lt $sounds[$name].Count; $s++) { $entries += "      { `"name`": `"$($sounds[$name][$s])`", `"type`": `"event`" }" }
        Write-Host ("{0,-16} the game's {1}" -f $name, (($sounds[$name] | Select-Object -Skip 1) -join ", "))
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
