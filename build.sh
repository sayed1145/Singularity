#!/usr/bin/env bash
# Builds the universal Singularity JAR (desktop .class + Android classes.dex).
set -euo pipefail
cd "$(dirname "$0")"

VENDOR="${VENDOR:-/opt/vendor}"
JDK="${JDK_HOME:-$VENDOR/jdk-17.0.21+9}"
MINDUSTRY="$VENDOR/Mindustry.jar"
R8="$VENDOR/r8.jar"
PLAT="$(ls "$VENDOR"/plat/*/android.jar | head -1)"

NAME="${NAME:-Singularity}"
VERSION="${VERSION:-$(grep -m1 '^version:' mod.hjson | sed 's/version: *//; s/"//g')}"
OUT="dist/${NAME}-${VERSION}.jar"

[ -x "$JDK/bin/javac" ] || { echo "missing JDK 17 at $JDK (run tools/setup_env.sh)"; exit 1; }
for f in "$MINDUSTRY" "$R8" "$PLAT"; do [ -f "$f" ] || { echo "missing $f (run tools/setup_env.sh)"; exit 1; }; done

echo "== compiling (java 17) =="
rm -rf build/classes build/dex && mkdir -p build/classes build/dex dist
find src -name '*.java' > build/srcs.txt
"$JDK/bin/javac" -nowarn -encoding UTF-8 -source 17 -target 17 \
  -cp "$MINDUSTRY" -d build/classes @"build/srcs.txt"

echo "== dex (d8, android) =="
"$JDK/bin/java" -Xmx2G -cp "$R8" com.android.tools.r8.D8 \
  --release --min-api 21 \
  --lib "$PLAT" --lib "$MINDUSTRY" \
  --output build/dex \
  $(find build/classes -name '*.class') 2>&1 | tail -5

echo "== packaging =="
rm -f "$OUT"
mkdir -p build/stage
cp -r build/classes/. build/stage/
if [ -d assets ]; then cp -r assets/. build/stage/; fi
cp mod.hjson icon.png build/stage/ 2>/dev/null || true
cp build/dex/classes.dex build/stage/classes.dex

( cd build/stage && "$JDK/bin/jar" cfM "../../$OUT" . )
ls -la "$OUT"
echo "built $OUT"
