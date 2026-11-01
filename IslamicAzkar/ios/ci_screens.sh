#!/bin/bash
# Launches the app on a booted Simulator once per screen and saves a screenshot of each.
# The app reads -ciScreen / -ciLang / -ciDark launch arguments (see IslamicAzkarApp.swift, CI only).
set -uo pipefail
DEVICE="$1"; OUT="$2"; BUNDLE=com.koussa.IslamicAzkar

shot() { # name screen lang dark
  xcrun simctl terminate "$DEVICE" "$BUNDLE" >/dev/null 2>&1 || true
  xcrun simctl launch "$DEVICE" "$BUNDLE" -ciScreen "$2" -ciLang "$3" -ciDark "$4" >/dev/null
  sleep "${WAIT:-5}"
  xcrun simctl io "$DEVICE" screenshot "$OUT/$1.png" >/dev/null && echo "saved $1"
}

WAIT=9 shot 00-dedication dedication ar 1
shot 01-home-ar home ar 0
shot 02-home-en-dark home en 1
shot 03-reader-ar reader ar 0
shot 04-reader-fr-dark reader fr 1
shot 05-done-ar celebration ar 0
shot 06-tasbih-ar tasbih ar 0
shot 07-widgets-ar widgets ar 0
shot 08-widgets-en-dark widgets en 1
shot 09-settings-en settings en 0
shot 10-favorites-fr favorites fr 0
exit 0
