#!/usr/bin/env bash
# Hook PreToolUse (Bash) : avant un `git push`, tests complets des modules touches.
# Portage bash de pre-push-tests.ps1. Ne fait rien hors projet Maven. Exit 2 = push bloque, message renvoye a Claude.
source "$(dirname "${BASH_SOURCE[0]}")/_common.sh"

cmd="$(json_field tool_input.command)"
printf '%s' "$cmd" | grep -Eiq '(^|[^[:alnum:]_])git[[:space:]]+push([^[:alnum:]_]|$)' || exit 0
require_maven_project

# Fichiers qui seraient pousses : commits locaux non presents sur le suivi amont
if upstream="$(git rev-parse --abbrev-ref --symbolic-full-name '@{u}' 2>/dev/null)" && [ -n "$upstream" ]; then
  range="$upstream..HEAD"
else
  range='HEAD~1..HEAD'
fi
files="$(git diff --name-only "$range" 2>/dev/null)"
[ -n "$files" ] || exit 0

pl="$(printf '%s\n' "$files" | maven_modules)"
[ -n "$pl" ] || exit 0

if ! out="$(mvn -q -B test -pl "$pl" 2>&1)"; then
  printf 'Push bloque : tests en echec sur %s.\n%s\n' "$pl" "$(printf '%s\n' "$out" | tail -n 40)" >&2
  exit 2
fi
exit 0
