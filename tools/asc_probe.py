"""Read-only questions to the App Store Connect API (prints the answers).

    python3 tools/asc_probe.py screenshot-types   # the screenshot display types the live API accepts

Env: KEY_ID, ISSUER and ~/.appstoreconnect/private_keys/AuthKey_<KEY_ID>.p8.
"""
import json, os, sys, time, urllib.error, urllib.request
import jwt

KID, ISS = os.environ["KEY_ID"].strip(), os.environ["ISSUER"].strip()
KEY = open(os.path.expanduser(f"~/.appstoreconnect/private_keys/AuthKey_{KID}.p8")).read()


def call(path):
    tok = jwt.encode({"iss": ISS, "iat": int(time.time()), "exp": int(time.time()) + 600, "aud": "appstoreconnect-v1"},
                     KEY, algorithm="ES256", headers={"kid": KID, "typ": "JWT"})
    req = urllib.request.Request("https://api.appstoreconnect.apple.com/" + path, headers={"Authorization": "Bearer " + tok})
    try:
        with urllib.request.urlopen(req, timeout=60) as r: return r.status, json.loads(r.read() or b"{}")
    except urllib.error.HTTPError as e: return e.code, json.loads(e.read() or b"{}")


def screenshot_types():
    st, b = call("v1/apps?filter[bundleId]=com.koussa.IslamicAzkar")
    app = b["data"][0]["id"]
    st, b = call(f"v1/apps/{app}/appStoreVersions?filter[platform]=IOS&limit=5")
    ver = b["data"][0]
    print("iOS version", ver["attributes"]["versionString"], ver["attributes"].get("appVersionState"))
    st, b = call(f"v1/appStoreVersions/{ver['id']}/appStoreVersionLocalizations?limit=5")
    loc = b["data"][0]["id"]
    # An unknown value makes the API list the accepted ones.
    st, b = call(f"v1/appStoreVersionLocalizations/{loc}/appScreenshotSets?filter[screenshotDisplayType]=NOT_A_TYPE")
    print("status", st)
    for e in b.get("errors", []):
        print(e.get("detail"))
    st, b = call(f"v1/appStoreVersionLocalizations/{loc}/appScreenshotSets?limit=50")
    print("sets on this version:", [s["attributes"]["screenshotDisplayType"] for s in b.get("data", [])])


if __name__ == "__main__":
    {"screenshot-types": screenshot_types}[sys.argv[1]]()
