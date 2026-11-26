"""App Store marketing screenshots for Islamic Azkar.

Reads the raw simulator screenshots (appstore-raw branch: <device>/<lang>-<screen>.png) and writes
store-ready images to store/appstore/screenshots/<device>/<lang>/NN-<screen>.png at each device's
exact App Store size: a deep green and gold backdrop with an eight-pointed-star pattern, a headline
and a sub-line, and the screen in a device frame.

    python3 tools/appstore_screens.py <raw-dir> <out-dir>
"""
import math
import os
import sys

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FONTS = os.path.join(ROOT, "IslamicAzkar", "app", "src", "main", "res", "font")

# (folder, output size, frame corner radius as a share of the screen width, bezel share)
DEVICES = {
    "iPhone-6.9": ((1320, 2868), 0.135, 0.022),
    "iPhone-6.3": ((1206, 2622), 0.135, 0.022),
    "iPad-13": ((2064, 2752), 0.045, 0.016),
    "iPhone-Duo-folded": ((1398, 2034), 0.10, 0.02),       # outer display (App Store "iPhone Duo")
    "iPhone-Duo-unfolded": ((2007, 2853), 0.06, 0.018),    # inner display
}

# Store order, screen, and the copy for each language: (headline, sub-line).
SLIDES = [
    ("home", {
        "en": ("Your daily azkar,\nbeautifully", "Morning, evening and every moment: the complete Hisn\u00a0al-Muslim"),
        "ar": ("أذكارك اليومية\nبحُلّة أنيقة", "الصباح والمساء وكل لحظة: حصن المسلم كاملًا"),
        "fr": ("Vos adhkar du jour,\nen beauté", "Matin, soir et à chaque instant : toute la Citadelle du musulman"),
    }),
    ("reader", {
        "en": ("Count every dhikr\nwith one tap", "A gentle counter, meaning and transliteration"),
        "ar": ("عُدّ كل ذكر\nبلمسة واحدة", "عدّاد لطيف مع المعنى والنطق"),
        "fr": ("Comptez chaque dhikr\nd'une seule touche", "Un compteur doux, la traduction et la translittération"),
    }),
    ("record", {
        "en": ("See every day you\nremembered Allah", "Your streak and calendar, synced across your devices"),
        "ar": ("كل يوم ذكرت فيه الله\nفي سجلّك", "الأيام المتتالية والتقويم، متزامنة بين أجهزتك"),
        "fr": ("Chaque jour de rappel\nd'Allah, en un coup d'œil", "Jours d'affilée et calendrier, synchronisés sur vos appareils"),
    }),
    ("widgets", {
        "en": ("Widgets in real\nArabic calligraphy", "Prayer times, Hijri date, verses and hadith on your Home Screen"),
        "ar": ("أدوات بالخط\nالعربي الأصيل", "مواقيت الصلاة والتاريخ الهجري والآيات على شاشتك"),
        "fr": ("Des widgets en vraie\ncalligraphie arabe", "Horaires de prière, date hégirienne, versets sur l'écran d'accueil"),
    }),
    ("tasbih", {
        "en": ("A tasbih that's\nalways with you", "33, 99, 100 or unlimited, with weekly statistics"),
        "ar": ("مسبحتك\nمعك دائمًا", "٣٣ أو ٩٩ أو ١٠٠ أو بلا حدّ، مع إحصاءات أسبوعية"),
        "fr": ("Un chapelet\ntoujours avec vous", "33, 99, 100 ou sans limite, avec statistiques"),
    }),
    ("celebration", {
        "en": ("A prayer for you\nevery time you finish", "Stay consistent, day after day"),
        "ar": ("دعاء لك\nكلما أتممت أذكارك", "واظب يومًا بعد يوم"),
        "fr": ("Une invocation pour vous\nà chaque fin d'adhkar", "Restez régulier, jour après jour"),
    }),
    ("guide", {
        "en": ("ʿUmrah,\nstep by step", "From Ihram to the end, with the duas of each step"),
        "ar": ("العمرة\nخطوة بخطوة", "من الإحرام إلى التحلّل، مع أدعية كل خطوة"),
        "fr": ("La ʿOumra,\npas à pas", "De l'Ihram à la fin, avec les douas de chaque étape"),
    }),
    ("dedication", {
        "en": ("Free forever.\nNo ads. No tracking.", "A sadaqa jariyah, made with love"),
        "ar": ("مجاني دائمًا\nبلا إعلانات ولا تتبّع", "صدقة جارية، صُنعت بمحبة"),
        "fr": ("Gratuit pour toujours.\nSans pub ni suivi.", "Une sadaqa jariya, faite avec amour"),
    }),
]

GOLD = (217, 182, 108)
GOLD_LIGHT = (240, 214, 150)
WHITE = (246, 244, 236)
TOP = (24, 72, 55)
BOTTOM = (6, 22, 16)


def font(lang, weight, size):
    if lang == "ar":
        name = "ibmplexsansarabic_semibold.ttf" if weight == "bold" else "ibmplexsansarabic_medium.ttf"
    else:
        name = "inter.ttf"
    f = ImageFont.truetype(os.path.join(FONTS, name), size)
    if lang != "ar":
        try:
            f.set_variation_by_axes([32, 800 if weight == "bold" else 450])   # optical size, weight
        except Exception:
            pass
    return f


def star(cx, cy, r, inner=0.62):
    pts = []
    for i in range(16):
        a = math.pi / 8 * i - math.pi / 2
        rr = r if i % 2 == 0 else r * inner
        pts.append((cx + rr * math.cos(a), cy + rr * math.sin(a)))
    return pts


