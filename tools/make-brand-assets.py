#!/usr/bin/env python3
"""Generate every MAP-RMG launcher, adaptive-icon, splash and notification asset from the official artwork.

Source: docs/branding/map-rmg-icon.png (the exact MAP-RMG icon: a map with a blue route, a red
destination pin and the navigation puck, shipped on a black field outside its rounded frame).
Run from the repo root:

    python3 tools/make-brand-assets.py

Writes (all inside app/src/main/res):
  mipmap-{m,h,xh,xxh,xxxh}dpi/ic_launcher.png             legacy square icon, frame kept, corners cut
  mipmap-{m,h,xh,xxh,xxxh}dpi/ic_launcher_round.png       legacy round icon, circular crop of the art
  mipmap-{m,h,xh,xxh,xxxh}dpi/ic_launcher_foreground.png  adaptive foreground, art inside the safe zone
  mipmap-{m,h,xh,xxh,xxxh}dpi/ic_launcher_monochrome.png  themed-icon silhouette (white on alpha)
  mipmap-{m,h,xh,xxh,xxxh}dpi/ic_notification.png         status-bar mark (white silhouette on alpha)
  drawable-{m,h,xh,xxh,xxxh}dpi/splash_logo.png           splash: the icon on a white disc

The artwork keeps its own rounded frame on purpose: it is the official mark. The PNG ships the
area outside that frame as pure black, which is turned into real transparency here (see
[load_art]'s alpha ramp), so no black square ever shows on a home screen, in the splash or in an
in-app mark. Sizes follow the platform's numbers (48/72/96/144/192 px for legacy icons, a 108dp
adaptive layer, 240dp for the Android 12 splash icon, 24dp for a notification mark).
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
NOTIFY_DP = 24
BRAND_MARK_PX = 448     # in-app mark: the artwork as uploaded, one crisp master for Compose         # status-bar icon
ART_IN_ADAPTIVE = 0.62  # how much of the 108dp canvas the artwork fills (the launcher shows the
                        # middle 72dp, so the frame and the red pin stay whole under any mask)
EDGE_LUM = 100          # brightest channel at which the artwork starts (see load_art)
ART_IN_SPLASH = 0.62    # Android 12 shows the middle 2/3 of the 240dp canvas (a 160dp circle), so
                        # the artwork stays inside that circle with a white ring around it
FRAME_RADIUS = 0.235    # the artwork's own corner radius, as a fraction of its side
SPLASH_DISC = (255, 255, 255)  # white disc: the icon is a light map, a navy disc read as a hole


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
    """The official artwork, cropped to its frame, with the black field outside it made transparent.

    The PNG has no alpha channel and the field beyond the rounded frame is pure black. Its edge
    pixels blend into that black (a single row of grays), and keeping them would leave a dark rim
    around the icon, so the cut is made on brightness alone: a pixel is part of the artwork when
    its brightest channel reaches EDGE_LUM. Nothing inside the artwork is that dark (the darkest
    interior pixel is around 156), while everything outside is under 10, so the line falls in the
    gap. The alpha edge is then softened by one pixel, and the LANCZOS downscale finishes the
    antialiasing at every target size.
    """
    rgb = Image.open(SRC).convert("RGB")
    lum = np.asarray(rgb).astype(np.float32).max(axis=2)
    alpha = Image.fromarray((np.clip(lum, 0, 255) >= EDGE_LUM).astype(np.uint8) * 255)
    alpha = alpha.filter(ImageFilter.GaussianBlur(1.0))
    out = rgb.convert("RGBA")
    out.putalpha(alpha)
    box = out.getchannel("A").point(lambda v: 255 if v > EDGE_LUM else 0).getbbox()
    return out.crop(box)


def scaled(img: Image.Image, size: int) -> Image.Image:
    return img.resize((size, size), Image.LANCZOS)


def place_on_canvas(img: Image.Image, canvas_px: int, art_fraction: float) -> Image.Image:
    art_px = max(1, round(canvas_px * art_fraction))
    canvas = Image.new("RGBA", (canvas_px, canvas_px), (0, 0, 0, 0))
    off = (canvas_px - art_px) // 2
    a = scaled(img, art_px)
    canvas.paste(a, (off, off), a)
    return canvas


def monochrome(art: Image.Image) -> Image.Image:
    """The themed-icon silhouette: a route running from the nav puck to the destination pin.

    A luminance silhouette cannot work here (the map is a pale, high-key illustration, so it either
    washes out or turns into a white square), so the mark is taken from the artwork's own ink: its
    blue route and puck plus its red pin. Those two hues are the only saturated colors on the icon,
    which makes the selection exact rather than a guess at thresholds.
    """
    a = np.asarray(art.convert("RGB")).astype(np.int16)
    r, g, b = a[..., 0], a[..., 1], a[..., 2]
    # Tight tests, so the pale map, the yellow wedge and the washed highlights stay out: the blue
    # has to be genuinely blue and genuinely dark in red, the red genuinely red.
    blue = (b > 170) & (b - g > 60) & (b - r > 90)
    red = (r > 170) & (g < 130) & (b < 130)
    mask = (blue | red)
    # Drop the specks that antialiased color edges leave beside the map's yellow wedge: anything
    # that is not part of a sizeable blob is noise, not a line of the mark.
    mask = keep_large_blobs(mask, min_fraction=0.0004)
    alpha = Image.fromarray(np.clip(mask.astype(np.float32) * 255.0, 0, 255).astype(np.uint8))
    alpha = alpha.filter(ImageFilter.GaussianBlur(1.2))
    return Image.merge("LA", (Image.new("L", art.size, 255), alpha))


def keep_large_blobs(mask: np.ndarray, min_fraction: float) -> np.ndarray:
    """The union of the mask's connected components that cover at least [min_fraction] of it.

    A flood fill over the full-resolution mask would need a library this repo does not depend on,
    so the labeling runs on a quarter-size copy and the kept labels are scaled back up: the mark's
    strokes survive that, and specks only ever get smaller.
    """
    small = np.asarray(
        Image.fromarray((mask * 255).astype(np.uint8)).resize(
            (mask.shape[1] // 4, mask.shape[0] // 4), Image.BILINEAR,
        )
    ) > 127
    h, w = small.shape
    labels = np.zeros((h, w), dtype=np.int32)
    kept = np.zeros((h, w), dtype=bool)
    min_px = max(16, int(mask.size / 16 * min_fraction))
    next_label = 0
    for y0 in range(h):
        for x0 in range(w):
            if not small[y0, x0] or labels[y0, x0]:
                continue
            next_label += 1
            stack = [(y0, x0)]
            labels[y0, x0] = next_label
            blob = []
            while stack:
                y, x = stack.pop()
                blob.append((y, x))
                for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    ny, nx = y + dy, x + dx
                    if 0 <= ny < h and 0 <= nx < w and small[ny, nx] and not labels[ny, nx]:
                        labels[ny, nx] = next_label
                        stack.append((ny, nx))
            if len(blob) >= min_px:
                for y, x in blob:
                    kept[y, x] = True
    grown = Image.fromarray((kept * 255).astype(np.uint8)).resize(
        (mask.shape[1], mask.shape[0]), Image.BILINEAR,
    )
    return np.asarray(grown) > 127


def splash_disc(art: Image.Image, canvas_px: int) -> Image.Image:
    """A white disc carrying the official artwork, sized for Android 12's circular splash mask.

    White, not the brand navy the first icon used: this mark is a light-colored map, and on navy
    its pale frame vanished into the background while the black-free corners made a dark square.
    """
    disc = Image.new("RGBA", (canvas_px, canvas_px), (0, 0, 0, 0))
    fill = Image.new("RGBA", (canvas_px, canvas_px), SPLASH_DISC + (255,))
    disc.paste(fill, (0, 0), circle_mask(canvas_px))
    art_px = round(canvas_px * ART_IN_SPLASH)
    off = (canvas_px - art_px) // 2
    a = scaled(art, art_px)
    disc.paste(a, (off, off), a)
    return disc


def save(img: Image.Image, path: pathlib.Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path, optimize=True)
    kb = path.stat().st_size / 1024
    print(f"  {path.relative_to(ROOT)}  {img.width}px  {kb:6.1f} KB")


def main() -> None:
    art = load_art()
    leg = art                                            # legacy square: the frame, corners cut
    print("launcher + adaptive assets")
    mono = monochrome(art)
    for name, factor in DENSITIES.items():
        d = RES / f"mipmap-{name}"
        save(scaled(leg, round(LEGACY_DP * factor)), d / "ic_launcher.png")
        side = round(LEGACY_DP * factor)
        rnd = Image.new("RGBA", (side, side), (0, 0, 0, 0))
        a = scaled(art, side)
        rnd.paste(a, (0, 0), a)
        rnd.putalpha(circle_mask(side))
        save(rnd, d / "ic_launcher_round.png")
        canvas = round(ADAPTIVE_DP * factor)
        save(place_on_canvas(leg, canvas, ART_IN_ADAPTIVE), d / "ic_launcher_foreground.png")
        save(place_on_canvas(mono, canvas, ART_IN_ADAPTIVE), d / "ic_launcher_monochrome.png")
        # A status-bar glyph keeps a little air around it, like the platform's own icons.
        save(place_on_canvas(mono, round(NOTIFY_DP * factor), 0.90), d / "ic_notification.png")
    # In-app mark (onboarding header, Welcome/start page): the artwork exactly as uploaded, at a
    # density that stays sharp at 84dp on a 4x screen. Kept out of the launcher mipmaps because it
    # is not masked there.
    mark_dir = RES / "drawable-nodpi"
    mark_dir.mkdir(parents=True, exist_ok=True)
    save(scaled(art, BRAND_MARK_PX), mark_dir / "ic_brand_mark.png")
    print("splash assets")
    for name, factor in DENSITIES.items():
        save(splash_disc(art, round(SPLASH_DP * factor)), RES / f"drawable-{name}" / "splash_logo.png")
    print("done")


if __name__ == "__main__":
    main()
