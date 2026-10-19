#!/usr/bin/env bash
# Hook Stop : a la fin d'un tour, si des .java ont change, lance les tests cibles.
# Portage bash de stop-tests.ps1. Ne fait rien hors projet Maven. Exit 2 = Claude doit corriger avant de s'arreter.
source "$(dirname "${BASH_SOURCE[0]}")/_common.sh"

# Garde anti-boucle : on ne re-bloque pas un arret deja relance par ce hook
[ "$(json_field stop_hook_active)" = "true" ] && exit 0
require_maven_project

# Fichiers .java modifies (suivis + nouveaux), hors fichiers de test eux-memes
files="$(git status --porcelain | cut -c4- | sed 's/^"//; s/"$//' | grep -i '\.java$' | grep -Eiv '/tests?/')"
[ -n "$files" ] || exit 0

pl="$(printf '%s\n' "$files" | maven_modules)"
[ -n "$pl" ] || exit 0

# Foo.java -> FooTest / FooTests
names="$(printf '%s\n' "$files" | sed 's#.*/##; s/\.[^.]*$//' | sort -u)"
# shellcheck disable=SC2086
tests="$(find_tests $names)"
[ -n "$tests" ] || exit 0

if ! out="$(mvn -q -B test -pl "$pl" "-Dtest=$tests" -Dsurefire.failIfNoSpecifiedTests=false 2>&1)"; then
  printf 'Tests en echec (%s) : corriger avant de terminer.\n%s\n' "${tests//,/, }" "$(printf '%s\n' "$out" | tail -n 40)" >&2
  exit 2
fi
exit 0
