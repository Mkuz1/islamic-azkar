#!/bin/bash
# Signs "Islamic Azkar.app" with Developer ID (hardened runtime), has Apple notarize it, and packages
# it into a signed, notarized DMG that opens without any warning.
# Usage: sign_mac.sh <path to .app> <output.dmg> <keychain>
# Env: KEY_ID, ISSUER (App Store Connect API key in ~/.appstoreconnect/private_keys/).
set -euo pipefail
APP="$1"; OUT="$2"; KC="$3"
HERE=$(cd "$(dirname "$0")" && pwd)
P8=~/.appstoreconnect/private_keys/AuthKey_$KEY_ID.p8
IDENT=$(security find-identity -v -p codesigning "$KC" | grep "Developer ID Application" | head -1 | awk '{print $2}')
[ -n "$IDENT" ] || { echo "::error::no Developer ID Application identity"; exit 1; }
security find-identity -v -p codesigning "$KC" | grep "Developer ID" | sed 's/[0-9A-F]\{40\}/<id>/'

notarize() {
  local out
  out=$(xcrun notarytool submit "$1" --key "$P8" --key-id "$KEY_ID" --issuer "$ISSUER" --wait --output-format json)
  echo "$out"
  if ! echo "$out" | grep -q '"status" *: *"Accepted"'; then
    id=$(echo "$out" | python3 -c 'import json,sys; print(json.load(sys.stdin).get("id",""))')
    [ -n "$id" ] && xcrun notarytool log "$id" --key "$P8" --key-id "$KEY_ID" --issuer "$ISSUER" || true
    echo "::error::Apple did not notarize $1"; exit 1
  fi
}

# Inside out: anything nested first, then the app with the hardened runtime and its entitlements.
find "$APP/Contents" \( -name "*.dylib" -o -name "*.framework" -o -name "*.appex" -o -name "*.xpc" \) -prune -print0 2>/dev/null |
  while IFS= read -r -d '' f; do codesign --force --options runtime --timestamp --keychain "$KC" --sign "$IDENT" "$f"; done
codesign --force --options runtime --timestamp --keychain "$KC" --sign "$IDENT" \
  --entitlements "$HERE/../IslamicAzkar/mac/DeveloperID.entitlements" "$APP"
codesign --verify --deep --strict --verbose=2 "$APP"

# Notarize the app and staple its ticket (so it also opens offline once copied out of the DMG).
ZIP=$(mktemp -d)/app.zip
ditto -c -k --keepParent "$APP" "$ZIP"
notarize "$ZIP"
xcrun stapler staple "$APP"

# The DMG: signed, notarized and stapled too.
"$HERE/../IslamicAzkar/mac/make_dmg.sh" "$APP" "$OUT"
codesign --force --timestamp --keychain "$KC" --sign "$IDENT" "$OUT"
notarize "$OUT"
xcrun stapler staple "$OUT"
spctl --assess --type open --context context:primary-signature -vv "$OUT"
spctl --assess --type execute -vv "$APP"
echo "Signed and notarized: $OUT"
