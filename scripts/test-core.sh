#!/usr/bin/env bash
# Tests the entire local export pipeline without BMC SDK or network access.
set -o pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
CLASS="$ROOT/.build/coretest"
mkdir -p "$CLASS" || exit 2
SRC="$ROOT/source/se.yrell.aiassistant/src/se/yrell/ai/core"
javac --release 17 -encoding UTF-8 -d "$CLASS" "$SRC/Json.java" "$SRC/TaskTemplates.java" "$SRC/TaskExporter.java" "$ROOT/tests/TaskExportTest.java" || exit 3
java -cp "$CLASS" TaskExportTest || exit 4
python3 - "$ROOT" <<'PYCODE'
import sys,xml.etree.ElementTree as ET
r=sys.argv[1]
tree=ET.parse(r+'/source/se.yrell.aiassistant/plugin.xml')
classes=[el.attrib.get('class','') for el in tree.findall('.//*[@class]')]
assert 'se.yrell.ai.ui.ExportPreferencesPage' in classes
assert 'se.yrell.ai.ui.PromptPreferencesPage' in classes
assert 'se.yrell.ai.ui.ContextPreferencesPage' in classes
assert not any('ModelPreferencesPage' in x for x in classes)
print('Plugin XML structure: PASS')
PYCODE
