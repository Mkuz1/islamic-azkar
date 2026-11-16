"""Makes every platform's icon from the one App Store icon (store/appstore/icon-1024.png).

    python3 tools/brand_icons.py

Android: adaptive icon layers (all densities) and the TV banner. Mac: the app icon set (rounded
square with Apple's margins and a soft shadow). Website: icon and share image. Play Store:
the 512 px icon, and the icon in the feature graphics.
"""
import json
import os

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = Image.open(os.path.join(ROOT, "store", "appstore", "icon-1024.png")).convert("RGBA")
ART = Image.open(os.path.join(ROOT, "store", "brand", "icon-artwork-original.jpg")).convert("RGB")
RES = os.path.join(ROOT, "IslamicAzkar", "app", "src", "main", "res")
FONTS = os.path.join(RES, "font")


def p(*parts):
    return os.path.join(ROOT, *parts)


def rounded(img, radius):
    mask = Image.new("L", img.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, img.width - 1, img.height - 1), radius=radius, fill=255)
    out = img.copy()
    out.putalpha(mask)
    return out


def android():
    # Launchers show the middle 72 of 108 dp, cut to a circle, squircle or square. The artwork fills
    # exactly that, so the calligraphy stays inside even the round mask; around it, the same artwork blurred.
    for d, px in {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}.items():
        bg = SRC.resize((px, px), Image.LANCZOS).filter(ImageFilter.GaussianBlur(px / 18))
        inner = round(px * 72 / 108)
        o = (px - inner) // 2
        bg.alpha_composite(SRC.resize((inner, inner), Image.LANCZOS), (o, o))
        bg.convert("RGB").save(os.path.join(RES, f"mipmap-{d}", "ic_launcher_background.png"), optimize=True)
        Image.new("RGBA", (px, px), (0, 0, 0, 0)).save(os.path.join(RES, f"mipmap-{d}", "ic_launcher_foreground.png"), optimize=True)
    # TV banner (320 x 180 at xhdpi): the icon and the name.
    w, h = 640, 360
    ban = ART.resize((round(ART.width * h / ART.height), h), Image.LANCZOS)
    ban = ban.crop(((ban.width - w) // 2, 0, (ban.width - w) // 2 + w, h)).filter(ImageFilter.GaussianBlur(24)).convert("RGBA")
    ban.alpha_composite(Image.new("RGBA", (w, h), (6, 30, 22, 120)))
    ic = rounded(SRC.resize((236, 236), Image.LANCZOS), 52)
    ban.alpha_composite(ic, (40, (h - 236) // 2))
    d = ImageDraw.Draw(ban)
    en = ImageFont.truetype(os.path.join(FONTS, "inter.ttf"), 46)
    d.text((300, 118), "Islamic", font=en, fill=(240, 226, 190))
    d.text((300, 172), "Azkar", font=en, fill=(240, 226, 190))
    small = ImageFont.truetype(font_any(), 24)
    d.text((302, 236), "Hisn al-Muslim", font=small, fill=(190, 210, 198))
    ban.convert("RGB").resize((320, 180), Image.LANCZOS).save(os.path.join(RES, "drawable-xhdpi", "tv_banner.png"), optimize=True)


def font_any():
    for f in sorted(os.listdir(FONTS)):
        if f.endswith(".ttf") and ("inter" in f or "sans" in f):
            return os.path.join(FONTS, f)
    return os.path.join(FONTS, sorted(f for f in os.listdir(FONTS) if f.endswith(".ttf"))[0])


def mac_icon(size):
    # Apple's macOS grid: an 824 pt rounded square (radius 185) in a 1024 canvas, with a drop shadow.
    k = size / 1024
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    body = round(824 * k)
    o = (size - body) // 2
    if size >= 64:
        sh = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        ImageDraw.Draw(sh).rounded_rectangle((o, o + round(10 * k), o + body, o + body + round(10 * k)), radius=round(185 * k), fill=(0, 0, 0, 110))
        canvas.alpha_composite(sh.filter(ImageFilter.GaussianBlur(max(1, 14 * k))))
    canvas.alpha_composite(rounded(SRC.resize((body, body), Image.LANCZOS), round(185 * k)), (o, o))
    return canvas


def mac():
    d = p("IslamicAzkar", "mac", "Assets.xcassets", "AppIcon.appiconset")
    for im in json.load(open(os.path.join(d, "Contents.json")))["images"]:
        if "filename" not in im:
            continue
        pt = float(im["size"].split("x")[0])
        px = round(pt * float(im.get("scale", "1x").rstrip("x")))
        mac_icon(px).save(os.path.join(d, im["filename"]), optimize=True)


def web():
    SRC.convert("RGB").resize((512, 512), Image.LANCZOS).save(p("website", "img", "icon.png"), optimize=True)
    # Share image 1200 x 630 from the wide artwork.
    w, h = 1200, 630
    a = ART.resize((w, round(ART.height * w / ART.width)), Image.LANCZOS)
    t = (a.height - h) // 2
    a.crop((0, t, w, t + h)).save(p("website", "img", "og.jpg"), quality=90, optimize=True)


def play():
    SRC.convert("RGB").resize((512, 512), Image.LANCZOS).save(p("store", "icon_512.png"), optimize=True)
    # The feature graphics carry the icon at (56, 113), 104 px: swap in the new one.
    for lang in ("en", "ar", "fr"):
        f = p("store", lang, "feature_graphic.jpg")
        if not os.path.exists(f):
            continue
        g = Image.open(f).convert("RGBA")
        x, y, s = ICON_BOX.get(lang, ICON_BOX["en"])
        # Cover the old icon and its glow: a slightly larger new icon with a soft shadow.
        x, y, s = x - 7, y - 7, s + 14
        sh = Image.new("RGBA", g.size, (0, 0, 0, 0))
        ImageDraw.Draw(sh).rounded_rectangle((x - 4, y + 2, x + s + 4, y + s + 10), radius=round(s * 0.24), fill=(0, 0, 0, 120))
        g.alpha_composite(sh.filter(ImageFilter.GaussianBlur(9)))
        g.alpha_composite(rounded(SRC.resize((s, s), Image.LANCZOS), round(s * 0.22)), (x, y))
        g.convert("RGB").save(f, quality=92, optimize=True)


ICON_BOX = {}


if __name__ == "__main__":
    import sys
    android(); mac(); web()
    if "--play" in sys.argv:
        ICON_BOX.update(json.loads(sys.argv[sys.argv.index("--play") + 1]))
        play()
    print("icons written")
