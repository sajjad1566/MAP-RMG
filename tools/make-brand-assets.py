#!/usr/bin/env python3
"""Generate every MAP-RMG launcher, adaptive-icon and splash asset from the official artwork.

Source: docs/branding/map-rmg-icon.png (the exact MAP-RMG icon, 1536x1536, black outside the
rounded frame). Run from the repo root:

    python3 tools/make-brand-assets.py

Writes (all inside app/src/main/res):
  mipmap-{m,h,xh,xxh,xxxh}dpi/ic_launcher.png             legacy square icon, frame kept, corners cut
  mipmap-{m,h,xh,xxh,xxxh}dpi/ic_launcher_round.png       legacy round icon, globe inset, circular
  mipmap-{m,h,xh,xxh,xxxh}dpi/ic_launcher_foreground.png  adaptive foreground, art inside the safe zone
  mipmap-{m,h,xh,xxh,xxxh}dpi/ic_launcher_monochrome.png  themed-icon silhouette (white on alpha)
  drawable-{m,h,xh,xxh,xxxh}dpi/splash_logo.png           splash: brand disc with the icon on it

The artwork keeps its own gold frame on purpose: it is the official mark, and the launcher masks
only cut the black corners the PNG ships with. Sizes follow the platform's numbers (48/72/96/144/
192 dpi for legacy icons, 108dp for an adaptive layer, 240dp for the Android 12 splash icon).
"""
import pathlib

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = pathlib.Path(__file__).resolve().parent.parent
SRC = ROOT / "docs/branding/map-rmg-icon.png"
RES = ROOT / "app/src/main/res"

DENSITIES = {"mdpi": 1.0, "hdpi": 1.5, "xhdpi": 2.0, "xxhdpi": 3.0, "xxxhdpi": 4.0}
LEGACY_DP = 48         # launcher icon
ADAPTIVE_DP = 108      # adaptive-icon layer
SPLASH_DP = 240        # Android 12 splash icon canvas
ART_IN_ADAPTIVE = 0.70  # how much of the 108dp canvas the artwork fills (the rest is the brand background)
ART_IN_SPLASH = 0.66    # keeps the artwork's diagonal inside the splash's circular mask
FRAME_RADIUS = 0.235    # the artwork's own corner radius, as a fraction of its side


def rounded_mask(size: int, radius_fraction: float) -> Image.Image:
    """An antialiased rounded-square mask, drawn at 4x and downsampled."""
    ss = 4
    big = Image.new("L", (size * ss, size * ss), 0)
    ImageDraw.Draw(big).rounded_rectangle(
        (0, 0, size * ss - 1, size * ss - 1), radius=int(size * ss * radius_fraction), fill=255,
    )
    return big.resize((size, size), Image.LANCZOS)


def circle_mask(size: int) -> Image.Image:
    ss = 4
    big = Image.new("L", (size * ss, size * ss), 0)
    ImageDraw.Draw(big).ellipse((0, 0, size * ss - 1, size * ss - 1), fill=255)
    return big.resize((size, size), Image.LANCZOS)


def load_art() -> Image.Image:
    art = Image.open(SRC).convert("RGB")
    return art


def cut_corners(art: Image.Image, radius_fraction: float = FRAME_RADIUS) -> Image.Image:
    """The PNG's rounded frame sits on black; give it real transparency outside the frame."""
    out = art.convert("RGBA")
    out.putalpha(rounded_mask(out.width, radius_fraction))
    return out


def scaled(img: Image.Image, size: int) -> Image.Image:
    return img.resize((size, size), Image.LANCZOS)


def place_on_canvas(img: Image.Image, canvas_px: int, art_fraction: float) -> Image.Image:
    art_px = max(1, round(canvas_px * art_fraction))
    canvas = Image.new("RGBA", (canvas_px, canvas_px), (0, 0, 0, 0))
    off = (canvas_px - art_px) // 2
    canvas.paste(scaled(img, art_px), (off, off), scaled(img, art_px))
    return canvas


