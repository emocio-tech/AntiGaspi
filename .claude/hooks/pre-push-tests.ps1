# Hook PreToolUse (Bash) : avant un `git push`, tests complets des modules touches.
# Ne fait rien hors projet Maven. Exit 2 = push bloque, message renvoye a Claude.
$ErrorActionPreference = 'Stop'

$payload = [Console]::In.ReadToEnd() | ConvertFrom-Json
$cmd = $payload.tool_input.command
if (-not $cmd -or $cmd -notmatch '\bgit\s+push\b') { exit 0 }

$root = if ($payload.cwd) { $payload.cwd } else { (Get-Location).Path }
if (-not (Test-Path (Join-Path $root 'pom.xml'))) { exit 0 }
Set-Location $root

# Fichiers qui seraient pousses : commits locaux non presents sur le suivi amont
$upstream = git rev-parse --abbrev-ref --symbolic-full-name '@{u}' 2>$null
$range = if ($LASTEXITCODE -eq 0 -and $upstream) { "$upstream..HEAD" } else { 'HEAD~1..HEAD' }
$files = @(git diff --name-only $range 2>$null)
if ($files.Count -eq 0) { exit 0 }

$modules = $files | ForEach-Object {
    if ($_ -match '^(extensions/[^/]+)/') { $Matches[1] } else { ($_ -split '/')[0] }
} | Sort-Object -Unique | Where-Object { Test-Path (Join-Path $root "$_\pom.xml") }
if (-not $modules) { exit 0 }
$pl = $modules -join ','

$out = & mvn -q -B test -pl $pl 2>&1
if ($LASTEXITCODE -ne 0) {
    [Console]::Error.WriteLine("Push bloque : tests en echec sur $pl.`n$($out | Select-Object -Last 40 | Out-String)")
    exit 2
}
exit 0
