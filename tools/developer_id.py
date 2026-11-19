"""Developer ID signing for the Mac app published on GitHub (outside the Mac App Store).

    python3 tools/developer_id.py setup <workdir>   # puts the Developer ID certificate in a keychain

Env: KEY_ID, ISSUER and ~/.appstoreconnect/private_keys/AuthKey_<KEY_ID>.p8 (the App Store Connect
API key). The first run creates a "Developer ID Application" certificate through the API and keeps
its private key encrypted in store/devid/devid.enc (the repository is public: only the holder of
the API key can decrypt it, and that key can already make certificates). Later runs reuse it.

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


def create(work):
    """A new Developer ID Application certificate; returns the .p12 bytes (password "ci")."""
    run("openssl", "genrsa", "-out", f"{work}/devid.key", "2048")
    run("openssl", "req", "-new", "-key", f"{work}/devid.key", "-out", f"{work}/devid.csr", "-subj", "/CN=Islamic Azkar/O=Islamic Azkar/C=US")
    csr = open(f"{work}/devid.csr").read()
    for ctype in ("DEVELOPER_ID_APPLICATION_G2", "DEVELOPER_ID_APPLICATION"):
        st, b = call("POST", "certificates", {"data": {"type": "certificates", "attributes": {"certificateType": ctype, "csrContent": csr}}})
        if st < 300:
            break
        for err in b.get("errors", []):
            print(f"   {ctype}: {err.get('title')}: {err.get('detail')}")
    else:
        sys.exit("::error::Apple refused to make a Developer ID certificate with this API key. Only the Account Holder can: "
                 "create one at developer.apple.com → Certificates → + → Developer ID Application, or give the API key the Admin role.")
    cert = b["data"]["attributes"]
    print("created:", cert.get("name"), cert.get("serialNumber"), "expires", cert.get("expirationDate"))
    open(f"{work}/devid.cer", "wb").write(base64.b64decode(cert["certificateContent"]))
    run("openssl", "x509", "-inform", "DER", "-in", f"{work}/devid.cer", "-out", f"{work}/devid.pem")
    run("openssl", "pkcs12", "-export", "-legacy", "-inkey", f"{work}/devid.key", "-in", f"{work}/devid.pem",
        "-out", f"{work}/devid.p12", "-passout", "pass:ci")
    p12 = open(f"{work}/devid.p12", "rb").read()
    nonce = os.urandom(12)
    os.makedirs(os.path.dirname(SEALED), exist_ok=True)
    json.dump({"nonce": base64.b64encode(nonce).decode(), "ct": base64.b64encode(BOX.encrypt(nonce, p12, None)).decode(),
               "serial": cert.get("serialNumber"), "expires": cert.get("expirationDate")}, open(SEALED, "w"), indent=1)
    for f in ("devid.key", "devid.csr"):
        os.remove(f"{work}/{f}")
    print("NEW-CERTIFICATE saved to store/devid/devid.enc")
    return p12


def setup(work):
    os.makedirs(work, exist_ok=True)
    if os.path.exists(SEALED):
        box = json.load(open(SEALED))
        p12 = BOX.decrypt(base64.b64decode(box["nonce"]), base64.b64decode(box["ct"]), None)
        print("Developer ID certificate", box.get("serial"), "expires", box.get("expires"))
    else:
        p12 = create(work)
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
