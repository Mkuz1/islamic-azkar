"""Fills in the App Store listing through the App Store Connect API and submits the app for review.

    python3 tools/appstore_submit.py [--no-submit]

Env: KEY_ID, ISSUER (and REVIEW_PHONE, optional) and ~/.appstoreconnect/private_keys/AuthKey_<KEY_ID>.p8.
Reads store/appstore/listing.json and store/appstore/screenshots/. Every step is safe to run again:
it updates what is there and replaces the screenshots. What Apple's API cannot set (the App Privacy
answers) is listed at the end.
"""
import hashlib, json, os, sys, time, urllib.error, urllib.request
import jwt

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
# PLATFORM=mac submits the Mac version (texts, screenshots and notes from store/macstore).
# PLATFORM=tv: the Apple TV version (store/tvstore).
MAC = os.environ.get("PLATFORM", "").strip().lower() == "mac"
TV = os.environ.get("PLATFORM", "").strip().lower() == "tv"
APPSTORE_DIR = os.path.join(ROOT, "store", "appstore")
STORE = os.path.join(ROOT, "store", "macstore" if MAC else "tvstore" if TV else "appstore")
PLATFORM = "MAC_OS" if MAC else "TV_OS" if TV else "IOS"
API = "https://api.appstoreconnect.apple.com/"
BUNDLE = "com.koussa.IslamicAzkar"
def _project_version():
    """The version the app's project builds (what the uploaded build carries)."""
    path = os.path.join(ROOT, "IslamicAzkar", *(("mac", "project.yml") if MAC else ("tv", "project.yml") if TV
                                                 else ("ios", "IslamicAzkar.xcodeproj", "project.pbxproj")))
    import re
    return re.search(r'MARKETING_VERSION *[:=] *"?([0-9.]+)', open(path).read()).group(1)


VERSION = os.environ.get("VERSION", "").strip() or _project_version()
BUILD = os.environ.get("BUILD", "").strip()   # e.g. "19.5"; empty = newest valid build of VERSION
COPYRIGHT = "2026 Mkoussa"
SITE = "https://islamicazkar.net"
PRIVACY = "https://islamicazkar.net/privacy/"
SHOTS = {"Mac": "APP_DESKTOP"} if MAC else {"TV": "APP_APPLE_TV"} if TV else {"iPhone-6.9": "APP_IPHONE_67", "iPhone-6.3": "APP_IPHONE_61", "iPad-13": "APP_IPAD_PRO_3GEN_129"}
LANG = {"en-US": "en", "ar-SA": "ar", "fr-FR": "fr"}
NOTES = open(os.path.join(STORE, "review-notes.txt"), encoding="utf-8").read().strip()   # answers to App Review

# The privacy policy as text (https://islamicazkar.net/privacy/ in short), for Apple TV.
PRIVACY_TEXT = {
    "en-US": "Islamic Azkar collects no personal data. There is no account, no advertising, no analytics and no tracking. "
             "Your settings, favourites and azkar record stay on your device. Prayer times are calculated on the device; "
             "your location, if you allow it, is never sent anywhere. Only if you turn on \"Sync between devices\", the dates and "
             "names of the azkar you complete are stored on our server (islamicazkar.net, hosted by Cloudflare) under an anonymous "
             "code, to show them on your other devices. You can stop syncing at any time and delete this data at "
             "islamicazkar.net/delete. Contact: through the App Store page of the app.",
    "ar-SA": "لا يجمع تطبيق «أذكار إسلامية» أي بيانات شخصية: لا حساب ولا إعلانات ولا إحصاءات ولا تتبّع. تبقى إعداداتك ومفضلتك "
             "وسجلّ أذكارك على جهازك. تُحسب مواقيت الصلاة على الجهاز، ولا يُرسل موقعك إلى أي مكان. فقط إن فعّلت «المزامنة بين "
             "الأجهزة» تُحفظ تواريخ الأذكار التي أتممتها وأسماؤها على خادمنا (islamicazkar.net على Cloudflare) تحت رمز مجهول، "
             "لعرضها على أجهزتك الأخرى. يمكنك إيقاف المزامنة متى شئت وحذف هذه البيانات من islamicazkar.net/delete.",
    "fr-FR": "Islamic Azkar ne collecte aucune donnée personnelle : pas de compte, pas de publicité, pas de statistiques, pas de "
             "suivi. Vos réglages, favoris et votre historique restent sur votre appareil. Les horaires de prière sont calculés sur "
             "l'appareil ; votre position n'est jamais envoyée. Seulement si vous activez la « Synchronisation entre appareils », "
             "les dates et les noms des adhkar terminés sont enregistrés sur notre serveur (islamicazkar.net, hébergé par Cloudflare) "
             "sous un code anonyme, pour les afficher sur vos autres appareils. Vous pouvez arrêter la synchronisation à tout moment "
             "et supprimer ces données sur islamicazkar.net/delete.",
}

