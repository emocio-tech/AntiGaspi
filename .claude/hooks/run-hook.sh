#!/usr/bin/env bash
# Dispatcher multi-OS des hooks Claude Code. Usage : run-hook.sh <nom-du-hook>
# Windows (Git Bash / MSYS / Cygwin) : lance <nom>.ps1 avec PowerShell, sans le modifier.
# Linux / macOS / WSL : lance <nom>.sh. Si le script n'existe pas, ne fait rien (exit 0).
# Le payload JSON du hook arrive sur stdin et est transmis tel quel au script.

name="$1"
[ -n "$name" ] || { echo "run-hook.sh : nom de hook manquant" >&2; exit 1; }

src="${BASH_SOURCE[0]}"
# Sous Git Bash, le chemin peut arriver au format Windows (C:\...) : on le convertit.
command -v cygpath >/dev/null 2>&1 && src="$(cygpath -u "$src")"
dir="$(cd "$(dirname "$src")" && pwd)"

case "$(uname -s)" in
  MINGW* | MSYS* | CYGWIN*)
    # Script versionné dans le repo, sinon script personnel (ex. auto-commit-push.ps1, gitignoré)
    ps1="$dir/$name.ps1"
    [ -f "$ps1" ] || ps1="$HOME/.claude/hooks/$name.ps1"
    [ -f "$ps1" ] || exit 0
    command -v cygpath >/dev/null 2>&1 && ps1="$(cygpath -w "$ps1")"
    if command -v powershell >/dev/null 2>&1; then ps=powershell; else ps=pwsh; fi
    exec "$ps" -NoProfile -ExecutionPolicy Bypass -File "$ps1"
    ;;
  *)
    sh="$dir/$name.sh"
    [ -f "$sh" ] || exit 0
    exec bash "$sh"
    ;;
esac
