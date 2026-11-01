#!/usr/bin/env python3
"""Adds the duas listed in hadith_duas.py to the three copies of azkar.json.

Every Arabic text is cut word for word out of the cited hadith in the
fawazahmed0/hadith-api editions (downloaded from raw.githubusercontent.com into
--cache). The build stops if an anchor is not found, if a listed cross-reference
does not contain its phrase, or if a non-Bukhari/Muslim hadith is not graded
sahih or hasan by al-Albani. Re-running replaces the chapters it added earlier.

    python3 tools/build_hadith_duas.py [--cache DIR]
"""
import argparse, json, os, re, sys, urllib.request

sys.path.insert(0, os.path.dirname(__file__))
from hadith_duas import CATEGORY, CHAPTERS, DUAS  # noqa: E402
import quran_duas  # noqa: E402
import umrah_guide  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TARGETS = ["IslamicAzkar/app/src/main/assets/azkar.json", "IslamicAzkar/ios/Shared/azkar.json", "website/data/azkar.json"]
URL = "https://raw.githubusercontent.com/fawazahmed0/hadith-api/1/editions/ara-{}.min.json"
QURAN_URL = "https://raw.githubusercontent.com/fawazahmed0/quran-api/1/editions/ara-quransimple.min.json"
FIRST_ITEM_ID = 268  # item ids 1-267 belong to Hisn al-Muslim
BOOKS = {
    "bukhari": ("صحيح البخاري", "Sahih al-Bukhari", "Sahih al-Boukhari"),
    "muslim": ("صحيح مسلم", "Sahih Muslim", "Sahih Muslim"),
    "abudawud": ("سنن أبي داود", "Sunan Abi Dawud", "Sunan Abou Daoud"),
    "tirmidhi": ("جامع الترمذي", "Jamiʿ al-Tirmidhi", "Jamiʿ at-Tirmidhi"),
    "nasai": ("سنن النسائي", "Sunan al-Nasa'i", "Sunan an-Nasa'i"),
    "ibnmajah": ("سنن ابن ماجه", "Sunan Ibn Majah", "Sunan Ibn Majah"),
}
GRADE = {
    "Sahih": ("صححه الألباني", "sahih according to al-Albani", "authentique selon al-Albani"),
    "Hasan": ("حسّنه الألباني", "hasan according to al-Albani", "hasan selon al-Albani"),
}
# New categories, inserted before "praise" in this order.
NEW_CATEGORIES = ["quran", "sunnah"]

HARAKAT = re.compile("[ً-ٰٟۖ-ۭـ‎‏]")


def norm(s):
    s = HARAKAT.sub("", s)
    for a, b in [("أ", "ا"), ("إ", "ا"), ("آ", "ا"), ("ٱ", "ا"), ("ى", "ي"), ("ة", "ه"), ("ؤ", "و"), ("ئ", "ي")]:
        s = s.replace(a, b)
    s = re.sub("[^ء-ي ]", " ", s)
    return re.sub(" +", " ", s).strip()


def load(book, cache):
    path = os.path.join(cache, f"ara-{book}.json")
    if not os.path.exists(path):
        os.makedirs(cache, exist_ok=True)
        print("downloading", book)
        urllib.request.urlretrieve(URL.format(book), path)
    with open(path, encoding="utf-8") as f:
        return {h["hadithnumber"]: h for h in json.load(f)["hadiths"]}


def extract(text, start, end):
    words = text.split()
    nw = [norm(w) for w in words]
    s, e = start.split(), end.split()
    for i in range(len(words)):
        if not nw[i] or [x for x in nw[i:i + 2 * len(s)] if x][: len(s)] != s:
            continue
        for j in range(i, len(words)):
            seg = [x for x in nw[i:j + 1] if x]
            if seg[-len(e):] == e and len(seg) >= len(s):
                return " ".join(words[i:j + 1])
    return None


def clean(s):
    s = s.replace("‏", "").replace("‎", "").replace('"', "").replace("{", "﴿").replace("}", "﴾")
    s = s.replace("﴿ ", "﴿").replace(".", "")
    s = re.sub(r"\s+", " ", s).strip(" ،,")
    if "﴾" in s and "﴿" not in s:  # the verse opened before the start anchor
        s = "﴿" + s
    return s


def load_quran(cache):
    path = os.path.join(cache, "ara-quransimple.json")
    if not os.path.exists(path):
        os.makedirs(cache, exist_ok=True)
        print("downloading the Quran text")
        urllib.request.urlretrieve(QURAN_URL, path)
    with open(path, encoding="utf-8") as f:
        return {(v["chapter"], v["verse"]): v["text"] for v in json.load(f)["quran"]}


def quran_items(cache, next_id):
    """Quran duas: the verses' own text between the start and end words, with verse markers."""
    verses = load_quran(cache)
    chapters = {k: {"id": v[0], "ar": v[1], "en": v[2], "fr": v[3], "items": []} for k, v in quran_duas.CHAPTERS.items()}
    for key, surah, first, last, start, end, tr, en, fr in quran_duas.DUAS:
        text = " ".join(verses[(surah, a)].replace("۞", "") + f" ﴿{ar_digits(a)}﴾" for a in range(first, last + 1))
        ar = extract(text, start, end)
        if ar is None:
            sys.exit(f"Quran {surah}:{first}-{last}: anchors '{start}' … '{end}' not found")
        ar = re.sub(r"\s+", " ", ar).strip()
        if not ar.endswith("﴾"):
            ar += f" ﴿{ar_digits(last)}﴾"
        name_ar, name_lat = quran_duas.SURAHS[surah]
        ayat = f"{first}" if first == last else f"{first}–{last}"
        ref = {"ar": f"سورة {name_ar} {ar_digits(first) if first == last else ar_digits(first) + '–' + ar_digits(last)}",
               "en": f"Quran, {name_lat} {surah}:{ayat}", "fr": f"Coran, {name_lat} {surah}:{ayat}"}
        chapters[key]["items"].append({"id": next_id, "ar": ar, "tr": tr, "en": en, "fr": fr, "n": 1, "ref": ref})
        next_id += 1
    return chapters, next_id


