# Fonctions partagées par les hooks bash (portage des .ps1). Sourcé, jamais exécuté seul.

PAYLOAD="$(cat)"

# Lit un champ du payload JSON (chemin pointé, ex. tool_input.command) ; vide si absent.
json_field() {
  if command -v jq >/dev/null 2>&1; then
    printf '%s' "$PAYLOAD" | jq -r --arg p "$1" 'getpath($p | split(".")) // empty'
  elif command -v python3 >/dev/null 2>&1; then
    printf '%s' "$PAYLOAD" | python3 -c '
import json, sys
v = json.load(sys.stdin)
for k in sys.argv[1].split("."):
    v = v.get(k) if isinstance(v, dict) else None
print("" if v is None else json.dumps(v) if isinstance(v, bool) else v)' "$1"
  elif command -v node >/dev/null 2>&1; then
    printf '%s' "$PAYLOAD" | node -e '
let v = JSON.parse(require("fs").readFileSync(0, "utf8"));
for (const k of process.argv[1].split(".")) v = v && typeof v === "object" ? v[k] : undefined;
console.log(v == null ? "" : String(v));' "$1"
  else
    echo "Hook ignoré : jq, python3 ou node requis pour lire le payload JSON" >&2
    exit 1
  fi
}

ROOT="$(json_field cwd)"
[ -n "$ROOT" ] || ROOT="$PWD"

# Ne fait rien hors projet Maven ; sinon se place à la racine.
require_maven_project() {
  [ -f "$ROOT/pom.xml" ] || exit 0
  cd "$ROOT" || exit 0
  command -v mvn >/dev/null 2>&1 || { echo "Hook ignoré : mvn introuvable dans le PATH" >&2; exit 1; }
}

# Modules Maven touchés (extensions/<nom> ou premier dossier), lus sur stdin, joints par des virgules.
maven_modules() {
  local f m
  while IFS= read -r f; do
    [ -n "$f" ] || continue
    if [[ $f =~ ^(extensions/[^/]+)/ ]]; then m="${BASH_REMATCH[1]}"; else m="${f%%/*}"; fi
    [ -f "$ROOT/$m/pom.xml" ] && printf '%s\n' "$m"
  done | sort -u | paste -sd, -
}

# Classes de test des noms donnés : Foo -> FooTest / FooTests, dans un dossier test(s)/.
find_tests() {
  local n
  for n in "$@"; do
    find "$ROOT" -name .git -prune -o -type f \( -iname "${n}Test.java" -o -iname "${n}Tests.java" \) -print
  done | grep -Ei '/tests?/' | sed 's#.*/##; s/\.[^.]*$//' | sort -u | paste -sd, -
}
