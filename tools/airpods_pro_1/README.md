# AirPods Pro 1 artwork

> Current production layout, studio and still-layer contract: [shared artwork pipeline](../apple_artwork/README.md). The per-model screenshot fits and color calibration below are historical reference measurements, not production spacing or grading.


Editable Blender rig and Android artwork derived from Apple's original 2019
AirPods Pro USDZ. This is the A2190 charging case: no lanyard loop or bottom
speaker. AirPods 1/2 are excluded from this work and retain their app fallback.

## Sources

`provenance.json` pins source URLs and SHA-256 hashes. The original USDZ is an
Apple product asset, not a CC BY model or an original LibrePods mesh. No open
asset license is claimed. Reference photos are used for comparison only and
are not shipped as textures or app artwork.

- [Apple announcement](https://www.apple.com/newsroom/2019/10/apple-reveals-new-airpods-pro-available-october-30/)
- [Apple original USDZ](https://www.apple.com/105/media/us/airpods-pro/2019/1299e2f5_9206_4470_b28e_08307a42f19b/quicklook/airpods_pro_ios13.usdz)
- [2021 connected popup](https://www.laptopmag.com/how-to/how-to-connect-airpods-to-iphone-the-easiest-way-to-pair-your-earbuds)
- [A2190 rear photograph](https://www.ebay.com/itm/115589475274)

## Rebuild

Run from the repository root. Blender 5.2, `uv`, Python with numpy/Pillow and
ffmpeg are required. Keep intermediate frames outside the repository.

```sh
python3 tools/airpods_pro_1/fetch.py --out /path/to/pro1/refs
uv run --with usd-core python tools/airpods_pro_1/prepare.py \
  --source /path/to/pro1/refs/apple_airpods_pro_1.usdz --out /path/to/pro1/prepared
blender -b --python-exit-code 1 --python tools/airpods_pro_1/build.py -- \
  --source /path/to/pro1/prepared/preview-surface.usdc --out /path/to/pro1/airpods_pro_1.blend
blender -b /path/to/pro1/airpods_pro_1.blend --python-exit-code 1 \
  --python tools/airpods_pro_1/verify_rig.py -- /path/to/pro1/rig-verification.json
blender -b --python-exit-code 1 --python tools/airpods_pro_1/render.py -- \
  --model /path/to/pro1/airpods_pro_1.blend --mode connected \
  --out /path/to/pro1/connected --frames 360 --samples 128
```

Render `island` with the same frame/sample counts. Render `assembly`, `buds`,
`case`, `left` and `right` with one frame and 192 samples. Then:

```sh
python3 tools/airpods_pro_1/encode.py --renders /path/to/pro1 --out /path/to/pro1/assets
python3 tools/airpods_pro_1/verify_assets.py --renders /path/to/pro1 \
  --assets /path/to/pro1/assets --report /path/to/pro1/asset-verification.json
```

## Rig and rear hardware

`H_Left` and `H_Right` move each complete unit independently. `H_BudPair` rotates
both together. `H_Case["lid_open_degrees"]` drives `Lid_Hinge` from 0 to 115°;
timeline frames 1–160 demonstrate opening and closing. Clear the case action
before posing it manually. Textures are packed into the blend.

The source's open pose is baked into its mesh. The 115° rotation makes its lid
rim planar; its pivot is solved by matching that rim to the body seam with a
0.05 mm clearance. Case vertices are retained. Only earbud outer shells receive
one subdivision level. The tracking placeholder is excluded.

The AR hinge has overlapping relief and baked appearance overlays. `hinge.py`
reconstructs its visible face as a planar satin plate using the original
footprint, with separate fixed end tabs and a moving central leaf. The rear
button has a flush face with a 0.03 mm edge bevel. The case seam is not enlarged.

## Popup calibration

Production videos start at front 0°, last six seconds at 60 fps, and turn 360°.
`--reference-phase --frames 1` is for comparison only. `popup-layout.json`
records the original Pro 1 screenshot's bounds and pose. Complete units receive
a rigid orientation correction to match upright stems and visible tip openings.
Silicone has a separate matte material. Color calibration measures the case in
the reference, while studio reflection cards determine gloss physically.

```sh
python3 tools/apple_artwork/calibrate_popup.py --tools tools/airpods_pro_1 \
  --work /path/to/pro1 --frame /path/to/pro1/popup-preview/0000.png
python3 tools/apple_artwork/compare_popup.py --tools tools/airpods_pro_1 --work /path/to/pro1
```

Outputs are day/night connection clips (1050×354), an island clip (418×418), and
five RGBA PNGs. H.264 uses full-range BT.709 and faststart. Verification covers
all 720 frames, clipping, loop boundaries, decode, metadata, and reference bounds.

Individual battery rings follow the final front-frame stem centers; combined
and case indicators retain the screenshot battery positions.
