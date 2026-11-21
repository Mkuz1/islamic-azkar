"""App Store signing through the App Store Connect API (no Xcode account sign-in needed).

    python3 tools/appstore_signing.py setup  <workdir>   # certificate + App Store profiles, keychain
    python3 tools/appstore_signing.py cleanup <workdir>  # delete the profiles, revoke the certificate

Env: KEY_ID, ISSUER, TEAM and ~/.appstoreconnect/private_keys/AuthKey_<KEY_ID>.p8.
PLATFORM=mac signs the Mac App Store build instead: a Mac app and a Mac installer certificate,
and a Mac App Store profile. PLATFORM=tv: the Apple TV app (tvOS App Store profile).
The iPhone and Apple TV certificate and profiles are made for one upload and removed afterwards (the
App Store re-signs those apps). The Mac App Store ones are kept: App Review checks a Mac build's
signature again when it is submitted, and a build signed with a revoked certificate is marked
"Invalid Binary". They are made once and stored encrypted in store/macstore/signing.enc (only the
App Store Connect API key, i.e. GitHub Actions, can open it), and made again only when expired.
"""
import base64, json, os, plistlib, re, subprocess, sys, time, urllib.error, urllib.request
import jwt

API = "https://api.appstoreconnect.apple.com/v1/"
PLATFORM = os.environ.get("PLATFORM", "").strip().lower()
MAC, TV = PLATFORM == "mac", PLATFORM == "tv"
BUNDLES = ({"com.koussa.IslamicAzkar": "Islamic Azkar Mac App Store"} if MAC else
           {"com.koussa.IslamicAzkar": "Islamic Azkar tvOS App Store"} if TV else
           {"com.koussa.IslamicAzkar": "Islamic Azkar App Store", "com.koussa.IslamicAzkar.Widgets": "Islamic Azkar Widgets App Store"})
# (certificate type, file name): the Mac App Store also needs an installer certificate for the .pkg.
CERTS = [("DISTRIBUTION", "dist"), ("MAC_INSTALLER_DISTRIBUTION", "inst")] if MAC else [("DISTRIBUTION", "dist")]
PROFILE_TYPE = "MAC_APP_STORE" if MAC else "TVOS_APP_STORE" if TV else "IOS_APP_STORE"
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


SEALED = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "store", "macstore", "signing.enc")


def _box():
    import hashlib
    from cryptography.hazmat.primitives.ciphers.aead import AESGCM
    return AESGCM(hashlib.sha256(KEY.encode() + b"islamic-azkar mac store signing").digest())


def kept():
    """The kept Mac App Store certificates and profile, if all still valid; else None."""
    if not (MAC and os.path.exists(SEALED)):
        return None
    box = json.load(open(SEALED))
    data = json.loads(_box().decrypt(base64.b64decode(box["nonce"]), base64.b64decode(box["ct"]), None))
    for c in data["certificates"]:
        st, b = call("GET", f"certificates/{c['id']}")
        exp = (b.get("data") or {}).get("attributes", {}).get("expirationDate", "")
        if st >= 300 or exp[:10] <= time.strftime("%Y-%m-%d", time.gmtime(time.time() + 7 * 86400)):
            print("kept certificate", c["id"], "is gone or expires soon: making new ones")
            return None
    for ident, pr in data["profiles"].items():
        st, b = call("GET", f"profiles/{pr['id']}")
        if st >= 300 or (b.get("data") or {}).get("attributes", {}).get("profileState") != "ACTIVE":
            print("kept profile for", ident, "is gone: making new ones")
            return None
    return data


