"""Generate build-only WebP resource overlays; never modify production PNGs.

Requires Pillow with WebP support. Dimensions and alpha remain identical.
Compress RGB at high quality while keeping alpha lossless. Report visible error
instead of automatically replacing detailed sprites with large lossless files.
"""

import argparse
from concurrent.futures import ThreadPoolExecutor
from io import BytesIO
import json
from pathlib import Path

from PIL import Image, ImageChops, ImageStat


def encode(source: Path, output: Path) -> dict:
    with Image.open(source) as image:
        original = image.convert("RGBA")
    quality = 94 if source.stem.startswith(("ui_", "ic_")) else 90
    if source.stem == "road_asphalt_texture":
        quality = 86
    buffer = BytesIO()
    original.save(buffer, "WEBP", quality=quality, method=6, exact=True)
    encoded = buffer.getvalue()
    with Image.open(BytesIO(encoded)) as image:
        decoded = image.convert("RGBA")
    assert decoded.size == original.size, source.name
    alpha = original.getchannel("A")
    assert ImageChops.difference(alpha, decoded.getchannel("A")).getbbox() is None, source.name
    visible = alpha.point(lambda value: 255 if value >= 32 else 0)
    difference = ImageChops.difference(original.convert("RGB"), decoded.convert("RGB"))
    error = ImageStat.Stat(difference, visible).mean
    # Hidden and faint edge RGB does not contribute equally to the rendered image.
    # Alpha itself was checked bit-for-bit above; measure the composited RGB error.
    weighted = ImageChops.multiply(difference, Image.merge("RGB", (alpha, alpha, alpha)))
    composited_error = ImageStat.Stat(weighted, visible).mean
    # Use luminance-weighted channel error: the blue channel contributes much
    # less perceived brightness in gold/orange art than green or red.
    perceptual_error = sum(error * weight for error, weight in zip(composited_error, (0.2126, 0.7152, 0.0722)))
    if perceptual_error > 4.5 or max(composited_error) > 12.0:
        raise ValueError(f"{source.name}: excessive visible RGB error {composited_error}; review encoding")
    destination = output / "drawable-nodpi" / source.with_suffix(".webp").name
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_bytes(encoded)
    return {
        "source": source.name,
        "output": destination.name,
        "width": original.width,
        "height": original.height,
        "original_bytes": source.stat().st_size,
        "webp_bytes": len(encoded),
        "alpha_exact": True,
        "lossless": False,
        "quality": quality,
        "visible_rgb_mean_absolute_error": error,
        "composited_rgb_mean_absolute_error": composited_error,
        "luminance_weighted_mean_absolute_error": perceptual_error,
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--report", type=Path, required=True)
    args = parser.parse_args()
    sources = sorted(args.source.glob("*.png"))
    if not sources:
        raise SystemExit("No PNG resources found")
    # Remove only obsolete generated overlays, never production inputs.
    target = (args.output.resolve() / "drawable-nodpi").resolve()
    if target == args.source.resolve() or args.source.resolve() in target.parents:
        raise SystemExit("Output must not be inside the original resource directory")
    expected = {source.with_suffix(".webp").name for source in sources}
    for stale in target.glob("*.webp"):
        if stale.name not in expected:
            stale.unlink()
    with ThreadPoolExecutor(max_workers=4) as pool:
        rows = list(pool.map(lambda source: encode(source, args.output), sources))
    report = {
        "count": len(rows),
        "original_bytes": sum(row["original_bytes"] for row in rows),
        "optimized_bytes": sum(row["webp_bytes"] for row in rows),
        "resources": rows,
    }
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    # Leave space for the existing soundtrack, splash, dex, libraries and signing.
    # The signed APK and ZIP are also checked against the actual 10,000,000-byte cap.
    if report["optimized_bytes"] > 8_050_000:
        raise ValueError(f"Art pack exceeds the 8,050,000-byte budget: {report['optimized_bytes']:,}")
    print(f"Optimized {len(rows)} PNGs: {report['original_bytes']:,} -> {report['optimized_bytes']:,} bytes; exact alpha and dimensions")


if __name__ == "__main__":
    main()
