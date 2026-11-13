#!/usr/bin/env bash
# Import 0.2.0 into an existing Git checkout, preserving Git history and BMC build cache.
set -o pipefail
ZIP="${YR_AI_ZIP:-$HOME/Downloads/YrellDevstudioAI-0.2.0-FullSource.zip}"
TARGET="${YR_AI_TARGET:-$HOME/Git/YrellDevstudioAI}"
SDK="${YR_DEVSTUDIO_ARCHIVE:-$HOME/Downloads/componentDeveloperStudioWindows.zip}"
if [[ ! -e "$SDK" ]]; then SDK="$HOME/Downloads/ARSuiteKitDeveloperWindows26.1.01/Disk1/files/DeveloperStudio/componentDeveloperStudioWindows.zip"; fi
if [[ ! -f "$ZIP" ]]; then echo "Missing: $ZIP" >&2;exit 2;fi
for exe in unzip rsync git java javac jar;do command -v "$exe" >/dev/null || { echo "Missing: $exe" >&2;exit 2; };done
if [[ ! -e "$SDK" && ! -f "$TARGET/.build/devstudio/x64/plugins/com.bmc.arsys.studio.ui_26.1.1.build001.jar" ]];then
  echo "BMC Developer Studio SDK not found. Set YR_DEVSTUDIO_ARCHIVE before running." >&2;exit 2
fi
TEMP="$(mktemp -d)" || exit 3
trap 'rm -rf -- "$TEMP"' EXIT
unzip -q "$ZIP" -d "$TEMP" || exit 3
FROM="$TEMP/YrellDevstudioAI-0.2.0"
[[ -f "$FROM/source/se.yrell.aiassistant/plugin.xml" ]] || { echo 'Invalid source ZIP';exit 4; }
if [[ -e "$TARGET" && ! -d "$TARGET/.git" ]];then
  echo "Refusing to change non-Git project: $TARGET" >&2;exit 4
fi
mkdir -p "$TARGET" || exit 5
if [[ ! -d "$TARGET/.git" ]];then git -C "$TARGET" init -q || exit 5;fi
if [[ -d "$TARGET/source" ]];then
  BACKUP="${TARGET}-backup-$(date +%Y%m%d-%H%M%S).tar.gz"
  tar -czf "$BACKUP" --exclude=.git --exclude=.build -C "$TARGET" . || exit 5
  echo "Backup: $BACKUP"
fi
for dir in source scripts tests;do
  mkdir -p "$TARGET/$dir"
  rsync -a --delete "$FROM/$dir/" "$TARGET/$dir/" || exit 6
done
for f in README.md CHANGELOG.md .gitignore;do cp "$FROM/$f" "$TARGET/$f" || exit 6;done
bash "$TARGET/scripts/test-core.sh" || exit 7
bash "$TARGET/scripts/build.sh" "$SDK" || exit 8
bash "$TARGET/scripts/test.sh" || exit 9
mkdir -p "$HOME/Downloads"
cp "$TARGET/install/se.yrell.aiassistant_0.2.0.jar" "$HOME/Downloads/" || exit 10
printf '\nSource: %s\nJAR: %s\n' "$TARGET" "$HOME/Downloads/se.yrell.aiassistant_0.2.0.jar"
git -C "$TARGET" status --short || true