def setup(work):
    os.makedirs(work, exist_ok=True)
    state = {}
    reuse = kept()
    if reuse:
        return setup_kept(work, reuse)
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
    names, uuids, raws = {}, {}, {}
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
        uuids[ident], raws[ident] = uuid, raw
        for d in dirs:
            open(os.path.join(d, f"{uuid}{PROFILE_EXT}"), "wb").write(raw)
        # The profile's entitlements (App Groups must be enabled on the identifier).
        xml = raw[raw.find(b"<?xml"):raw.rfind(b"</plist>") + 8]
        ent = plistlib.loads(xml).get("Entitlements", {})
        groups = ent.get("com.apple.security.application-groups")
        print(f"profile {name}: uuid {uuid}, app groups {groups}")
        if not groups and not (MAC or TV):
            print(f"::warning::{ident} has no App Groups in its profile: enable App Groups on it at developer.apple.com and add group.com.koussa.IslamicAzkar")
        names[ident] = name
    json.dump(state, open(f"{work}/state.json", "w"))
    json.dump(names, open(f"{work}/profiles.json", "w"))
    if MAC:   # keep them (see the top of this file)
        data = {"certificates": [{"id": cid, "file": f, "p12": base64.b64encode(open(f"{work}/{f}.p12", "rb").read()).decode()}
                                 for cid, (_, f) in zip(state["certificates"], CERTS)],
                "profiles": {ident: {"id": pid, "name": names[ident], "uuid": uuids[ident],
                                     "raw": base64.b64encode(raws[ident]).decode()}
                             for ident, pid in zip(BUNDLES, state["profiles"])}}
        nonce = os.urandom(12)
        os.makedirs(os.path.dirname(SEALED), exist_ok=True)
        json.dump({"nonce": base64.b64encode(nonce).decode(),
                   "ct": base64.b64encode(_box().encrypt(nonce, json.dumps(data).encode(), None)).decode()}, open(SEALED, "w"))
        state["keep"] = True
        json.dump(state, open(f"{work}/state.json", "w"))
        print("NEW-SIGNING kept in store/macstore/signing.enc")


def keychain(work, p12s):
    kc = f"{work}/signing.keychain-db"
    run("security", "create-keychain", "-p", "ci", kc)
    run("security", "set-keychain-settings", "-lut", "21600", kc)
    run("security", "unlock-keychain", "-p", "ci", kc)
    for f, raw in p12s:
        open(f"{work}/{f}.p12", "wb").write(raw)
        run("security", "import", f"{work}/{f}.p12", "-k", kc, "-P", "ci", "-T", "/usr/bin/codesign", "-T", "/usr/bin/security",
            "-T", "/usr/bin/productbuild")
    run("security", "set-key-partition-list", "-S", "apple-tool:,apple:,codesign:", "-s", "-k", "ci", kc, stdout=subprocess.DEVNULL)
    existing = subprocess.run(["security", "list-keychains", "-d", "user"], capture_output=True, text=True).stdout.split()
    run("security", "list-keychains", "-d", "user", "-s", kc, *[k.strip('"') for k in existing])


def setup_kept(work, data):
    print("Using the kept Mac App Store certificates:", ", ".join(c["id"] for c in data["certificates"]))
    keychain(work, [(c["file"], base64.b64decode(c["p12"])) for c in data["certificates"]])
    dirs = [os.path.expanduser("~/Library/MobileDevice/Provisioning Profiles"),
            os.path.expanduser("~/Library/Developer/Xcode/UserData/Provisioning Profiles")]
    names = {}
    for ident, pr in data["profiles"].items():
        for d in dirs:
            os.makedirs(d, exist_ok=True)
            open(os.path.join(d, f"{pr['uuid']}{PROFILE_EXT}"), "wb").write(base64.b64decode(pr["raw"]))
        names[ident] = pr["name"]
        print(f"profile {pr['name']}: uuid {pr['uuid']}")
    json.dump({"keep": True}, open(f"{work}/state.json", "w"))
    json.dump(names, open(f"{work}/profiles.json", "w"))


def cleanup(work):
    try:
        state = json.load(open(f"{work}/state.json"))
    except Exception:
        return
    if state.get("keep"):
        print("Mac App Store certificates and profile kept (store/macstore/signing.enc)")
        return
    for pid in state.get("profiles", []):
        print("delete profile", pid, call("DELETE", f"profiles/{pid}")[0])
    for cid in state.get("certificates") or ([state["certificate"]] if state.get("certificate") else []):
        print("revoke certificate", call("DELETE", f"certificates/{cid}")[0])


if __name__ == "__main__":
    {"setup": setup, "cleanup": cleanup}[sys.argv[1]](sys.argv[2])