KID, ISS = os.environ["KEY_ID"].strip(), os.environ["ISSUER"].strip()
KEY = open(os.path.expanduser(f"~/.appstoreconnect/private_keys/AuthKey_{KID}.p8")).read()
problems = []


def call(method, path, body=None, quiet=False):
    tok = jwt.encode({"iss": ISS, "iat": int(time.time()), "exp": int(time.time()) + 900, "aud": "appstoreconnect-v1"},
                     KEY, algorithm="ES256", headers={"kid": KID, "typ": "JWT"})
    url = path if path.startswith("http") else API + (path if path.startswith("v") else "v1/" + path)
    req = urllib.request.Request(url, method=method, data=json.dumps(body).encode() if body is not None else None,
                                 headers={"Authorization": "Bearer " + tok, "Content-Type": "application/json"})
    for attempt in range(4):
        try:
            with urllib.request.urlopen(req, timeout=120) as r:
                raw = r.read()
                return r.status, json.loads(raw) if raw else {}
        except urllib.error.HTTPError as e:
            raw = e.read()
            b = json.loads(raw) if raw else {}
            if e.code in (429, 500, 502, 503) and attempt < 3:
                time.sleep(5 * (attempt + 1)); continue
            if not quiet and e.code >= 400:
                for err in b.get("errors", []):
                    print(f"     {err.get('title')}: {err.get('detail')}"
                          + (f" [{err['source'].get('pointer') or err['source'].get('parameter')}]" if err.get("source") else ""))
                    for group in (err.get("meta") or {}).get("associatedErrors", {}).values():
                        for a in group:
                            print(f"       → {a.get('title') or a.get('code')}: {a.get('detail')}")
            return e.code, b
        except urllib.error.URLError:
            if attempt == 3: raise
            time.sleep(5)


def step(name, ok, extra=""):
    print(("  ✓ " if ok else "  ✗ ") + name + (f" — {extra}" if extra else ""))
    if not ok:
        problems.append(name)
    return ok


def patch_dropping(path, typ, oid, attrs, rels=None):
    """PATCH, leaving out attributes this API version doesn't know (the age rating questions change)."""
    attrs = dict(attrs)
    for _ in range(12):
        data = {"type": typ, "id": oid, "attributes": attrs}
        if rels: data["relationships"] = rels
        st, b = call("PATCH", path, {"data": data}, quiet=True)
        if st < 300:
            return st, b
        bad = [e["source"]["pointer"].rsplit("/", 1)[-1] for e in b.get("errors", [])
               if e.get("source", {}).get("pointer", "").startswith("/data/attributes/")]
        bad = [a for a in bad if a in attrs]
        # questions Apple added since: answer "no"
        import re
        missing = [m for e in b.get("errors", []) for m in re.findall(r"attribute '(\w+)'", e.get("detail", ""))]
        missing = [m for m in missing if m not in attrs]
        for m in missing:
            attrs[m] = False
        if not bad and not missing:
            for err in b.get("errors", []):
                print(f"     {err.get('title')}: {err.get('detail')}")
            return st, b
        for a in bad:
            attrs.pop(a)
    return st, b


