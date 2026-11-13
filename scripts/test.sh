#!/usr/bin/env bash
set -o pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
bash "$ROOT/scripts/test-core.sh" || exit 1
JAR="$ROOT/install/se.yrell.aiassistant_0.2.0.jar"
if [[ ! -f "$JAR" ]]; then echo 'Build the plug-in before running full tests.' >&2;exit 2; fi
python3 - "$JAR" <<'PYCODE'
import sys,zipfile
with zipfile.ZipFile(sys.argv[1]) as z:
  for n in ['META-INF/MANIFEST.MF','plugin.xml','se/yrell/ai/ui/AIView.class',
    'se/yrell/ai/ui/PromptPreferencesPage.class','se/yrell/ai/ui/ExportPreferencesPage.class',
    'se/yrell/ai/bmc/ContextCollector.class','se/yrell/ai/core/TaskExporter.class']:
    assert n in z.namelist(),n
print('Compiled Eclipse plugin/JAR structure: PASS')
PYCODE
