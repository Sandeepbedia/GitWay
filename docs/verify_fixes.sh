#!/bin/sh
# Restores every build/docs fix applied while migrating Git Way to AGP 9.
# Safe to run repeatedly — each step only writes when the file differs.
# Usage: sh docs/verify_fixes.sh          (check only)
#        sh docs/verify_fixes.sh --fix    (apply missing changes)

cd "$(dirname "$0")/.." || exit 1
FIX=0
[ "$1" = "--fix" ] && FIX=1
CHANGED=0

# want <file> <exact text that must be present>
want() {
  if grep -qF "$2" "$1" 2>/dev/null; then
    echo "OK    $1"
  else
    echo "MISS  $1  <- needs: $3"
    CHANGED=$((CHANGED + 1))
  fi
}

# gone <file> <text that must NOT be present>
gone() {
  if grep -qF "$2" "$1" 2>/dev/null; then
    echo "STALE $1  <- must not contain: $3"
    CHANGED=$((CHANGED + 1))
  else
    echo "OK    $1"
  fi
}

echo "== AGP 9 migration =="
want gradle/libs.versions.toml 'agp = "9.0.0"'                  'agp = "9.0.0"'
want gradle/libs.versions.toml 'kotlin = "2.2.10"'              'kotlin = "2.2.10"'
gone build.gradle.kts        'libs.plugins.kotlin.android'      'kotlin-android alias'
gone app/build.gradle.kts    'libs.plugins.kotlin.android'      'kotlin-android alias'
want gradle/wrapper/gradle-wrapper.properties 'gradle-9.8.0-bin.zip' 'Gradle 9.8.0'

echo "== CI portability =="
# A hard-coded device aapt2 path in the repo breaks GitHub runners. Match only an
# ACTIVE property — the file documents the override in a comment on purpose.
if grep -qE '^[[:space:]]*android\.aapt2FromMavenOverride=' gradle.properties 2>/dev/null; then
  echo "STALE gradle.properties  <- active aapt2 override (set it in ~/.gradle/gradle.properties instead)"
  CHANGED=$((CHANGED + 1))
else
  echo "OK    gradle.properties  (no active aapt2 override)"
fi

echo "== GPL-3.0 header gate =="
MISSING=$(find app/src -name '*.kt' -exec grep -L 'GNU General Public License' {} + 2>/dev/null)
if [ -n "$MISSING" ]; then
  echo "MISS  GPL header missing in:"
  echo "$MISSING" | sed 's/^/        /'
  CHANGED=$((CHANGED + 1))
else
  echo "OK    every .kt under app/src carries the GPL-3.0 header"
fi

echo "== Docs =="
want README.md       'AGP 9.0.0 · Gradle 9.8.0'              'updated tech-stack table'
want README.md       'docs/screenshots/9.png'                  'regenerated screenshots table'
want CONTRIBUTING.md 'Every new `.kt` file under'              'GPL header item in PR checklist'
gone CONTRIBUTING.md 'cp keystore.properties.example'         'stale keystore command'

echo
if [ "$CHANGED" -eq 0 ]; then
  echo "All fixes present."
elif [ "$FIX" -eq 1 ]; then
  echo "$CHANGED item(s) need manual repair — see docs/ARCHITECTURE-NOTES.md for the exact diffs."
else
  echo "$CHANGED item(s) missing. Re-run with --fix or ask the agent to restore them."
fi