def main(submit):
    listing = json.load(open(os.path.join(STORE, "listing.json"), encoding="utf-8"))

    st, b = call("GET", f"apps?filter[bundleId]={BUNDLE}")
    if st >= 300 or not b.get("data"):
        sys.exit(f"::error::No app with bundle ID {BUNDLE} in App Store Connect ({st}).")
    app = b["data"][0]["id"]
    print(f"App: {b['data'][0]['attributes']['name']} ({app})")

    print("\n== App information")
    st, _ = call("PATCH", f"apps/{app}", {"data": {"type": "apps", "id": app, "attributes": {
        "contentRightsDeclaration": "DOES_NOT_USE_THIRD_PARTY_CONTENT"}}})
    step("Content rights: no third-party content", st < 300)

    st, b = call("GET", f"apps/{app}/appInfos")
    infos = b.get("data", [])
    editable = [i for i in infos if i["attributes"].get("appStoreState") in (None, "PREPARE_FOR_SUBMISSION", "DEVELOPER_REJECTED", "REJECTED", "METADATA_REJECTED")
                or i["attributes"].get("state") in ("PREPARE_FOR_SUBMISSION", "DEVELOPER_REJECTED", "REJECTED")]
    info = (editable or infos)[0]["id"]
    st, _ = call("PATCH", f"appInfos/{info}", {"data": {"type": "appInfos", "id": info, "relationships": {
        "primaryCategory": {"data": {"type": "appCategories", "id": "LIFESTYLE"}},
        "secondaryCategory": {"data": {"type": "appCategories", "id": "REFERENCE"}}}}})
    step("Category: Lifestyle (secondary: Reference)", st < 300)

    st, b = call("GET", f"appInfos/{info}/appInfoLocalizations?limit=50")
    have = {l["attributes"]["locale"]: l["id"] for l in b.get("data", [])}
    for loc, t in listing.items():
        attrs = {"name": t["name"], "subtitle": t["subtitle"], "privacyPolicyUrl": PRIVACY}
        if loc in PRIVACY_TEXT:   # Apple TV can't open links: the policy as text (asked for tvOS)
            attrs["privacyPolicyText"] = PRIVACY_TEXT[loc]
        if loc in have:
            st, _ = call("PATCH", f"appInfoLocalizations/{have[loc]}", {"data": {"type": "appInfoLocalizations", "id": have[loc], "attributes": attrs}})
        else:
            st, _ = call("POST", "appInfoLocalizations", {"data": {"type": "appInfoLocalizations", "attributes": {"locale": loc, **attrs},
                         "relationships": {"appInfo": {"data": {"type": "appInfos", "id": info}}}}})
        step(f"Name, subtitle, privacy policy ({loc})", st < 300)

    st, b = call("GET", f"appInfos/{info}/ageRatingDeclaration")
    if st < 300 and b.get("data"):
        none = "NONE"
        attrs = {k: none for k in (
            "alcoholTobaccoOrDrugUseOrReferences", "contests", "gamblingSimulated", "gunsOrOtherWeapons", "horrorOrFearThemes",
            "matureOrSuggestiveThemes", "medicalOrTreatmentInformation", "profanityOrCrudeHumor", "sexualContentGraphicAndNudity",
            "sexualContentOrNudity", "violenceCartoonOrFantasy", "violenceRealistic", "violenceRealisticProlongedGraphicOrSadistic")}
        attrs.update({k: False for k in (
            "gambling", "unrestrictedWebAccess", "lootBox", "messagingAndChat", "parentalControls", "ageAssurance",
            "userGeneratedContent", "healthOrWellnessTopics", "advertising", "socialMedia")})
        st, _ = patch_dropping(f"ageRatingDeclarations/{b['data']['id']}", "ageRatingDeclarations", b["data"]["id"], attrs)
        step("Age rating: none of the content → 4+", st < 300)
    else:
        step("Age rating", False, "not found")

    print("\n== Price and availability")
    st, b = call("GET", f"apps/{app}/appPriceSchedule/manualPrices?limit=5", quiet=True)
    if st < 300 and b.get("data"):
        step("Price already set", True)
    else:   # (every app has an empty schedule until a price is chosen)
        st, b = call("GET", f"apps/{app}/appPricePoints?filter[territory]=USA&limit=200")
        free = next((p["id"] for p in b.get("data", []) if float(p["attributes"].get("customerPrice") or 0) == 0), None)
        if free:
            st, _ = call("POST", "appPriceSchedules", {"data": {"type": "appPriceSchedules", "relationships": {
                "app": {"data": {"type": "apps", "id": app}},
                "baseTerritory": {"data": {"type": "territories", "id": "USA"}},
                "manualPrices": {"data": [{"type": "appPrices", "id": "${free}"}]}}},
                "included": [{"type": "appPrices", "id": "${free}", "attributes": {"startDate": None},
                              "relationships": {"appPricePoint": {"data": {"type": "appPricePoints", "id": free}}}}]})
            step("Price: Free", st < 300)
        else:
            step("Price: Free", False, "no free price point found")
    st, b = call("GET", f"apps/{app}/appAvailabilityV2", quiet=True)
    if st < 300 and b.get("data"):
        step("Availability already set", True)
    else:
        terrs, url = [], "territories?limit=200"
        while url:
            st, b = call("GET", url)
            terrs += [t["id"] for t in b.get("data", [])]
            url = b.get("links", {}).get("next")
        st, _ = call("POST", "v2/appAvailabilities", {"data": {"type": "appAvailabilities", "attributes": {"availableInNewTerritories": True},
            "relationships": {"app": {"data": {"type": "apps", "id": app}},
                              "territoryAvailabilities": {"data": [{"type": "territoryAvailabilities", "id": f"${{{t}}}"} for t in terrs]}}},
            "included": [{"type": "territoryAvailabilities", "id": f"${{{t}}}", "attributes": {"available": True},
                          "relationships": {"territory": {"data": {"type": "territories", "id": t}}}} for t in terrs]})
        step(f"Available in all {len(terrs)} countries and regions", st < 300)

    if submit and REPLACE:
        replace_pending(app)

    print(f"\n== Version {VERSION}")
    st, b = call("GET", f"apps/{app}/appStoreVersions?filter[platform]={PLATFORM}&limit=20")
    versions = b.get("data", [])
    # An update (a version is already on the App Store) needs "What's New" text.
    live = any((v["attributes"].get("appStoreState") or "") in ("READY_FOR_SALE", "READY_FOR_DISTRIBUTION")
               or (v["attributes"].get("appVersionState") or "") == "READY_FOR_DISTRIBUTION" for v in versions)
    open_states = ("PREPARE_FOR_SUBMISSION", "DEVELOPER_REJECTED", "REJECTED", "METADATA_REJECTED", "INVALID_BINARY")
    ver = next((v for v in versions if (v["attributes"].get("appVersionState") or v["attributes"].get("appStoreState")) in open_states
                or v["attributes"].get("appStoreState") in open_states), None)
    if not ver:
        st, b = call("POST", "appStoreVersions", {"data": {"type": "appStoreVersions", "attributes": {"platform": PLATFORM, "versionString": VERSION},
                     "relationships": {"app": {"data": {"type": "apps", "id": app}}}}})
        if not step("Create version", st < 300):
            sys.exit(1)
        ver = b["data"]
    vid = ver["id"]
    st, _ = call("PATCH", f"appStoreVersions/{vid}", {"data": {"type": "appStoreVersions", "id": vid, "attributes": {
        "versionString": VERSION, "copyright": COPYRIGHT, "releaseType": "AFTER_APPROVAL"}}})
    step(f"Version {VERSION}, © {COPYRIGHT}, released once approved", st < 300)

    st, b = call("GET", f"appStoreVersions/{vid}/appStoreVersionLocalizations?limit=50")
    vlocs = {l["attributes"]["locale"]: l["id"] for l in b.get("data", [])}
    for loc, t in listing.items():
        attrs = {"description": t["description"], "keywords": t["keywords"], "promotionalText": t["promotional_text"],
                 "supportUrl": SITE, "marketingUrl": SITE}
        if live and WHATS_NEW.get(LANG.get(loc)):
            attrs["whatsNew"] = WHATS_NEW[LANG[loc]]
        if loc in vlocs:
            st, _ = call("PATCH", f"appStoreVersionLocalizations/{vlocs[loc]}", {"data": {"type": "appStoreVersionLocalizations", "id": vlocs[loc], "attributes": attrs}})
        else:
            st, b = call("POST", "appStoreVersionLocalizations", {"data": {"type": "appStoreVersionLocalizations", "attributes": {"locale": loc, **attrs},
                         "relationships": {"appStoreVersion": {"data": {"type": "appStoreVersions", "id": vid}}}}})
            if st < 300: vlocs[loc] = b["data"]["id"]
        step(f"Description, keywords, promotional text, URLs ({loc})", st < 300)

    print("\n== Screenshots")
    for loc, lid in vlocs.items():
        lang = LANG.get(loc)
        if not lang:
            continue
        st, b = call("GET", f"appStoreVersionLocalizations/{lid}/appScreenshotSets?limit=50")
        sets = {s["attributes"]["screenshotDisplayType"]: s["id"] for s in b.get("data", [])}
        for folder, kind in SHOTS.items():
            d = os.path.join(STORE, "screenshots", folder, lang)
            files = sorted(f for f in os.listdir(d) if f.lower().endswith((".jpg", ".png"))) if os.path.isdir(d) else []
            if not files:
                continue
            sid = sets.get(kind)
            if not sid:
                st, b = call("POST", "appScreenshotSets", {"data": {"type": "appScreenshotSets", "attributes": {"screenshotDisplayType": kind},
                             "relationships": {"appStoreVersionLocalization": {"data": {"type": "appStoreVersionLocalizations", "id": lid}}}}})
                if not step(f"{loc} {folder} set", st < 300):
                    continue
                sid = b["data"]["id"]
            else:   # start clean so a re-run doesn't add duplicates (unless they are the same files)
                st, b = call("GET", f"appScreenshotSets/{sid}/appScreenshots?limit=50")
                there = [x["attributes"].get("sourceFileChecksum") for x in b.get("data", [])]
                local = [hashlib.md5(open(os.path.join(d, f), "rb").read()).hexdigest() for f in files[:10]]
                if there == local:
                    step(f"{loc} {folder}: {len(local)} screenshots already up to date", True)
                    continue
                for s in b.get("data", []):
                    call("DELETE", f"appScreenshots/{s['id']}")
            ok, ids = 0, []
            for f in files[:10]:
                ids.append(upload_shot(sid, os.path.join(d, f)))
                ok += ids[-1] is not None
            ids = [i for i in ids if i]
            if ids:
                call("PATCH", f"appScreenshotSets/{sid}/relationships/appScreenshots",
                     {"data": [{"type": "appScreenshots", "id": i} for i in ids]}, quiet=True)
            step(f"{loc} {folder}: {ok}/{len(files[:10])} screenshots", ok == len(files[:10]))

    print("\n== Build")
    q = f"builds?filter[app]={app}&filter[preReleaseVersion.version]={VERSION}&filter[preReleaseVersion.platform]={PLATFORM}&sort=-uploadedDate&limit=20"
    if BUILD: q += f"&filter[version]={BUILD}"
    build = None
    for _ in range(30):   # a fresh upload can still be processing
        st, b = call("GET", q)
        valid = [x for x in b.get("data", []) if x["attributes"].get("processingState") == "VALID"]
        if valid:
            build = valid[0]; break
        print("   waiting for the build to finish processing…"); time.sleep(60)
    if build:
        bid = build["id"]
        if build["attributes"].get("usesNonExemptEncryption") is None:
            call("PATCH", f"builds/{bid}", {"data": {"type": "builds", "id": bid, "attributes": {"usesNonExemptEncryption": False}}}, quiet=True)
        st, _ = call("PATCH", f"appStoreVersions/{vid}/relationships/build", {"data": {"type": "builds", "id": bid}})
        step(f"Build {build['attributes']['version']} attached", st < 300)
    else:
        step("Build", False, f"no processed build of {VERSION}")

    print("\n== App Review information")
    phone = os.environ.get("REVIEW_PHONE", "").strip() or sealed_phone()
    st, b = call("GET", "users?filter[roles]=ACCOUNT_HOLDER&limit=5", quiet=True)
    holder = (b.get("data") or [{}])[0].get("attributes", {}) if st < 300 else {}
    attrs = {"demoAccountRequired": False, "notes": NOTES,
             "contactFirstName": holder.get("firstName"), "contactLastName": holder.get("lastName"),
             "contactEmail": holder.get("username"), "contactPhone": phone or None}
    attrs = {k: v for k, v in attrs.items() if v is not None}
    st, b = call("GET", f"appStoreVersions/{vid}/appStoreReviewDetail", quiet=True)
    if st < 300 and b.get("data"):
        st, _ = call("PATCH", f"appStoreReviewDetails/{b['data']['id']}", {"data": {"type": "appStoreReviewDetails", "id": b["data"]["id"], "attributes": attrs}})
    else:
        st, _ = call("POST", "appStoreReviewDetails", {"data": {"type": "appStoreReviewDetails", "attributes": attrs,
                     "relationships": {"appStoreVersion": {"data": {"type": "appStoreVersions", "id": vid}}}}})
    step("Review notes, no sign-in needed, contact: the account holder" + ("" if phone else " (no phone number yet)"), st < 300)

    if not submit:
        print("\nNot submitting (--no-submit).")
        return report(False)
    print("\n== Submit for review")
    st, b = call("GET", f"reviewSubmissions?filter[app]={app}&filter[platform]={PLATFORM}&filter[state]=READY_FOR_REVIEW,WAITING_FOR_REVIEW,IN_REVIEW,UNRESOLVED_ISSUES")
    subs = b.get("data", [])
    if any(s["attributes"]["state"] in ("WAITING_FOR_REVIEW", "IN_REVIEW") for s in subs):
        step("Already waiting for review", True)
        return report(True)
    sub = next((s["id"] for s in subs if s["attributes"]["state"] in ("READY_FOR_REVIEW", "UNRESOLVED_ISSUES")), None)   # (a reply to App Review reuses its submission)
    unresolved = any(s["id"] == sub and s["attributes"]["state"] == "UNRESOLVED_ISSUES" for s in subs)
    if unresolved:
        # App Review asked for something: mark its items resolved, then submit the same submission again.
        st, b = call("GET", f"reviewSubmissions/{sub}/items")
        for it in b.get("data", []):
            st, _ = call("PATCH", f"reviewSubmissionItems/{it['id']}", {"data": {"type": "reviewSubmissionItems", "id": it["id"], "attributes": {"resolved": True}}})
            step(f"Mark App Review's request as answered ({it['attributes'].get('state')})", st < 300)
        st, b = call("PATCH", f"reviewSubmissions/{sub}", {"data": {"type": "reviewSubmissions", "id": sub, "attributes": {"submitted": True}}})
        return report(step("Resubmitted for App Review", st < 300))
    if not sub:
        st, b = call("POST", "reviewSubmissions", {"data": {"type": "reviewSubmissions", "attributes": {"platform": PLATFORM},
                     "relationships": {"app": {"data": {"type": "apps", "id": app}}}}})
        if not step("Create the review submission", st < 300):
            return report(False)
        sub = b["data"]["id"]
    st, b = call("GET", f"reviewSubmissions/{sub}/items")
    if not any((i.get("relationships", {}).get("appStoreVersion", {}).get("data") or {}).get("id") == vid for i in b.get("data", [])):
        st, _ = call("POST", "reviewSubmissionItems", {"data": {"type": "reviewSubmissionItems", "relationships": {
            "reviewSubmission": {"data": {"type": "reviewSubmissions", "id": sub}},
            "appStoreVersion": {"data": {"type": "appStoreVersions", "id": vid}}}}})
        if not step(f"Add version {VERSION} to the submission", st < 300):
            return report(False)
    st, b = call("PATCH", f"reviewSubmissions/{sub}", {"data": {"type": "reviewSubmissions", "id": sub, "attributes": {"submitted": True}}})
    done = step("Submitted for App Review", st < 300)
    return report(done)


