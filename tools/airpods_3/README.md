# AirPods 3 — official-source Blender artwork

AirPods 3 now has its own connection videos, island video and settings images,
replacing its previous reference to the AirPods 4 fallback set. The new assets
use Apple's official 2021 AirPods 3 MagSafe AR geometry and textures. This is an
Apple-derived model with a LibrePods rig and studio, not geometry made from scratch.

The source URL was recovered from an archived Apple product page and fetched
directly from Apple's server. `provenance.json` pins the USDZ and two official
reference photographs by SHA-256. Sources:
[Apple announcement](https://www.apple.com/newsroom/2021/10/introducing-the-next-generation-of-airpods/),
[technical specifications](https://support.apple.com/en-us/111863), and
[archived page containing the AR link](https://web.archive.org/web/20230103214237/https://www.apple.com/airpods-pro/).

## Resources and composition

All eight files reside in `android/app/src/main/res-apple/`. No CC BY or GPL
rights are claimed for this Apple-derived artwork. Both AirPods 3 model numbers
(A2565 and A2564), and the overlay's AirPods 3 broadcast-name lookup, use the
new resources. AirPods 1 and 2 retain the AirPods 4 fallback.

| Resource | Output |
|---|---|
| `raw/airpods_3_connected.mp4` | 1050×354; white background |
| `raw-night/airpods_3_connected.mp4` | 1050×354; #1d1c1f background |
| `raw/airpods_3_island.mp4` | 418×418; black background |
| `drawable/airpods_3.png` | Open case and floating units, RGBA |
| `drawable/airpods_3_buds.png` | Paired units with settings margins, RGBA |
| `drawable/airpods_3_case.png` | Closed case, 805×805 RGBA |
| `drawable/airpods_3_left.png`, `..._right.png` | Independent units, RGBA |

Videos are 6 seconds, 360 frames, 60 fps, H.264 with full-range BT.709 metadata
and faststart. The pair and case rotate about separate centers. The connection
camera uses the Pro 3/Pro 2 composition: 0.2 m orthographic width, 10° elevation,
case X=0.0407 m, pair X=-0.04106 m, unit offsets ±0.02008 m. AirPods 3 keeps its
original physical size. Island rendering uses 0.060 m width and unit offsets
±0.012 m. The neutral grade is calibrated from the new front render to the
same white-surface target as Pro 3, preserving dark sensors.

## Editable model

Local workspace: `/Users/jsp/Git/librepods-render/a3-apple/`.
`airpods_3.blend` contains the independent rig, packed textures and lid action.
`airpods_3_studio.blend` preserves the connection camera and lighting.
`assets/` contains the final eight app files. Raw frames, source references and
inspection previews remain in the render workspace.

| Handle | Contents |
|---|---|
| `H_Left`, `H_Right` | 9 meshes each; complete independently movable units |
| `H_BudPair` | Shared animation parent |
| `H_Case` | 21 body meshes and `lid_open_degrees` property |
| `Lid_Hinge` | 4 moving lid meshes, including liner and hinge |

The source places each outer earbud shell under the opposite unit root. The
build script corrects those two memberships explicitly before creating handles.
The lid's 110° open pose is baked into vertices. Its pivot is reconstructed by
aligning the rotated lid rim to the case seam with 0.05 mm clearance. The derived
axis passes through approximately (0, 0.00969661, 0.01526236) m; it is not an
original animated transform. Timeline frames 1–160 demonstrate opening/closing.
Clear `H_Case`'s action for manual posing, then use `lid_open_degrees` (0–110).
Python edits need `case.update_tag(refresh={'OBJECT'})` and a view-layer update.

Four outer shells have one non-destructive subdivision level and recalculated
normals. Plastic uses physical studio illumination instead of baked AR emission
and roughness; sensor, grill, contact and engraving materials retain source detail.
Evaluated closed-case bounds are 54.222×21.523×46.071 mm, within 0.4 mm of Apple's
54.4×21.38×46.4 mm specification. All 13 imported file textures are packed.

`prepare.py` removes only the competing MaterialX output from a derivative
copy, allowing Blender 5.2 to import the source's existing USD Preview Surface
networks and textures correctly. The original USDZ remains untouched.

## Reproduce

Requires Blender 5.2, Python with numpy/Pillow, ffmpeg, and usd-core for source
preparation (shown with `uv`). Run from the repository root:

```bash
python3 tools/airpods_3/fetch.py --out ../librepods-render/a3-apple/refs
uv run --with usd-core python tools/airpods_3/prepare.py \
  --source ../librepods-render/a3-apple/refs/apple_airpods_3.usdz \
  --out ../librepods-render/a3-apple/prepared
blender -b --python-exit-code 1 --python tools/airpods_3/build.py -- \
  --source ../librepods-render/a3-apple/prepared/preview-surface.usdc \
  --out ../librepods-render/a3-apple/airpods_3.blend
blender -b ../librepods-render/a3-apple/airpods_3.blend --python-exit-code 1 \
  --python tools/airpods_3/verify_rig.py -- ../librepods-render/a3-apple/rig-verification.json
for mode in connected island; do
  blender -b --python-exit-code 1 --python tools/airpods_3/render.py -- \
    --model ../librepods-render/a3-apple/airpods_3.blend \
    --out ../librepods-render/a3-apple/$mode --mode "$mode" --frames 360 --samples 64
done
for mode in assembly buds case left right; do
  blender -b --python-exit-code 1 --python tools/airpods_3/render.py -- \
    --model ../librepods-render/a3-apple/airpods_3.blend \
    --out ../librepods-render/a3-apple/$mode --mode "$mode" --frames 1 --samples 96
done
python3 tools/apple_artwork/calibrate_popup.py --tools tools/airpods_3 \
  --work ../librepods-render/a3-apple --frame ../librepods-render/a3-apple/connected/0000.png
python3 tools/airpods_3/encode.py --renders ../librepods-render/a3-apple \
  --out ../librepods-render/a3-apple/assets
python3 tools/airpods_3/verify_assets.py --renders ../librepods-render/a3-apple \
  --assets ../librepods-render/a3-apple/assets --report ../librepods-render/a3-apple/asset-verification.json
```

Use `--preview` for four quarter-turns, `--mode hinge --preview --frames 120`
for lid inspection, and `--save-scene /path/to/studio.blend` to preserve a studio.

## Rear hardware correction

The actual-product rear photograph from
[ChargerLAB's teardown](https://www.chargerlab.com/teardown-of-airpods-3-charging-case/)
shows a nearly flush pairing button and uniformly finished metal hinge. The shared
`tools/apple_artwork/rear.py` excludes the duplicate moving-hinge overlay and
uses a consistent satin metal material with flat hardware normals, without AR
normal/emission maps. The excluded mesh remains in the editable rig for provenance.

The button retains its original center and diameter, with a flush face and 0.03 mm
bevel. Creased front edges around the case aperture prevent subdivision from
pulling the surrounding panel inward. The result has a thin outline instead of
an exaggerated recessed ring. The third-party photo is reference-only, not shipped
or used as a texture. `--mode rear` and `--mode rear-quarter` render inspection views.


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
54 unit tests and release lint passed; APK contains eight airpods_3 resources.
