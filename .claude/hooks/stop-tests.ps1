# Hook Stop : a la fin d'un tour, si des .java ont change, lance les tests cibles.
# Ne fait rien hors projet Maven. Exit 2 = Claude doit corriger avant de s'arreter.
$ErrorActionPreference = 'Stop'

$payload = [Console]::In.ReadToEnd() | ConvertFrom-Json
# Garde anti-boucle : on ne re-bloque pas un arret deja relance par ce hook
if ($payload.stop_hook_active) { exit 0 }

$root = if ($payload.cwd) { $payload.cwd } else { (Get-Location).Path }
if (-not (Test-Path (Join-Path $root 'pom.xml'))) { exit 0 }
Set-Location $root

# Fichiers .java modifies (suivis + nouveaux), hors fichiers de test eux-memes
$files = @(git status --porcelain | ForEach-Object { $_.Substring(3).Trim('"') } |
    Where-Object { $_ -like '*.java' -and $_ -notmatch '[\\/]tests?[\\/]' })
if ($files.Count -eq 0) { exit 0 }

$modules = $files | ForEach-Object {
    if ($_ -match '^(extensions/[^/]+)/') { $Matches[1] } else { ($_ -split '/')[0] }
} | Sort-Object -Unique | Where-Object { Test-Path (Join-Path $root "$_\pom.xml") }
if (-not $modules) { exit 0 }
$pl = $modules -join ','

# Foo.java -> FooTest / FooTests
$names = $files | ForEach-Object { [System.IO.Path]::GetFileNameWithoutExtension($_) } | Sort-Object -Unique
$tests = foreach ($n in $names) {
    Get-ChildItem -Path $root -Recurse -Include "${n}Test.java", "${n}Tests.java" -ErrorAction SilentlyContinue |
        Where-Object { $_.FullName -match '[\\/]tests?[\\/]' } |
        ForEach-Object { $_.BaseName }
}
$tests = @($tests | Sort-Object -Unique)
if ($tests.Count -eq 0) { exit 0 }

$out = & mvn -q -B test -pl $pl "-Dtest=$($tests -join ',')" -Dsurefire.failIfNoSpecifiedTests=false 2>&1
if ($LASTEXITCODE -ne 0) {
    [Console]::Error.WriteLine("Tests en echec ($($tests -join ', ')) : corriger avant de terminer.`n$($out | Select-Object -Last 40 | Out-String)")
    exit 2
}
exit 0
