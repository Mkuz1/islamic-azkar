"""Apple TV icon and Top Shelf images (IslamicAzkar/tv/Assets.xcassets) from the app icon artwork.

tvOS icons are layered (the Siri Remote tilts them): a back layer (the green backdrop with its
stars and glow) and a front layer (the calligraphy icon), at 400 x 240 (1x, 2x) for the Home
Screen and 1280 x 768 for the App Store. Top Shelf: 1920 x 720 and 2320 x 720 (1x, 2x).

    python3 tools/tv_assets.py
"""
import json
import os

from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ICON = Image.open(os.path.join(ROOT, "store", "appstore", "icon-1024.png")).convert("RGBA")
ART = Image.open(os.path.join(ROOT, "store", "brand", "icon-artwork-original.jpg")).convert("RGB")
OUT = os.path.join(ROOT, "IslamicAzkar", "tv", "Assets.xcassets")
INFO = {"author": "xcode", "version": 1}


def cover(img, w, h):
    """img scaled and centre-cropped to w x h."""
    k = max(w / img.width, h / img.height)
    r = img.resize((round(img.width * k), round(img.height * k)), Image.LANCZOS)
    x, y = (r.width - w) // 2, (r.height - h) // 2
    return r.crop((x, y, x + w, y + h))


def back(w, h):
    """The wide artwork, blurred: the icon's own green, stars and glow without the calligraphy."""
    b = cover(ART, w, h).filter(ImageFilter.GaussianBlur(max(w, h) / 60))
    shade = Image.new("RGB", (w, h), (8, 32, 24))
    return Image.blend(b, shade, 0.25)


def front(w, h, share=0.86):
    """The calligraphy icon with rounded corners, centred, on a transparent layer."""
    f = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    s = round(h * share)
    ic = ICON.resize((s, s), Image.LANCZOS)
    mask = Image.new("L", (s, s), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, s - 1, s - 1), radius=round(s * 0.2), fill=255)
    ic.putalpha(mask)
    f.alpha_composite(ic, ((w - s) // 2, (h - s) // 2))
    return f


def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as fh:
        json.dump(data, fh, indent=2)


def imageset(folder, images):
    """images: [(scale, PIL image, file name)]"""
    os.makedirs(folder, exist_ok=True)
    entries = []
    for scale, img, name in images:
        img.save(os.path.join(folder, name), optimize=True)
        entries.append({"filename": name, "idiom": "tv", "scale": scale})
    write_json(os.path.join(folder, "Contents.json"), {"images": entries, "info": INFO})


def imagestack(folder, sizes):
    """A two-layer image stack; sizes: [(scale, w, h)]."""
    write_json(os.path.join(folder, "Contents.json"),
               {"info": INFO, "layers": [{"filename": "Front.imagestacklayer"}, {"filename": "Back.imagestacklayer"}]})
    for layer, make in (("Front", front), ("Back", back)):
        lf = os.path.join(folder, f"{layer}.imagestacklayer")
        write_json(os.path.join(lf, "Contents.json"), {"info": INFO})
        imageset(os.path.join(lf, "Content.imageset"),
                 [(sc, make(w, h), f"{layer.lower()}{'' if sc == '1x' else '@' + sc}.png") for sc, w, h in sizes])


def top_shelf(w, h):
    """The wide banner: the blurred backdrop with the calligraphy icon in the middle."""
    img = back(w, h).convert("RGBA")
    img.alpha_composite(front(w, h, share=0.78))
    return img.convert("RGB")


def main():
    brand = os.path.join(OUT, "App Icon & Top Shelf Image.brandassets")
    write_json(os.path.join(OUT, "Contents.json"), {"info": INFO})
    write_json(os.path.join(brand, "Contents.json"), {"info": INFO, "assets": [
        {"filename": "App Icon - App Store.imagestack", "idiom": "tv", "role": "primary-app-icon", "size": "1280x768"},
        {"filename": "App Icon.imagestack", "idiom": "tv", "role": "primary-app-icon", "size": "400x240"},
        {"filename": "Top Shelf Image Wide.imageset", "idiom": "tv", "role": "top-shelf-image-wide", "size": "2320x720"},
        {"filename": "Top Shelf Image.imageset", "idiom": "tv", "role": "top-shelf-image", "size": "1920x720"},
    ]})
    imagestack(os.path.join(brand, "App Icon - App Store.imagestack"), [("1x", 1280, 768)])
    imagestack(os.path.join(brand, "App Icon.imagestack"), [("1x", 400, 240), ("2x", 800, 480)])
    imageset(os.path.join(brand, "Top Shelf Image Wide.imageset"),
             [("1x", top_shelf(2320, 720), "top-wide.png"), ("2x", top_shelf(4640, 1440), "top-wide@2x.png")])
    imageset(os.path.join(brand, "Top Shelf Image.imageset"),
             [("1x", top_shelf(1920, 720), "top.png"), ("2x", top_shelf(3840, 1440), "top@2x.png")])
    accent = os.path.join(OUT, "AccentColor.colorset")
    write_json(os.path.join(accent, "Contents.json"), {"info": INFO, "colors": [{"idiom": "universal", "color": {
        "color-space": "srgb", "components": {"red": "0.839", "green": "0.702", "blue": "0.416", "alpha": "1.000"}}}]})
    print("tv assets written")


if __name__ == "__main__":
    main()
