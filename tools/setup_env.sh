#!/usr/bin/env bash
# Downloads the toolchain used by build.sh / test_server.sh into $VENDOR (default ~/.cache/vendor):
# JDK 17, the official Mindustry v160.5 desktop + server jars, r8 (D8 dexer) and the Android platform jar.
set -euo pipefail
VERSION="${1:-v160.5}"; VENDOR="${VENDOR:-$HOME/.cache/vendor}"
mkdir -p "$VENDOR"; cd "$VENDOR"
if ! ls -d jdk-17* >/dev/null 2>&1; then curl -sL -o jdk17.tgz "https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse"; tar xzf jdk17.tgz; rm jdk17.tgz; fi
[ -f Mindustry.jar ] || curl -sL -o Mindustry.jar "https://github.com/Anuken/Mindustry/releases/download/$VERSION/Mindustry.jar"
[ -f server-release.jar ] || curl -sL -o server-release.jar "https://github.com/Anuken/Mindustry/releases/download/$VERSION/server-release.jar"
R8V="${R8_VERSION:-8.9.35}"; [ -f r8.jar ] || curl -sfL -o r8.jar "https://dl.google.com/dl/android/maven2/com/android/tools/r8/$R8V/r8-$R8V.jar"
if [ ! -f plat/android-35/android.jar ]; then curl -sL -o p35.zip https://dl.google.com/android/repository/platform-35_r02.zip; unzip -oq p35.zip '*/android.jar' -d plat; rm p35.zip; fi
echo "toolchain ready in $VENDOR"
