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
  # Submit, then ask Apple every minute (the first submissions from a new Developer ID can take hours).
  local out id status
  out=$(xcrun notarytool submit "$1" --key "$P8" --key-id "$KEY_ID" --issuer "$ISSUER" --output-format json)
  echo "$out"
  id=$(echo "$out" | python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])')
  for i in $(seq 1 280); do
    status=$(xcrun notarytool info "$id" --key "$P8" --key-id "$KEY_ID" --issuer "$ISSUER" --output-format json 2>/dev/null \
      | python3 -c 'import json,sys; print(json.load(sys.stdin).get("status",""))' 2>/dev/null || true)
    [ "$status" = "In Progress" ] || [ -z "$status" ] || break
    [ $((i % 10)) = 0 ] && echo "   still with Apple after $i min"
    sleep 60
  done
  echo "notarization of $(basename "$1"): $status"
  if [ "$status" != "Accepted" ]; then
    xcrun notarytool log "$id" --key "$P8" --key-id "$KEY_ID" --issuer "$ISSUER" || true
    echo "::error::Apple did not notarize $1 ($status)"; exit 1
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