def _whats_new():
    """RELEASE_NOTES.md: an English, an Arabic and a French block, separated by blank lines."""
    try:
        blocks = [b.strip() for b in open(os.path.join(ROOT, "RELEASE_NOTES.md"), encoding="utf-8").read().split("\n\n") if b.strip()]
    except OSError:
        return {}
    return dict(zip(("en", "ar", "fr"), blocks)) if len(blocks) == 3 else {}


WHATS_NEW = _whats_new()
REPLACE = "--replace" in sys.argv


def replace_pending(app):
    """Withdraw a submission still waiting for (or in) review, so the newer build can take its place."""
    print("\n== Withdraw the older submission")
    st, b = call("GET", f"reviewSubmissions?filter[app]={app}&filter[platform]={PLATFORM}&filter[state]=WAITING_FOR_REVIEW,IN_REVIEW")
    subs = b.get("data", [])
    if not subs:
        step("Nothing waiting for review", True)
        return
    for s in subs:
        st, _ = call("PATCH", f"reviewSubmissions/{s['id']}", {"data": {"type": "reviewSubmissions", "id": s["id"], "attributes": {"canceled": True}}})
        step(f"Cancel the submission {s['attributes']['state'].lower().replace('_', ' ')}", st < 300)
        for _ in range(40):   # canceling takes a moment
            st, b = call("GET", f"reviewSubmissions/{s['id']}")
            state = b.get("data", {}).get("attributes", {}).get("state")
            if state not in ("CANCELING", "WAITING_FOR_REVIEW", "IN_REVIEW"):
                print(f"   now {state}")
                break
            time.sleep(15)
    for _ in range(40):   # and the version goes back to "Developer Rejected"
        st, b = call("GET", f"apps/{app}/appStoreVersions?filter[platform]={PLATFORM}&limit=20")
        if not any((v["attributes"].get("appStoreState") or "") in ("WAITING_FOR_REVIEW", "IN_REVIEW") for v in b.get("data", [])):
            step("Older version withdrawn from review", True)
            return
        time.sleep(15)
    step("Older version withdrawn from review", False, "still in review after 10 minutes")