def backdrop(w, h):
    img = Image.new("RGB", (w, h))
    px = img.load()
    for y in range(h):
        t = y / h
        row = tuple(int(TOP[i] * (1 - t) + BOTTOM[i] * t) for i in range(3))
        for x in range(w):
            px[x, y] = row
    # A soft gold glow behind the headline, and a green one behind the device.
    glow = Image.new("L", (w, h), 0)
    g = ImageDraw.Draw(glow)
    g.ellipse((w * 0.05, -h * 0.12, w * 0.95, h * 0.34), fill=120)
    glow = glow.filter(ImageFilter.GaussianBlur(w * 0.12))
    img = Image.composite(Image.new("RGB", (w, h), (70, 110, 70)), img, glow.point(lambda v: int(v * 0.55)))
    # Interlaced eight-pointed stars (the app's backdrop).
    pat = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(pat)
    step = int(w / 6.5)
    for yy in range(-step, h + step, step):
        for xx in range(-step, w + step, step):
            off = step // 2 if (yy // step) % 2 else 0
            d.polygon(star(xx + off, yy, step * 0.42), outline=GOLD + (22,), width=max(2, w // 600))
    img = Image.alpha_composite(img.convert("RGBA"), pat)
    return img


def device(screen, width, radius_share, bezel_share, ipad=False):
    """The screen in a dark device frame with a thin gold edge and a soft shadow."""
    sw = width
    sh = int(screen.height * sw / screen.width)
    scr = screen.convert("RGB").resize((sw, sh), Image.LANCZOS)
    r = int(sw * radius_share)
    b = int(sw * bezel_share)
    fw, fh = sw + 2 * b, sh + 2 * b
    pad = int(fw * 0.08)
    out = Image.new("RGBA", (fw + 2 * pad, fh + 2 * pad), (0, 0, 0, 0))
    shadow = Image.new("L", out.size, 0)
    ImageDraw.Draw(shadow).rounded_rectangle((pad, pad + pad // 3, pad + fw, pad + fh + pad // 3), radius=r + b, fill=170)
    shadow = shadow.filter(ImageFilter.GaussianBlur(pad // 2))
    out.paste((0, 0, 0, 255), (0, 0), shadow)
    frame = Image.new("RGBA", (fw, fh), (0, 0, 0, 0))
    fd = ImageDraw.Draw(frame)
    fd.rounded_rectangle((0, 0, fw - 1, fh - 1), radius=r + b, fill=(18, 20, 19, 255), outline=GOLD + (200,), width=max(3, b // 5))
    mask = Image.new("L", (sw, sh), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, sw - 1, sh - 1), radius=r, fill=255)
    frame.paste(scr, (b, b), mask)
    out.alpha_composite(frame, (pad, pad))
    return out, pad


def wrap(draw, text, f, max_w):
    lines = []
    for para in text.split("\n"):
        words, cur = para.split(" "), ""
        for wd in words:
            t = (cur + " " + wd).strip()
            if draw.textlength(t, font=f) <= max_w:
                cur = t
            else:
                if cur:
                    lines.append(cur)
                cur = wd
        lines.append(cur)
    return lines


def compose(raw, out, name, size, radius_share, bezel_share, lang, headline, sub):
    w, h = size
    img = backdrop(w, h)
    d = ImageDraw.Draw(img)
    rtl = lang == "ar"
    kw = {"direction": "rtl", "language": "ar"} if rtl else {}
    ipad = name.startswith("iPad")
    margin = int(w * 0.07)
    # Headline: as large as fits the width in its lines.
    size_h = int(w * (0.078 if not ipad else 0.06))
    fh = font(lang, "bold", size_h)
    while max(d.textlength(l, font=fh, **kw) for l in headline.split("\n")) > w - 2 * margin and size_h > 30:
        size_h -= 4
        fh = font(lang, "bold", size_h)
    y = int(h * 0.055)
    for i, line in enumerate(headline.split("\n")):
        lw = d.textlength(line, font=fh, **kw)
        d.text(((w - lw) / 2, y), line, font=fh, fill=WHITE if i == 0 else GOLD_LIGHT, **kw)
        y += int(size_h * (1.32 if rtl else 1.16))
    fs = font(lang, "regular", int(size_h * 0.42))
    y += int(size_h * 0.18)
    for line in wrap(d, sub, fs, w - 2 * margin):
        lw = d.textlength(line, font=fs, **kw)
        d.text(((w - lw) / 2, y), line, font=fs, fill=(205, 214, 206), **kw)
        y += int(size_h * 0.6)
    # The device: wide, starting under the text and running off the bottom edge.
    dev_w = int(w * (0.80 if not ipad else 0.82))
    frame, pad = device(raw, dev_w, radius_share, bezel_share, ipad)
    top = y + int(h * 0.035) - pad
    img.alpha_composite(frame, ((w - frame.width) // 2, top))
    img.convert("RGB").save(out, optimize=True)


def main(raw_dir, out_dir):
    made = 0
    for name, (size, radius, bezel) in DEVICES.items():
        for lang in ("en", "ar", "fr"):
            n = 0
            for screen, copy in SLIDES:
                src = os.path.join(raw_dir, name, f"{lang}-{screen}.png")
                if not os.path.exists(src):
                    continue
                n += 1
                dst = os.path.join(out_dir, name, lang, f"{n:02d}-{screen}.png")
                os.makedirs(os.path.dirname(dst), exist_ok=True)
                compose(Image.open(src), dst, name, size, radius, bezel, lang, *copy[lang])
                made += 1
    print(f"{made} screenshots")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
