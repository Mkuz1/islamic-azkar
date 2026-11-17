"""App Store signing through the App Store Connect API (no Xcode account sign-in needed).

    python3 tools/appstore_signing.py setup  <workdir>   # certificate + App Store profiles, keychain
    python3 tools/appstore_signing.py cleanup <workdir>  # delete the profiles, revoke the certificate

Env: KEY_ID, ISSUER, TEAM and ~/.appstoreconnect/private_keys/AuthKey_<KEY_ID>.p8.
PLATFORM=mac signs the Mac App Store build instead: a Mac app and a Mac installer certificate,
and a Mac App Store profile.
The certificate and profiles are made for this one upload and removed afterwards; an app already
uploaded stays valid (the App Store re-signs it).
"""
import base64, json, os, plistlib, re, subprocess, sys, time, urllib.error, urllib.request
import jwt

API = "https://api.appstoreconnect.apple.com/v1/"
MAC = os.environ.get("PLATFORM", "").strip().lower() == "mac"
BUNDLES = ({"com.koussa.IslamicAzkar": "Islamic Azkar Mac App Store"} if MAC else
           {"com.koussa.IslamicAzkar": "Islamic Azkar App Store", "com.koussa.IslamicAzkar.Widgets": "Islamic Azkar Widgets App Store"})
# (certificate type, file name): the Mac App Store also needs an installer certificate for the .pkg.
CERTS = [("DISTRIBUTION", "dist"), ("MAC_INSTALLER_DISTRIBUTION", "inst")] if MAC else [("DISTRIBUTION", "dist")]
PROFILE_TYPE = "MAC_APP_STORE" if MAC else "IOS_APP_STORE"
PROFILE_EXT = ".provisionprofile" if MAC else ".mobileprovision"
KID, ISS = os.environ["KEY_ID"].strip(), os.environ["ISSUER"].strip()
KEY = open(os.path.expanduser(f"~/.appstoreconnect/private_keys/AuthKey_{os.environ['KEY_ID']}.p8")).read()