def upload_shot(set_id, path):
    data = open(path, "rb").read()
    st, b = call("POST", "appScreenshots", {"data": {"type": "appScreenshots",
                 "attributes": {"fileName": os.path.basename(path), "fileSize": len(data)},
                 "relationships": {"appScreenshotSet": {"data": {"type": "appScreenshotSets", "id": set_id}}}}})
    if st >= 300:
        return None
    sid = b["data"]["id"]
    for op in b["data"]["attributes"]["uploadOperations"]:
        chunk = data[op["offset"]: op["offset"] + op["length"]]
        req = urllib.request.Request(op["url"], method=op["method"], data=chunk,
                                     headers={h["name"]: h["value"] for h in op.get("requestHeaders", [])})
        with urllib.request.urlopen(req, timeout=120) as r:
            r.read()
    st, _ = call("PATCH", f"appScreenshots/{sid}", {"data": {"type": "appScreenshots", "id": sid, "attributes": {
        "uploaded": True, "sourceFileChecksum": hashlib.md5(data).hexdigest()}}})
    if st >= 300:
        return None
    for _ in range(30):
        st, b = call("GET", f"appScreenshots/{sid}")
        state = (b.get("data", {}).get("attributes", {}).get("assetDeliveryState") or {})
        if state.get("state") == "COMPLETE":
            return sid
        if state.get("state") == "FAILED":
            for e in state.get("errors") or []:
                print(f"     {os.path.basename(path)}: {e.get('description') or e.get('code')}")
            return None
        time.sleep(3)
    return sid


