#!/usr/bin/env bash
# compile mod + tools to build/classes (desktop JVM) - used by bake / preview / tests
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"; VENDOR="${VENDOR:-$HOME/.cache/vendor}"
JAVA_HOME="${JAVA_HOME:-$(find "$VENDOR" -maxdepth 1 -type d -name 'jdk-17*' | head -1)}"
rm -rf "$ROOT/build/classes" "$ROOT/build/tools"; mkdir -p "$ROOT/build/classes" "$ROOT/build/tools"
"$JAVA_HOME/bin/javac" -nowarn -encoding UTF-8 --release 17 -cp "$VENDOR/Mindustry.jar" -d "$ROOT/build/classes" ${SRC_FILES:-$(find "$ROOT/src" -name '*.java')}
"$JAVA_HOME/bin/javac" -nowarn -encoding UTF-8 --release 17 -cp "$VENDOR/Mindustry.jar:$ROOT/build/classes" -d "$ROOT/build/tools" $(find "$ROOT/tools" -name '*.java' ${TOOL_FILTER:-})
echo compiled
