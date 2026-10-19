#!/usr/bin/env bash
# Hook PreToolUse (Bash) : avant un `git commit`, lint Java + tests cibles.
# Portage bash de pre-commit-check.ps1. Ne fait rien hors projet Maven. Exit 2 = commit bloque, message renvoye a Claude.
source "$(dirname "${BASH_SOURCE[0]}")/_common.sh"

fail() { printf '%s\n' "$1" >&2; exit 2; }

cmd="$(json_field tool_input.command)"
printf '%s' "$cmd" | grep -Eiq '(^|[^[:alnum:]_])git[[:space:]]+commit([^[:alnum:]_]|$)' || exit 0
require_maven_project

# Fichiers concernes : indexes, ou tous les suivis si `commit -a`
if printf '%s' "$cmd" | grep -Eiq '[[:space:]]-[a-zA-Z]*a([^[:alnum:]_]|$)|--all'; then
  files="$(git diff HEAD --name-only | grep -i '\.java$')"
else
  files="$(git diff --cached --name-only | grep -i '\.java$')"
fi
[ -n "$files" ] || exit 0

pl="$(printf '%s\n' "$files" | maven_modules)"
[ -n "$pl" ] || exit 0

# 1. Lint
if ! out="$(mvn -q -B formatter:validate -pl "$pl" 2>&1)"; then
  fail "Commit bloque : lint Java en echec (formatter:validate sur $pl). Corriger avec 'mvn formatter:format -pl $pl'.
$(printf '%s\n' "$out" | tail -n 30)"
fi

# 2. Tests cibles : Foo.java -> FooTests / FooTest
names="$(printf '%s\n' "$files" | sed 's#.*/##; s/\.[^.]*$//; s/Tests\{0,1\}$//' | sort -u)"
# shellcheck disable=SC2086
tests="$(find_tests $names)"
[ -n "$tests" ] || exit 0

if ! out="$(mvn -q -B test -pl "$pl" "-Dtest=$tests" -Dsurefire.failIfNoSpecifiedTests=false 2>&1)"; then
  fail "Commit bloque : tests en echec (${tests//,/, }).
$(printf '%s\n' "$out" | tail -n 40)"
fi
exit 0