def monochrome(art: Image.Image) -> Image.Image:
    """A themed-icon silhouette: white where the artwork is bright, so the globe and the pin read
    even though the source is a photo-like render."""
    lum = np.asarray(art.convert("L")).astype(np.float32) / 255.0
    # Lift the mid tones so the blue field is faint instead of solid black, and keep the chrome
    # and the gold at full strength.
    alpha = np.clip((lum - 0.06) * 1.9, 0, 1) ** 0.85
    # Grayscale + alpha: the same white silhouette at about half the bytes.
    return Image.merge(
        "LA",
        (Image.new("L", art.size, 255), Image.fromarray((alpha * 255).astype(np.uint8))),
    )


def splash_disc(art: Image.Image, canvas_px: int) -> Image.Image:
    """Brand-blue disc with the official artwork on it. The disc matches the circular mask Android
    12 puts over a splash icon, so nothing important is ever cropped."""
    disc = Image.new("RGBA", (canvas_px, canvas_px), (0, 0, 0, 0))
    fill = Image.new("RGBA", (canvas_px, canvas_px), (0, 0, 0, 255))
    # A soft royal-blue glow from the top left over the brand dark blue: the icon's own palette.
    grad = np.zeros((canvas_px, canvas_px, 3), dtype=np.float32)
    yy, xx = np.mgrid[0:canvas_px, 0:canvas_px].astype(np.float32)
    t = np.clip((xx + yy) / (2.0 * canvas_px), 0, 1)
    inner = np.array([15, 46, 108], dtype=np.float32)    # #0F2E6C, dark blue with a blue cast
    outer = np.array([11, 28, 61], dtype=np.float32)     # #0B1C3D, the brand dark blue
    grad = inner[None, None, :] * (1 - t)[..., None] + outer[None, None, :] * t[..., None]
    fill = Image.fromarray(grad.astype(np.uint8)).convert("RGB").convert("RGBA")
    disc.paste(fill, (0, 0), circle_mask(canvas_px).filter(ImageFilter.GaussianBlur(canvas_px * 0.004)))
    art_px = round(canvas_px * ART_IN_SPLASH)
    off = (canvas_px - art_px) // 2
    a = scaled(cut_corners(art), art_px)
    disc.paste(a, (off, off), a)
    return disc


def save(img: Image.Image, path: pathlib.Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path, optimize=True)
    kb = path.stat().st_size / 1024
    print(f"  {path.relative_to(ROOT)}  {img.width}px  {kb:6.1f} KB")


def main() -> None:
    art = load_art()
    leg = cut_corners(art)                                # legacy square: the frame, corners cut
    inset = round(art.width * 0.135)                      # round legacy: the globe inside the frame
    globe = art.crop((inset, inset, art.width - inset, art.height - inset))
    print("launcher + adaptive assets")
    for name, factor in DENSITIES.items():
        d = RES / f"mipmap-{name}"
        save(scaled(leg, round(LEGACY_DP * factor)), d / "ic_launcher.png")
        side = round(LEGACY_DP * factor)
        rnd = Image.new("RGBA", (side, side), (11, 28, 61, 255))   # brand dark blue disc
        rnd.putalpha(circle_mask(side))
        art_px = side
        off = (side - art_px) // 2
        a = scaled(globe, art_px).convert("RGBA")
        a.putalpha(circle_mask(art_px))
        rnd.paste(a, (off, off), a)
        save(rnd, d / "ic_launcher_round.png")
        canvas = round(ADAPTIVE_DP * factor)
        save(place_on_canvas(leg, canvas, ART_IN_ADAPTIVE), d / "ic_launcher_foreground.png")
        save(place_on_canvas(monochrome(art), canvas, ART_IN_ADAPTIVE), d / "ic_launcher_monochrome.png")
    print("splash assets")
    for name, factor in DENSITIES.items():
        save(splash_disc(art, round(SPLASH_DP * factor)), RES / f"drawable-{name}" / "splash_logo.png")
    print("done")


if __name__ == "__main__":
    main()
