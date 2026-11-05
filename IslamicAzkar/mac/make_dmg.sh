#!/bin/bash
# Packages "Islamic Azkar.app" into IslamicAzkar.dmg (drag to Applications).
# Usage: make_dmg.sh <path to .app> <output.dmg>
set -euo pipefail
APP="$1"; OUT="$2"
STAGE=$(mktemp -d)
cp -R "$APP" "$STAGE/"
ln -s /Applications "$STAGE/Applications"
rm -f "$OUT"
hdiutil create -volname "Islamic Azkar" -srcfolder "$STAGE" -ov -format UDZO "$OUT"
rm -rf "$STAGE"
