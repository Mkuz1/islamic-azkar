"""Mac App Store screenshots for Islamic Azkar (2880 x 1800), in the iPhone set's style.

Reads the Retina window captures (macstore-raw branch: <lang>-<screen>.png) and writes
store/macstore/screenshots/Mac/<lang>/NN-<screen>.jpg: the green and gold backdrop with stars,
a headline and a sub-line, and the app window with macOS's rounded corners and shadow. The menu
bar panel and the reminder card are shown over the Home window.

    python3 tools/macstore_screens.py <raw-dir> <out-dir>
"""
import os
import sys

from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from appstore_screens import GOLD, GOLD_LIGHT, WHITE, backdrop, font, wrap  # noqa: E402

W, H = 2880, 1800

SLIDES = [
    ("home", {
        "en": ("Your daily azkar,\non your Mac", "The complete Hisn al-Muslim, calm and beautiful"),
        "ar": ("أذكارك اليومية\nعلى جهاز الماك", "حصن المسلم كاملًا بتصميم هادئ وجميل"),
        "fr": ("Vos adhkar du jour,\nsur votre Mac", "Toute la Citadelle du musulman, dans un design apaisant"),
    }),
    ("menu", {
        "en": ("Prayer times\nin your menu bar", "The next prayer and a live countdown, one click away"),
        "ar": ("مواقيت الصلاة\nفي شريط القوائم", "الصلاة القادمة والعدّ التنازلي على بُعد نقرة"),
        "fr": ("Les horaires de prière\ndans la barre des menus", "La prochaine prière et un compte à rebours, à un clic"),
    }),
    ("banner", {
        "en": ("A gentle reminder,\nright on your screen", "Morning and evening azkar, even while you work"),
        "ar": ("تذكير لطيف\nأمام عينيك", "أذكار الصباح والمساء حتى أثناء عملك"),
        "fr": ("Un doux rappel,\nsous vos yeux", "Les adhkar du matin et du soir, même pendant le travail"),
    }),
    ("reader", {
        "en": ("Count every dhikr\nwith one click", "With the meaning and the transliteration"),
        "ar": ("عُدّ كل ذكر\nبنقرة واحدة", "مع المعنى والنطق"),
        "fr": ("Comptez chaque dhikr\nd'un simple clic", "Avec la traduction et la translittération"),
    }),
    ("record", {
        "en": ("See every day you\nremembered Allah", "Your streak and calendar, synced with your iPhone"),
        "ar": ("كل يوم ذكرت فيه الله\nفي سجلّك", "الأيام المتتالية والتقويم، متزامنة مع الآيفون"),
        "fr": ("Chaque jour de rappel\nd'Allah, en un coup d'œil", "Jours d'affilée et calendrier, synchronisés avec l'iPhone"),
    }),
    ("tasbih", {
        "en": ("A tasbih that's\nalways with you", "33, 99, 100 or unlimited, with weekly statistics"),
        "ar": ("مسبحتك\nمعك دائمًا", "٣٣ أو ٩٩ أو ١٠٠ أو بلا حدّ، مع إحصاءات أسبوعية"),
        "fr": ("Un chapelet\ntoujours avec vous", "33, 99, 100 ou sans limite, avec statistiques"),
    }),
    ("guide", {
        "en": ("ʿUmrah,\nstep by step", "From Ihram to the end, with the duas of each step"),
        "ar": ("العمرة\nخطوة بخطوة", "من الإحرام إلى التحلل، مع أدعية كل خطوة"),
        "fr": ("La Omra,\nétape par étape", "De l'ihram à la fin, avec les invocations de chaque étape"),
    }),
]


