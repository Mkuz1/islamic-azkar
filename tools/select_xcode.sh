#!/bin/bash
# Selects Xcode 27.1 (or a later 27.x) on the GitHub "xcode-27" runner, as the App Store requires
# for the iOS 27 SDK and the iPhone Duo; stops the build if it is not installed.
set -euo pipefail
echo "Installed:"; ls -d /Applications/Xcode*.app
XC=$(ls -d /Applications/Xcode_27.[1-9]*.app 2>/dev/null | grep -vi beta | sort -V | tail -1 || true)
[ -n "$XC" ] || { echo "::error::Xcode 27.1 or later is not installed on this runner"; exit 1; }
sudo xcode-select -s "$XC"
xcodebuild -version
xcodebuild -showsdks | grep -E "iOS|macOS" || true
