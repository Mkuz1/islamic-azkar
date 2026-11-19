"""Developer ID signing for the Mac app published on GitHub (outside the Mac App Store).

    python3 tools/developer_id.py setup <workdir>   # puts the Developer ID certificate in a keychain

Env: KEY_ID, ISSUER and ~/.appstoreconnect/private_keys/AuthKey_<KEY_ID>.p8 (the App Store Connect
API key). Apple lets only the Account Holder make a "Developer ID Application" certificate, so the
first run makes its private key (kept encrypted in store/devid/devid-key.enc) and a request
(store/devid/IslamicAzkar.certSigningRequest) for the Account Holder to upload at
developer.apple.com. The next run finds the certificate through the API and keeps it encrypted in
store/devid/devid.enc; later runs reuse it. The repository is public: the files are encrypted with
a key derived from the App Store Connect API key, so only its holder (GitHub Actions) can open them.

The certificate is never revoked: macOS would then refuse to open copies already downloaded.
"""
import base64, hashlib, json, os, subprocess, sys, time, urllib.error, urllib.request
import jwt
from cryptography.hazmat.primitives.ciphers.aead import AESGCM

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SEALED = os.path.join(ROOT, "store", "devid", "devid.enc")
API = "https://api.appstoreconnect.apple.com/v1/"
KID, ISS = os.environ["KEY_ID"].strip(), os.environ["ISSUER"].strip()
KEY = open(os.path.expanduser(f"~/.appstoreconnect/private_keys/AuthKey_{KID}.p8")).read()
BOX = AESGCM(hashlib.sha256(KEY.encode() + b"islamic-azkar developer id").digest())


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


def run(*cmd, **kw):
    subprocess.run(cmd, check=True, **kw)


KEY_SEALED = os.path.join(ROOT, "store", "devid", "devid-key.enc")
CSR_FILE = os.path.join(ROOT, "store", "devid", "IslamicAzkar.certSigningRequest")


def seal(path, data, **extra):
    nonce = os.urandom(12)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    json.dump({"nonce": base64.b64encode(nonce).decode(), "ct": base64.b64encode(BOX.encrypt(nonce, data, None)).decode(), **extra},
              open(path, "w"), indent=1)


def unseal(path):
    box = json.load(open(path))
    return BOX.decrypt(base64.b64decode(box["nonce"]), base64.b64decode(box["ct"]), None), box


def create(work):
    """The Developer ID certificate as .p12 bytes (password "ci"), or None while waiting for the Account Holder.

    Apple lets only the Account Holder make a Developer ID certificate (not an API key). So the first run
    makes a private key (kept encrypted) and its certificate request; once the Account Holder has
    uploaded that request at developer.apple.com, the next run finds the certificate through the API."""
    if not os.path.exists(KEY_SEALED):
        run("openssl", "genrsa", "-out", f"{work}/devid.key", "2048")
        run("openssl", "req", "-new", "-key", f"{work}/devid.key", "-out", CSR_FILE,
            "-subj", "/emailAddress=ci@islamicazkar.net/CN=Islamic Azkar/C=US")
        seal(KEY_SEALED, open(f"{work}/devid.key", "rb").read())
        os.remove(f"{work}/devid.key")
        print("NEW-REQUEST store/devid/IslamicAzkar.certSigningRequest")
    key, _ = unseal(KEY_SEALED)
    open(f"{work}/devid.key", "wb").write(key)
    mine = subprocess.run(["openssl", "rsa", "-in", f"{work}/devid.key", "-pubout", "-outform", "DER"], capture_output=True, check=True).stdout
    found = None
    for ctype in ("DEVELOPER_ID_APPLICATION_G2", "DEVELOPER_ID_APPLICATION"):
        st, b = call("GET", f"certificates?filter[certificateType]={ctype}&limit=200")
        for c in b.get("data", []):
            der = base64.b64decode(c["attributes"]["certificateContent"])
            open(f"{work}/c.cer", "wb").write(der)
            pub = subprocess.run(["sh", "-c", f"openssl x509 -inform DER -in {work}/c.cer -pubkey -noout | openssl pkey -pubin -outform DER"],
                                 capture_output=True).stdout
            if pub == mine:
                found = c; break
        if found:
            break
    if not found:
        print("::error::Waiting for the Developer ID certificate. Account Holder: developer.apple.com → Certificates → + → "
              "Developer ID Application (G2 Sub-CA) → upload store/devid/IslamicAzkar.certSigningRequest from the repository. "
              "Then run this workflow again; nothing needs to be downloaded.")
        os.remove(f"{work}/devid.key")
        return None
    cert = found["attributes"]
    print("found:", cert.get("name"), cert.get("serialNumber"), "expires", cert.get("expirationDate"))
    run("openssl", "x509", "-inform", "DER", "-in", f"{work}/c.cer", "-out", f"{work}/devid.pem")
    run("openssl", "pkcs12", "-export", "-legacy", "-inkey", f"{work}/devid.key", "-in", f"{work}/devid.pem",
        "-out", f"{work}/devid.p12", "-passout", "pass:ci")
    os.remove(f"{work}/devid.key")
    p12 = open(f"{work}/devid.p12", "rb").read()
    seal(SEALED, p12, serial=cert.get("serialNumber"), expires=cert.get("expirationDate"))
    print("NEW-CERTIFICATE saved to store/devid/devid.enc")
    return p12


def setup(work):
    os.makedirs(work, exist_ok=True)
    if os.path.exists(SEALED):
        p12, box = unseal(SEALED)
        print("Developer ID certificate", box.get("serial"), "expires", box.get("expires"))
    else:
        p12 = create(work)
        if p12 is None:
            sys.exit(2)
    open(f"{work}/devid.p12", "wb").write(p12)
    kc = f"{work}/devid.keychain-db"
    run("security", "create-keychain", "-p", "ci", kc)
    run("security", "set-keychain-settings", "-lut", "21600", kc)
    run("security", "unlock-keychain", "-p", "ci", kc)
    run("security", "import", f"{work}/devid.p12", "-k", kc, "-P", "ci", "-T", "/usr/bin/codesign")
    run("security", "set-key-partition-list", "-S", "apple-tool:,apple:,codesign:", "-s", "-k", "ci", kc, stdout=subprocess.DEVNULL)
    existing = subprocess.run(["security", "list-keychains", "-d", "user"], capture_output=True, text=True).stdout.split()
    run("security", "list-keychains", "-d", "user", "-s", kc, *[k.strip('"') for k in existing])
    os.remove(f"{work}/devid.p12")


if __name__ == "__main__":
    {"setup": setup}[sys.argv[1]](sys.argv[2])
