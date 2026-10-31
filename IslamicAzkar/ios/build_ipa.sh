#!/bin/bash
# Builds IslamicAzkar.ipa for iPhone/iPad. Needs Xcode (App Store) signed into your
# Apple ID (Xcode → Settings → Accounts). Signing and provisioning are automatic.
set -euo pipefail
cd "$(dirname "$0")"

if [[ "$(xcode-select -p)" == *CommandLineTools* ]]; then
  XCODE=$(ls -d /Applications/Xcode*.app 2>/dev/null | head -1 || true)
  [[ -z "$XCODE" ]] && { echo "Xcode is not installed. Install it from the App Store, then run this again."; exit 1; }
  export DEVELOPER_DIR="$XCODE/Contents/Developer"
fi

rm -rf build
xcodebuild -project IslamicAzkar.xcodeproj -scheme IslamicAzkar -configuration Release \
  -destination 'generic/platform=iOS' -archivePath build/IslamicAzkar.xcarchive \
  -allowProvisioningUpdates archive

xcodebuild -exportArchive -archivePath build/IslamicAzkar.xcarchive \
  -exportOptionsPlist ExportOptions.plist -exportPath build/ipa -allowProvisioningUpdates

cp build/ipa/IslamicAzkar.ipa ../IslamicAzkar.ipa
echo "✅ IPA ready: $(cd .. && pwd)/IslamicAzkar.ipa"