def report(submitted):
    print("\n== Summary")
    print("Submitted for review." if submitted else "NOT submitted yet.")
    if problems:
        print("Needs attention: " + "; ".join(problems))
    print("Set by hand in App Store Connect (Apple's API can't): App Privacy → Data collection → "
          "Other User Content, App Functionality, not linked to the user, no tracking → Publish.")
    if not submitted:
        sys.exit(1)


def phone_key():
    """A key pair only the holder of the App Store API key can re-create (the repository is public)."""
    from cryptography.hazmat.primitives.asymmetric.x25519 import X25519PrivateKey
    return X25519PrivateKey.from_private_bytes(hashlib.sha256(KEY.encode() + b"islamic-azkar review phone").digest())


def sealed_phone():
    """The review contact number, stored encrypted in store/appstore/review-phone.enc."""
    path = os.path.join(APPSTORE_DIR, "review-phone.enc")
    if not os.path.exists(path):
        return ""
    import base64
    from cryptography.hazmat.primitives.asymmetric.x25519 import X25519PublicKey
    from cryptography.hazmat.primitives.ciphers.aead import AESGCM
    box = json.load(open(path))
    shared = phone_key().exchange(X25519PublicKey.from_public_bytes(base64.b64decode(box["epk"])))
    phone = AESGCM(hashlib.sha256(shared).digest()).decrypt(base64.b64decode(box["nonce"]), base64.b64decode(box["ct"]), None).decode()
    print(f"::add-mask::{phone}")
    return phone


if __name__ == "__main__":
    if "--pubkey" in sys.argv:
        from cryptography.hazmat.primitives import serialization
        raw = phone_key().public_key().public_bytes(serialization.Encoding.Raw, serialization.PublicFormat.Raw)
        print("PHONE-PUBKEY " + raw.hex())
        sys.exit(0)
    main("--no-submit" not in sys.argv)
