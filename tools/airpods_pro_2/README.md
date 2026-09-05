# AirPods Pro 2 — official-source Blender artwork

This replaces the inherited Pro 2 videos and images with new renders of Apple's
2022 AirPods Pro 2 AR model. Geometry and textures are Apple-derived, not modeled
from scratch. The original URL was recovered from the January 3, 2023 archive of
Apple's product page and the USDZ was downloaded directly from Apple's server.
`provenance.json` pins it and an official reference photograph by SHA-256.

Sources: [Apple announcement](https://www.apple.com/newsroom/2022/09/apple-announces-the-next-generation-of-airpods-pro/),
[case and unit specifications](https://support.apple.com/en-us/111851), and
[archived product page](https://web.archive.org/web/20230103214237/https://www.apple.com/airpods-pro/).
The source has the 2022 Lightning case with speaker and lanyard loop. Lightning
and USB-C model classes continue sharing these resources; the bottom connector
is not a separate USB-C model. Pro 1 continues using this set as its fallback.

## Composition and resources

No previous Pro 2 image or video measurements are reused. Their provenance and
layout are uncertain because they came from upstream and were later recomposited.
`replaced-artwork.json` records their hashes as historical context only.

Connection rendering adopts the Pro 3 pipeline's 0.2 m orthographic width,
10-degree camera elevation, case center at X=0.0407 m, pair center at
X=-0.04106 m, and unit handles at X=±0.02008 m relative to that pair. The original
Pro 2 physical size is retained. Island rendering uses a 0.060 m camera width
and handles at ±0.012 m. The grade targets the same neutral white tone as Pro 3;
`calibrate.py` derives it from the new render rather than old Pro 2 pixels.

The eight resource names remain unchanged. All new assets reside in
`android/app/src/main/res-apple/`, with no CC BY or GPL grant claimed for them.
The three obsolete videos under `res/` are removed to avoid duplicate resources.

| Resource | Output |
|---|---|
| `raw/airpods_pro_2_connected.mp4` | 1050×354, white background |
| `raw-night/airpods_pro_2_connected.mp4` | 1050×354, #1d1c1f background |
| `raw/airpods_pro_2_island.mp4` | 418×418, black background |
| `drawable/airpods_pro_2.png` | Open case and floating units, RGBA |
| `drawable/airpods_pro_2_buds.png` | Paired units with settings margins, RGBA |
| `drawable/airpods_pro_2_case.png` | Closed case, 805×805 RGBA |
| `drawable/airpods_pro_2_left.png`, `..._right.png` | Individual units, RGBA |

All videos are 6 seconds, 360 frames, 60 fps, H.264 with explicit full-range
BT.709 VUI and faststart. The pair and case rotate about separate centers.

## Editable model

Local deliverables: `/Users/jsp/Git/librepods-render/pro2/`.
`airpods_pro_2.blend` contains packed textures and an opening/closing action;
`airpods_pro_2_studio.blend` contains the connection camera and lighting.
`assets/` contains all eight app files. Raw frames and inspection previews stay
in this render workspace rather than the source repository.

| Control | Contents |
|---|---|
| `H_Left`, `H_Right` | 20 meshes each; complete independent units |
| `H_BudPair` | Optional common turntable parent |
| `H_Case` | 32 body meshes and `lid_open_degrees` property |
| `Lid_Hinge` | 8 lid meshes, including liner and moving hinge |

Two inner black earbud meshes were authored under the source case hierarchy;
these are explicitly assigned to their respective units so they move with them.
The older USDZ bakes the lid's 115-degree pose into vertices. `build.py` restores
its pivot by aligning the lid's planar rim to the body's top seam, with a 0.05 mm
clearance. The derived pivot is approximately (0, 0.00994843, 0.00131840) m.
This is a geometric reconstruction, not an original animated source transform.
The closed case measures 60.817×21.852×45.277 mm after surface refinement,
within 0.4 mm of Apple's 60.6×21.7×45.2 mm specification.

Timeline frames 1–160 open and close the lid. Clear `H_Case`'s action for manual
posing; set `lid_open_degrees` from 0 to 115. Python edits require
`case.update_tag(refresh={'OBJECT'})` followed by a view-layer update.

`prepare.py` removes the competing MaterialX material output on a derivative
copy so Blender reads the source's existing USD Preview Surface texture networks.
Without that step Blender 5.2 silently imports materials without textures.
Four exterior shells use one non-destructive subdivision level and recalculated
normals. White plastic uses studio illumination instead of the AR material's
baked emission and roughness maps; inserts retain their source detail maps.

## Reproduce

Requires Blender 5.2, Python with numpy/Pillow, ffmpeg, and `usd-core` for source
preparation (the commands below use `uv`). Run from the repository root:

```bash
python3 tools/airpods_pro_2/fetch.py --out ../librepods-render/pro2/refs
uv run --with usd-core python tools/airpods_pro_2/prepare.py \
  --source ../librepods-render/pro2/refs/apple_airpods_pro_2.usdz \
  --out ../librepods-render/pro2/prepared
blender -b --python-exit-code 1 --python tools/airpods_pro_2/build.py -- \
  --source ../librepods-render/pro2/prepared/preview-surface.usdc \
  --out ../librepods-render/pro2/airpods_pro_2.blend
blender -b ../librepods-render/pro2/airpods_pro_2.blend --python-exit-code 1 \
  --python tools/airpods_pro_2/verify_rig.py -- ../librepods-render/pro2/rig-verification.json
for mode in connected island; do
  blender -b --python-exit-code 1 --python tools/airpods_pro_2/render.py -- \
    --model ../librepods-render/pro2/airpods_pro_2.blend \
    --out ../librepods-render/pro2/$mode --mode "$mode" --frames 360 --samples 64
done
for mode in assembly buds case left right; do
  blender -b --python-exit-code 1 --python tools/airpods_pro_2/render.py -- \
    --model ../librepods-render/pro2/airpods_pro_2.blend \
    --out ../librepods-render/pro2/$mode --mode "$mode" --frames 1 --samples 96
done
python3 tools/apple_artwork/calibrate_popup.py --tools tools/airpods_pro_2 \
  --work ../librepods-render/pro2 --frame ../librepods-render/pro2/connected/0000.png
python3 tools/airpods_pro_2/encode.py --renders ../librepods-render/pro2 \
  --out ../librepods-render/pro2/assets
python3 tools/airpods_pro_2/verify_assets.py --renders ../librepods-render/pro2 \
  --assets ../librepods-render/pro2/assets --report ../librepods-render/pro2/asset-verification.json
```

Use `--preview` to render four quarter-turns, `--mode hinge --preview --frames 120`
for lid inspection, and `--save-scene /path/to/studio.blend` to preserve a studio.
`rig-verification.json` records independence, hinge round-trip and dimensions;
`asset-verification.json` records every frame's margins, loop checks, video
metadata and complete decode checks, PNG transparency, and asset hashes.

## Validation — 2026-09-05

All 720 frames passed clipping and loop checks. Minimum margins are 44 px for
connection and 47 px for island. First-frame silhouette centers are 0.19381,
0.39476 and 0.70286 of video width, matching the Pro 3 composition and existing
battery-ring layout. All three videos fully decode and have the expected full-range
BT.709 metadata, frame count, rate and background colors. All five stills are RGBA.

Android `assembleFossDebug`, `testFossDebugUnitTest` (49 tests, no failures/errors)
and `lintVitalFossRelease` passed. The APK contains exactly eight Pro 2 resources,
with video bytes matching the generated files; it retains eight Pro 3 and eight
AirPods 4 resources. No connected-device UI run was performed.

## Rear hardware correction

A subsequent review against the real-product rear photograph from
[HeadphonesAddict](https://headphonesaddict.com/apple-airpods-pro-2-usb-c-review/)
identified patchy hinge shading and an excessively dimensional pairing button.
The shared `tools/apple_artwork/rear.py` now disables the two overlapping AR
hinge overlays and two button overlays, assigns consistent satin metal without
baked normal maps, and preserves flat hardware normals. Excluded meshes remain
in the source rig for provenance, but do not contribute to rendering.

The source button center and diameter are retained. Its face is rebuilt flush
with a 0.03 mm bevel. The case aperture's front edges are creased so subdivision
does not pull the surrounding flat panel inward and create a dark, recessed ring.
The third-party photo is a visual reference only; it is neither shipped nor used
as a texture. `--mode rear` and `--mode rear-quarter` reproduce inspection views.


## Final popup-matched revision

This revision supersedes the earlier Pro 3-derived composition and generic color
values above. `popup-layout.json` now pins product-specific connected-popup bounds,
phase, camera and spacing. `color-calibration.json` measures case RGB percentiles
from that screenshot; `tools/apple_artwork/calibrate_popup.py` reproduces the grade.
The plastic has 0.18 roughness, 0.15 coat weight and a tall softbox reflection.
All three videos and five stills use the same final studio and grade.

Only the earbud shells retain subdivision. Case and lid vertices and authored
normals are preserved, preventing artificial rounded seams and raised lanyard
surrounds. The flush button and consistent hinge finish remain enabled. Earlier
four-shell refinement descriptions are historical, not the final configuration.

Final rendering uses 128 samples per video frame and 192 samples for stills.
The connection studio is saved separately with its actual lights/materials.
A screenshot constrains one phase, not Apple's full original animation timing;
the existing six-second turntable is retained. Reference images are calibration
inputs only and are not shipped or applied as textures.

Final validation: all 720 frames and eight outputs passed. Android assemble,
54 unit tests and release lint passed; APK contains eight airpods_pro_2 resources.
