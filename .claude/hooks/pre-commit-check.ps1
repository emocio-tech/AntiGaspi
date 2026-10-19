# Hook PreToolUse (Bash) : avant un `git commit`, lint Java + tests cibles.
# Ne fait rien hors projet Maven. Exit 2 = commit bloque, message renvoye a Claude.
$ErrorActionPreference = 'Stop'

function Fail($msg) { [Console]::Error.WriteLine($msg); exit 2 }

$payload = [Console]::In.ReadToEnd() | ConvertFrom-Json
$cmd = $payload.tool_input.command
if (-not $cmd -or $cmd -notmatch '\bgit\s+commit\b') { exit 0 }

$root = if ($payload.cwd) { $payload.cwd } else { (Get-Location).Path }
if (-not (Test-Path (Join-Path $root 'pom.xml'))) { exit 0 }
Set-Location $root

# Fichiers concernes : indexes, ou tous les suivis si `commit -a`
$diffArgs = if ($cmd -match '\s-[a-zA-Z]*a\b|--all') { @('diff', 'HEAD', '--name-only') } else { @('diff', '--cached', '--name-only') }
$files = @(git @diffArgs | Where-Object { $_ -like '*.java' })
if ($files.Count -eq 0) { exit 0 }

# Modules Maven touches (extensions/<nom> ou premier dossier)
$modules = $files | ForEach-Object {
    if ($_ -match '^(extensions/[^/]+)/') { $Matches[1] } else { ($_ -split '/')[0] }
} | Sort-Object -Unique | Where-Object { Test-Path (Join-Path $root "$_\pom.xml") }
if (-not $modules) { exit 0 }
$pl = $modules -join ','

# 1. Lint
$out = & mvn -q -B formatter:validate -pl $pl 2>&1
if ($LASTEXITCODE -ne 0) {
    Fail "Commit bloque : lint Java en echec (formatter:validate sur $pl). Corriger avec 'mvn formatter:format -pl $pl'.`n$($out | Select-Object -Last 30 | Out-String)"
}

# 2. Tests cibles : Foo.java -> FooTests / FooTest
$names = $files | ForEach-Object { [System.IO.Path]::GetFileNameWithoutExtension($_) } | Sort-Object -Unique
$tests = foreach ($n in $names) {
    $base = $n -replace 'Tests?$', ''
    Get-ChildItem -Path $root -Recurse -Include "${base}Test.java", "${base}Tests.java" -ErrorAction SilentlyContinue |
        Where-Object { $_.FullName -match '[\\/]tests?[\\/]' } |
        ForEach-Object { $_.BaseName }
}
$tests = @($tests | Sort-Object -Unique)
if ($tests.Count -eq 0) { exit 0 }

$out = & mvn -q -B test -pl $pl "-Dtest=$($tests -join ',')" -Dsurefire.failIfNoSpecifiedTests=false 2>&1
if ($LASTEXITCODE -ne 0) {
    Fail "Commit bloque : tests en echec ($($tests -join ', ')).`n$($out | Select-Object -Last 40 | Out-String)"
}
exit 0