def window(img, width, radius=24):
    """A capture as a macOS window: rounded corners, a hairline edge and a deep soft shadow."""
    h = int(img.height * width / img.width)
    win = img.convert("RGB").resize((width, h), Image.LANCZOS)
    r = int(radius * width / 1280)
    pad = int(width * 0.06)
    out = Image.new("RGBA", (width + 2 * pad, h + 2 * pad), (0, 0, 0, 0))
    sh = Image.new("L", out.size, 0)
    ImageDraw.Draw(sh).rounded_rectangle((pad, pad + pad // 3, pad + width, pad + h + pad // 3), radius=r, fill=165)
    out.paste((0, 0, 0, 255), (0, 0), sh.filter(ImageFilter.GaussianBlur(pad // 2)))
    mask = Image.new("L", (width, h), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, width - 1, h - 1), radius=r, fill=255)
    out.paste(win, (pad, pad), mask)
    ImageDraw.Draw(out).rounded_rectangle((pad, pad, pad + width - 1, pad + h - 1), radius=r, outline=GOLD + (150,), width=3)
    return out, pad


def compose(raw_dir, lang, screen, headline, sub, out):
    img = backdrop(W, H)
    d = ImageDraw.Draw(img)
    rtl = lang == "ar"
    kw = {"direction": "rtl", "language": "ar"} if rtl else {}
    margin = int(W * 0.08)
    size_h = 120
    fh = font(lang, "bold", size_h)
    while max(d.textlength(l, font=fh, **kw) for l in headline.split("\n")) > W - 2 * margin and size_h > 40:
        size_h -= 4
        fh = font(lang, "bold", size_h)
    y = int(H * 0.05)
    for i, line in enumerate(headline.split("\n")):
        lw = d.textlength(line, font=fh, **kw)
        d.text(((W - lw) / 2, y), line, font=fh, fill=WHITE if i == 0 else GOLD_LIGHT, **kw)
        y += int(size_h * (1.3 if rtl else 1.15))
    fs = font(lang, "regular", int(size_h * 0.42))
    y += int(size_h * 0.15)
    for line in wrap(d, sub, fs, W - 2 * margin):
        lw = d.textlength(line, font=fs, **kw)
        d.text(((W - lw) / 2, y), line, font=fs, fill=(205, 214, 206), **kw)
        y += int(size_h * 0.58)
    top = y + int(H * 0.03)
    base = "home" if screen in ("menu", "banner") else screen
    src = os.path.join(raw_dir, f"{lang}-{base}.png")
    win_w = int(W * 0.74)
    frame, pad = window(Image.open(src), win_w)
    wx = (W - frame.width) // 2
    img.alpha_composite(frame, (wx, top - pad))
    k = win_w / Image.open(src).width
    if screen in ("menu", "banner"):
        over = Image.open(os.path.join(raw_dir, f"{lang}-{screen}.png")).convert("RGBA")
        ow = int(over.width * k * (1.25 if screen == "menu" else 1.15))
        oh = int(over.height * ow / over.width)
        over = over.resize((ow, oh), Image.LANCZOS)
        # Dim the window under it, as the eye goes to the panel or the card.
        dim = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        ImageDraw.Draw(dim).rectangle((wx + pad, top, wx + pad + win_w, H), fill=(0, 0, 0, 70))
        img.alpha_composite(dim)
        sh = Image.new("L", (ow + 120, oh + 120), 0)
        ImageDraw.Draw(sh).rounded_rectangle((60, 75, 60 + ow, 75 + oh), radius=40, fill=150)
        shadow = Image.new("RGBA", sh.size, (0, 0, 0, 255))
        shadow.putalpha(sh.filter(ImageFilter.GaussianBlur(30)))
        if screen == "menu":   # under the menu bar, at the top right (top left in Arabic)
            ox = wx + pad + win_w - ow - 40 if not rtl else wx + pad + 40
            oy = top + 10
        else:                  # the card slides down at the top centre
            ox, oy = (W - ow) // 2, top + 30
        img.alpha_composite(shadow, (ox - 60, oy - 60))
        img.alpha_composite(over, (ox, oy))
    img.convert("RGB").save(out, quality=92, optimize=True)


def main(raw_dir, out_dir):
    made = 0
    for lang in ("en", "ar", "fr"):
        n = 0
        for screen, copy in SLIDES:
            need = [f"{lang}-{'home' if screen in ('menu', 'banner') else screen}.png"] + ([f"{lang}-{screen}.png"] if screen in ("menu", "banner") else [])
            if not all(os.path.exists(os.path.join(raw_dir, f)) for f in need):
                print("skip", lang, screen)
                continue
            n += 1
            dst = os.path.join(out_dir, "Mac", lang, f"{n:02d}-{screen}.jpg")
            os.makedirs(os.path.dirname(dst), exist_ok=True)
            compose(raw_dir, lang, screen, *copy[lang], dst)
            made += 1
    print(f"{made} screenshots")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