def call(method, path, body=None):
    tok = jwt.encode({"iss": ISS, "iat": int(time.time()), "exp": int(time.time()) + 900, "aud": "appstoreconnect-v1"},
                     KEY, algorithm="ES256", headers={"kid": KID, "typ": "JWT"})
    req = urllib.request.Request(API + path, method=method, data=json.dumps(body).encode() if body else None,
                                 headers={"Authorization": "Bearer " + tok, "Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            raw = r.read()
            return r.status, json.loads(raw) if raw else {}
    except urllib.error.HTTPError as e:
        raw = e.read()
        return e.code, json.loads(raw) if raw else {}


def need(st, body, what):
    if st >= 300:
        for err in body.get("errors", []):
            print(f"   {err.get('title')}: {err.get('detail')}")
        sys.exit(f"::error::{what} failed ({st})")


def run(*cmd, **kw):
    subprocess.run(cmd, check=True, **kw)


def setup(work):
    os.makedirs(work, exist_ok=True)
    state = {}
    # 1. Distribution certificate(s), each from a fresh key, in a keychain of their own.
    kc = f"{work}/signing.keychain-db"
    run("security", "create-keychain", "-p", "ci", kc)
    run("security", "set-keychain-settings", "-lut", "21600", kc)
    run("security", "unlock-keychain", "-p", "ci", kc)
    state["certificates"] = []
    for ctype, f in CERTS:
        run("openssl", "genrsa", "-out", f"{work}/{f}.key", "2048")
        run("openssl", "req", "-new", "-key", f"{work}/{f}.key", "-out", f"{work}/{f}.csr", "-subj", "/CN=Islamic Azkar CI/O=CI/C=US")
        csr = open(f"{work}/{f}.csr").read()
        st, b = call("POST", "certificates", {"data": {"type": "certificates", "attributes": {"certificateType": ctype, "csrContent": csr}}})
        need(st, b, f"Creating the {ctype} certificate")
        cert = b["data"]
        state["certificates"].append(cert["id"])
        open(f"{work}/{f}.cer", "wb").write(base64.b64decode(cert["attributes"]["certificateContent"]))
        print("certificate:", cert["attributes"].get("name"), cert["attributes"].get("serialNumber"))
        run("openssl", "x509", "-inform", "DER", "-in", f"{work}/{f}.cer", "-out", f"{work}/{f}.pem")
        run("openssl", "pkcs12", "-export", "-legacy", "-inkey", f"{work}/{f}.key", "-in", f"{work}/{f}.pem",
            "-out", f"{work}/{f}.p12", "-passout", "pass:ci")
        run("security", "import", f"{work}/{f}.p12", "-k", kc, "-P", "ci", "-T", "/usr/bin/codesign", "-T", "/usr/bin/security",
            "-T", "/usr/bin/productbuild")
    state["certificate"] = state["certificates"][0]   # the one the profiles are made for
    run("security", "set-key-partition-list", "-S", "apple-tool:,apple:,codesign:", "-s", "-k", "ci", kc, stdout=subprocess.DEVNULL)
    existing = subprocess.run(["security", "list-keychains", "-d", "user"], capture_output=True, text=True).stdout.split()
    run("security", "list-keychains", "-d", "user", "-s", kc, *[k.strip('"') for k in existing])
    # 3. An App Store profile for each bundle ID.
    state["profiles"] = []
    dirs = [os.path.expanduser("~/Library/MobileDevice/Provisioning Profiles"),
            os.path.expanduser("~/Library/Developer/Xcode/UserData/Provisioning Profiles")]
    for d in dirs:
        os.makedirs(d, exist_ok=True)
    stamp = time.strftime("%Y%m%d%H%M%S")
    names = {}
    for ident, base in BUNDLES.items():
        st, b = call("GET", f"bundleIds?filter[identifier]={ident}&limit=20")
        need(st, b, f"Looking up {ident}")
        bid = next((d["id"] for d in b.get("data", []) if d["attributes"]["identifier"] == ident), None)
        if not bid:
            sys.exit(f"::error::{ident} is not registered under this team (developer.apple.com → Identifiers)")
        name = f"{base} {stamp}"
        st, b = call("POST", "profiles", {"data": {"type": "profiles", "attributes": {"name": name, "profileType": PROFILE_TYPE},
                     "relationships": {"bundleId": {"data": {"type": "bundleIds", "id": bid}},
                                       "certificates": {"data": [{"type": "certificates", "id": state["certificate"]}]}}}})
        need(st, b, f"Creating the App Store profile for {ident}")
        p = b["data"]
        state["profiles"].append(p["id"])
        raw = base64.b64decode(p["attributes"]["profileContent"])
        uuid = p["attributes"]["uuid"]
        for d in dirs:
            open(os.path.join(d, f"{uuid}{PROFILE_EXT}"), "wb").write(raw)
        # The profile's entitlements (App Groups must be enabled on the identifier).
        xml = raw[raw.find(b"<?xml"):raw.rfind(b"</plist>") + 8]
        ent = plistlib.loads(xml).get("Entitlements", {})
        groups = ent.get("com.apple.security.application-groups")
        print(f"profile {name}: uuid {uuid}, app groups {groups}")
        if not groups and not MAC:
            print(f"::warning::{ident} has no App Groups in its profile: enable App Groups on it at developer.apple.com and add group.com.koussa.IslamicAzkar")
        names[ident] = name
    json.dump(state, open(f"{work}/state.json", "w"))
    json.dump(names, open(f"{work}/profiles.json", "w"))


def cleanup(work):
    try:
        state = json.load(open(f"{work}/state.json"))
    except Exception:
        return
    for pid in state.get("profiles", []):
        print("delete profile", pid, call("DELETE", f"profiles/{pid}")[0])
    for cid in state.get("certificates") or ([state["certificate"]] if state.get("certificate") else []):
        print("revoke certificate", call("DELETE", f"certificates/{cid}")[0])


if __name__ == "__main__":
    {"setup": setup, "cleanup": cleanup}[sys.argv[1]](sys.argv[2])
