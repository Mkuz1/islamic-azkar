#!/bin/bash
# Launches the app on a booted Simulator once per screen and saves a screenshot of each.
# The app reads -ciScreen / -ciLang / -ciDark / -ciScroll launch arguments (CI only, see AppModel).
set -uo pipefail
DEVICE="$1"; OUT="$2"; BUNDLE=com.koussa.IslamicAzkar

shot() { # name screen lang dark [scroll]
  xcrun simctl terminate "$DEVICE" "$BUNDLE" >/dev/null 2>&1 || true
  xcrun simctl launch "$DEVICE" "$BUNDLE" -ciScreen "$2" -ciLang "$3" -ciDark "$4" -ciScroll "${5:-none}" >/dev/null
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
shot 11-w-clock-en widgets en 0 clock
shot 12-w-progress-ar widgets ar 1 progress
shot 13-w-timeline-en widgets en 0 timeline
shot 14-w-night-ar widgets ar 0 night
shot 15-w-quick-fr widgets fr 1 quick
shot 16-w-scripture-ar widgets ar 0 scripture
shot 17-w-counter-en widgets en 1 counter
shot 18-w-lock-ar widgets ar 0 lock
shot 19-w-style-en widgets en 0 style
shot 20-guide-en guide en 0
shot 21-guide-ar-dark guide ar 1
shot 22-sunnah-fr sunnah fr 0
shot 23-sunnah-ar-dark sunnah ar 1
shot 24-quran-ar quran ar 0
shot 25-quran-en-dark quran en 1
shot 26-record-en record en 0
shot 27-record-ar-dark record ar 1
shot 28-w-lock-en widgets en 1 lock
# Any crash during the run: what it was and where (the app's own frames).
for f in $(ls -t ~/Library/Logs/DiagnosticReports/IslamicAzkar*.ips 2>/dev/null | head -3); do
  echo "=== crash report $f"
  python3 - "$f" <<'PY' || head -c 4000 "$f"
import json, sys
raw = open(sys.argv[1]).read().split("\n", 1)
d = json.loads(raw[1])
print("exception:", d.get("exception"), "| termination:", d.get("termination", {}).get("indicator"))
for line in d.get("asi", {}).values(): print("asi:", line)
imgs = d.get("usedImages", [])
for t in d.get("threads", []):
    if t.get("triggered"):
        for fr in t.get("frames", [])[:40]:
            img = imgs[fr["imageIndex"]].get("name", "?") if fr.get("imageIndex") is not None and fr["imageIndex"] < len(imgs) else "?"
            print(f"  {img}: {fr.get('symbol', hex(fr.get('imageOffset', 0)))} {fr.get('sourceFile', '')}:{fr.get('sourceLine', '')}")
PY
done
exit 0
