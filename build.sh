#!/usr/bin/env bash
# Full build: compile -> model sanity check -> bake every sprite from the 3D model code (with the visibility audit)
# -> asset check -> desktop jar -> DEX -> universal (desktop + Android) jar
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"; VENDOR="${VENDOR:-$HOME/.cache/vendor}"
JAVA_HOME="${JAVA_HOME:-$(find "$VENDOR" -maxdepth 1 -type d -name 'jdk-17*' | head -1)}"
JAVA="$JAVA_HOME/bin/java"; JAR="$JAVA_HOME/bin/jar"; VERSION="8.3-beta"
MINDUSTRY_JAR="$VENDOR/Mindustry.jar"; R8_JAR="$VENDOR/r8.jar"; ANDROID_JAR="$VENDOR/plat/android-35/android.jar"
[ -f "$ANDROID_JAR" ] || ANDROID_JAR="$(find "$VENDOR/plat" -name android.jar | head -1)"
BUILD="$ROOT/build"; OUT="$ROOT/dist"
VENDOR="$VENDOR" JAVA_HOME="$JAVA_HOME" "$ROOT/build_classes.sh"
# every model builds headless, no primitive is left in a stale frame, every head frame is sane
(cd "$ROOT" && "$JAVA" -cp "build/tools:build/classes:$MINDUSTRY_JAR" preview.ModelCheck)
if [ "${SKIP_BAKE:-0}" != "1" ]; then
  # bakes blocks/3d (-hd, -front-hd, -preview, plain, -heads), items and liquids; exits non-zero if any model
  # shows an interior / cut face from the camera at any of 36 headings
  (cd "$ROOT" && "$JAVA" -Xmx1200m -Djava.awt.headless=true -cp "build/tools:build/classes:$MINDUSTRY_JAR" bake.Bake assets threads="${BAKE_THREADS:-2}" report=build/bake-report.txt)
fi
(cd "$ROOT" && "$JAVA" -Djava.awt.headless=true -cp "build/tools:build/classes:$MINDUSTRY_JAR" preview.AssetCheck assets/sprites)
rm -rf "$BUILD/jar" "$BUILD/dex"; mkdir -p "$BUILD/jar" "$BUILD/dex" "$OUT"
cp -r "$BUILD/classes/." "$BUILD/jar/"; cp "$ROOT/mod.hjson" "$ROOT/icon.png" "$BUILD/jar/"; cp -r "$ROOT/assets/." "$BUILD/jar/"
(cd "$BUILD/jar" && "$JAR" --create --file "$BUILD/SingularityDesktop.jar" .)
"$JAVA" -Xmx900m -cp "$R8_JAR" com.android.tools.r8.D8 --release --min-api 21 --lib "$ANDROID_JAR" --classpath "$MINDUSTRY_JAR" --output "$BUILD/dex" "$BUILD/SingularityDesktop.jar"
cp "$BUILD/dex/classes.dex" "$BUILD/jar/classes.dex"
rm -f "$OUT"/Singularity-v*.jar
(cd "$BUILD/jar" && "$JAR" --create --file "$OUT/Singularity-v$VERSION.jar" .)
echo "built: $OUT/Singularity-v$VERSION.jar"