def citation_number(book, h):
    # Sahih Muslim is cited by Fuad ʿAbd al-Baqi's number (the dataset's "arabicnumber").
    return int(float(h["arabicnumber"])) if book == "muslim" else h["hadithnumber"]


def ar_digits(n):
    return str(n).translate(str.maketrans("0123456789", "٠١٢٣٤٥٦٧٨٩"))


def albani_grade(book, h):
    grades = [g["grade"] for g in h.get("grades", []) if g["name"] == "Al-Albani"]
    grade = next((g for g in ("Sahih", "Hasan") if grades and grades[0].startswith(g)), None)
    if grade is None:
        sys.exit(f"{book} {h['hadithnumber']}: al-Albani grade is {grades}, not sahih/hasan")
    return grade


def labels(refs, grade=None):
    """{"ar","en","fr"} reference line, numbers grouped per book."""
    out = {}
    for li, lang in enumerate(("ar", "en", "fr")):
        by_book = {}
        for b, h in refs:
            n = citation_number(b, h)
            n = ar_digits(n) if lang == "ar" else str(n)
            if n not in by_book.setdefault(b, []):
                by_book[b].append(n)
        sep = "، " if lang == "ar" else ", "
        label = " · ".join(f"{BOOKS[b][li]} {sep.join(nums)}" for b, nums in by_book.items())
        if grade:
            label += f" ({GRADE[grade][li]})"
        out[lang] = label
    return out


def build_guide(hadith, chapters):
    steps = []
    for st in umrah_guide.STEPS:
        refs = []
        for b, n, phrase in st["refs"]:
            h = hadith(b, n)
            if norm(phrase) not in norm(h["text"]):
                sys.exit(f"guide: {b} {n} does not contain '{phrase}'")
            if b not in ("bukhari", "muslim"):
                albani_grade(b, h)
            refs.append((b, h))
        steps.append({"icon": st["icon"], "title": st["title"], "text": st["text"], "zikr": st["zikr"], "ref": labels(refs)})
    return {"key": "umrah", "title": umrah_guide.TITLE, "intro": umrah_guide.INTRO, "note": umrah_guide.NOTE,
            "chapter": chapters["umrah"]["id"], "steps": steps}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--cache", default=os.path.join(ROOT, ".hadith-cache"))
    args = ap.parse_args()
    books = {}

    def hadith(book, num):
        if book not in books:
            books[book] = load(book, args.cache)
        h = books[book].get(num)
        if h is None:
            sys.exit(f"{book} {num}: not in dataset")
        return h

    chapters = {k: {"id": v[0], "ar": v[1], "en": v[2], "fr": v[3], "items": []} for k, v in CHAPTERS.items()}
    next_id = FIRST_ITEM_ID
    for key, book, num, start, end, n, tr, en, fr, also in DUAS:
        h = hadith(book, num)
        ar = extract(h["text"], start, end)
        if ar is None:
            sys.exit(f"{book} {num}: anchors '{start}' … '{end}' not found")
        ar = clean(ar)
        refs = [(book, h)]
        grade = None if book in ("bukhari", "muslim") else albani_grade(book, h)
        for b2, n2, phrase in also:
            h2 = hadith(b2, n2)
            if norm(phrase) not in norm(h2["text"]):
                sys.exit(f"{b2} {n2}: does not contain '{phrase}'")
            refs.append((b2, h2))
        chapters[key]["items"].append({"id": next_id, "ar": ar, "tr": tr, "en": en, "fr": fr, "n": n, "ref": labels(refs, grade)})
        next_id += 1

    qchapters, next_id = quran_items(args.cache, next_id)
    guide = build_guide(hadith, chapters)
    category_of = {chapters[k]["id"]: CATEGORY.get(k, "sunnah") for k in chapters}
    category_of.update({c["id"]: "quran" for c in qchapters.values()})
    chapters.update({"q_" + k: v for k, v in qchapters.items()})
    new_ids = {c["id"] for c in chapters.values()}
    for rel in TARGETS:
        path = os.path.join(ROOT, rel)
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        data["chapters"] = [c for c in data["chapters"] if c["id"] not in new_ids] + sorted(chapters.values(), key=lambda c: c["id"])
        cats = [c for c in data["categories"] if c["key"] not in NEW_CATEGORIES]
        for c in cats:
            c["chapters"] = [i for i in c["chapters"] if i not in new_ids]
        at = next(i for i, c in enumerate(cats) if c["key"] == "praise")
        for k in reversed(NEW_CATEGORIES):
            cats.insert(at, {"key": k, "chapters": []})
        by_key = {c["key"]: c for c in cats}
        for cid, cat in sorted(category_of.items()):
            by_key[cat]["chapters"].append(cid)
        data["categories"] = cats
        data["guides"] = [guide]
        known = {i["id"] for c in data["chapters"] for i in c["items"]}
        missing = [z for st in guide["steps"] for z in st["zikr"] if z not in known]
        if missing:
            sys.exit(f"guide links to unknown dua ids {missing}")
        with open(path, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, separators=(",", ":"))
        print("wrote", rel)
    total = sum(len(c["items"]) for c in chapters.values())
    print(f"{total} duas in {len(chapters)} chapters")


if __name__ == "__main__":
    main()
