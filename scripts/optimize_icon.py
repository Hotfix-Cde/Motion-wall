from pathlib import Path

from PIL import Image


asset_dir = Path(__file__).resolve().parents[1] / "assets" / "images"
source = asset_dir / "icon.png"
targets = [
    asset_dir / "icon.png",
    asset_dir / "splash-icon.png",
    asset_dir / "favicon.png",
    asset_dir / "android-icon-foreground.png",
]

with Image.open(source) as original:
    icon = original.convert("RGBA")
    icon.thumbnail((512, 512), Image.Resampling.LANCZOS)
    for target in targets:
        icon.save(target, format="PNG", optimize=True, compress_level=9)
