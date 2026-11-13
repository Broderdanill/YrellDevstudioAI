#!/usr/bin/env bash
# Compile against the exact Developer Studio 26.1.1 plugins used on the target workstation.
set -o pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
INPUT="${1:-${YR_DEVSTUDIO_ARCHIVE:-$HOME/Downloads/componentDeveloperStudioWindows.zip}}"
BUILD="$ROOT/.build"
PLUGINS="$BUILD/devstudio/x64/plugins"
if [[ -d "$INPUT/x64/plugins" ]]; then PLUGINS="$INPUT/x64/plugins"
elif [[ -d "$INPUT/plugins" ]]; then PLUGINS="$INPUT/plugins"
elif [[ -f "$INPUT" ]]; then
  if [[ ! -f "$PLUGINS/com.bmc.arsys.studio.ui_26.1.1.build001.jar" ]]; then
    mkdir -p "$BUILD/devstudio" || exit 2
    echo 'Extracting BMC Developer Studio libraries...'
    unzip -q "$INPUT" 'x64/plugins/*' -d "$BUILD/devstudio" || exit 2
  fi
elif [[ ! -d "$PLUGINS" ]]; then
  echo 'Developer Studio libraries missing. Pass componentDeveloperStudioWindows.zip or a plugin directory.' >&2; exit 2
fi
if [[ ! -f "$PLUGINS/com.bmc.arsys.studio.ui_26.1.1.build001.jar" ]]; then
  echo 'Developer Studio 26.1.1 build001 JAR missing in plugin directory.' >&2; exit 3
fi
command -v javac >/dev/null || { echo 'JDK 17+ required';exit 4; }
mkdir -p "$BUILD/classes" "$BUILD/package" "$ROOT/install" || exit 5
find "$ROOT/source/se.yrell.aiassistant/src" -name '*.java' | sort > "$BUILD/sources.txt"
CP="$(find -L "$PLUGINS" -name '*.jar' | sort | paste -sd: -)"
rm -rf "$BUILD/classes" "$BUILD/package"
mkdir -p "$BUILD/classes" "$BUILD/package" || exit 5
echo "Compiling $(wc -l < "$BUILD/sources.txt") Java files..."
javac --release 17 -proc:none -encoding UTF-8 -cp "$CP" -d "$BUILD/classes" @"$BUILD/sources.txt" || exit 5
cp -a "$BUILD/classes"/. "$BUILD/package"/ || exit 6
cp "$ROOT/source/se.yrell.aiassistant/plugin.xml" "$ROOT/source/se.yrell.aiassistant/README.md" "$BUILD/package/" || exit 6
rm -f "$ROOT"/install/se.yrell.aiassistant_*.jar
jar --create --file "$ROOT/install/se.yrell.aiassistant_0.2.0.jar" --manifest "$ROOT/source/se.yrell.aiassistant/META-INF/MANIFEST.MF" -C "$BUILD/package" . || exit 6
echo "Built: $ROOT/install/se.yrell.aiassistant_0.2.0.jar"
