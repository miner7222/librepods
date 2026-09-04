# AirPods Pro 3 artwork

Apple's official AirPods Pro 3 product photographs and AR USDZ are the source.
The USDZ already contains the actual Pro 3 geometry, silicone tips, sensors,
case interior and hinge. This work converts it into an editable Blender rig;
it is **not independently authored geometry or a photogrammetric reconstruction**.
The LibrePods additions are the hierarchy, hinge control, animation, studio,
camera, color grade and Android resource mapping.

Source pages: [Apple product page](https://www.apple.com/airpods-pro/) and
[technical specifications](https://www.apple.com/airpods-pro/specs/).
Exact asset URLs and SHA-256 hashes are in `provenance.json`.
Apple-derived geometry, textures and rendered images are not claimed to be
CC BY or GPL assets. They are kept in the app's existing `res-apple` source
directory. AirPods 4 has subsequently been replaced with Apple-derived artwork
as well; its separate provenance is in `../airpods_4/README.md`.

## Blender files and controls

Local working directory: `/Users/jsp/Git/librepods-render/pro3/`.

- `airpods_pro_3.blend`: packed, editable model; 55 meshes.
- `airpods_pro_3_studio.blend`: the same model with the connection camera and studio lights.
- `refs/`: official USDZ and eight official image references.
- `assets/`: graded PNGs and encoded MP4s.
- `rig-verification.json`: articulation and dimension checks.

The Blender model uses meters, with unit-scale controls:

| Control | Function |
|---|---|
| `H_Left` | Complete left unit: 16 meshes; independent translation and rotation |
| `H_Right` | Complete right unit: 16 meshes; independent translation and rotation |
| `H_BudPair` | Optional shared parent for the app's paired turntable motion |
| `H_Case` | Body: 18 meshes; custom property `lid_open_degrees` |
| `Lid_Hinge` | Lid: 5 meshes, including liner and moving hinge hardware |

The case property is 0 when closed and 115 when fully open. The main model's
`Case_Open_Close` action demonstrates the motion over frames 1–160. Clear that
action to pose the property manually. When changing the custom property via
Python, call `H_Case.update_tag(refresh={'OBJECT'})` before evaluating the view
layer so Blender refreshes the driver. The UI updates it automatically.

The hinge comes from the source pin mesh, at `(0, 0.009924771, 0.001151885)`
in source world meters. The source lid is closed by rotating 115° around that
pin, then driven relative to the closed pose. Closed case bounds measured from
vertices are 62.047 × 21.871 × 47.298 mm (X/Y/Z), within 0.4 mm of Apple's
62.2 × 21.8 × 47.2 mm published dimensions. No shell deformation is applied.

## Android outputs

| Resource | Format |
|---|---|
| `raw/airpods_pro_3_connected.mp4` | 1050×354, white `#ffffff` |
| `raw-night/airpods_pro_3_connected.mp4` | 1050×354, dark `#1d1c1f` |
| `raw/airpods_pro_3_island.mp4` | 418×418, black |
| `drawable/airpods_pro_3.png` | Open case and floating separate units, RGBA |
| `drawable/airpods_pro_3_buds.png` | Both units, RGBA; settings-screen margins |
| `drawable/airpods_pro_3_case.png` | Closed case, 805×805 RGBA |
| `drawable/airpods_pro_3_left.png`, `..._right.png` | Individual units, RGBA |

All paths above are relative to `android/app/src/main/res-apple/`. Videos are
6 seconds, 360 frames, 60 fps, H.264, yuv420p, explicitly converted and tagged
full-range BT.709, with faststart. They contain no audio. The two units turn
together through `H_BudPair`, matching the current app motion; the case turns
about its own axis with the lid closed. The underlying units remain independent.
The final frame is 359°, so the loop advances a single degree into frame 0.

The connection layout preserves the app's existing ring centers (approximately
0.1943, 0.3951 and 0.7035 of video width). `AirPodsPro3` resolves its own five
images and two video IDs; Android selects the night variant through qualifiers.
Pro 1 retains its existing Pro 2 fallback.

## Reproduce

Requires Blender 5.2 (tested: 5.2.1), Python 3 with Pillow/numpy, and ffmpeg.
Run from the repository root. The scripts use Blender's USD importer directly;
the connected Blender MCP was also used for interactive inspection.

```bash
python3 tools/airpods_pro_3/fetch.py --out ../librepods-render/pro3/refs
blender -b --python-exit-code 1 --python tools/airpods_pro_3/build.py -- \
  --source ../librepods-render/pro3/refs/apple_airpods_pro_3.usdz \
  --out ../librepods-render/pro3/airpods_pro_3.blend
blender -b ../librepods-render/pro3/airpods_pro_3.blend --python-exit-code 1 \
  --python tools/airpods_pro_3/verify_rig.py -- ../librepods-render/pro3/rig-verification.json

for mode in connected island; do
  blender -b --python-exit-code 1 --python tools/airpods_pro_3/render.py -- \
    --model ../librepods-render/pro3/airpods_pro_3.blend \
    --out ../librepods-render/pro3/$mode --mode "$mode" --frames 360 --samples 64
done
for mode in assembly buds case left right; do
  blender -b --python-exit-code 1 --python tools/airpods_pro_3/render.py -- \
    --model ../librepods-render/pro3/airpods_pro_3.blend \
    --out ../librepods-render/pro3/$mode --mode "$mode" --frames 1 --samples 96
done
python3 tools/airpods_pro_3/encode.py \
  --renders ../librepods-render/pro3 --out ../librepods-render/pro3/assets
python3 tools/airpods_pro_3/verify_assets.py \
  --renders ../librepods-render/pro3 --assets ../librepods-render/pro3/assets \
  --report ../librepods-render/pro3/asset-verification.json
```

For quick shape checks, pass `--preview` to render only four quarter-turns.
For a studio `.blend`, pass `--save-scene /path/to/studio.blend` when rendering.
The shared `grade.json` is calibrated from the front render: preserve black
sensor detail, lift white surfaces to approximately 235/255, and apply a subtle
cool tint. Light and dark variants use identical product pixels before encoding.

## Validation (2026-09-05)

`asset-verification.json` records all 720 source frames, loop continuity,
decoded backgrounds, resource dimensions and output hashes. Minimum margins
are 39 px for connection and 50 px for island. Measured connection centers are
0.19381 / 0.39476 / 0.70286, matching the existing battery-ring positions.
All three MP4s decode completely and carry full-range BT.709 VUI metadata.
`rig-verification.json` records independent-unit movement, fixed-axis hinge
motion and the exact return to the closed pose. Front, side, rear and three
lid angles were also visually inspected.

Android validation passed:

```text
:app:assembleFossDebug
:app:testFossDebugUnitTest
:app:lintVitalFossRelease
```

The generated APK includes all eight Pro 3 resources. Validation was performed
on the source assets and Android build; no connected-device UI run was performed.


## Connected-popup calibration (2026-09-05 revision)

`popup-layout.json` records screenshot identity/hash, manually measured product
bounds, identified rotation phase and orthographic camera fit. The new framing
supersedes the earlier layout numbers above. `color-calibration.json` records
case RGB percentiles measured from the reference rather than a generic white
target. The case has symmetric tall reflection cards, while earbud highlights
are softer. Geometry and articulation are unchanged by this rendering revision.
Connection/island videos and all five stills are regenerated with this studio.

The first connection frame matches the reference's phase. A still screenshot
does not establish the original animation speed; the app retains its existing
six-second turntable. Remaining pixel differences reflect the source AR mesh,
lighting approximation and source screenshot compression, not an exact recovery
of Apple's original rendering scene. `popup-comparison.png` in the render
workspace shows the reference and new render at the same scale.

Recalibrate color after a studio change:

```bash
python3 tools/airpods_pro_3/calibrate_popup.py --tools tools/airpods_pro_3 \
  --work ../librepods-render/pro3 --frame ../librepods-render/pro3/popup-preview/0000.png
python3 tools/airpods_pro_3/compare_popup.py --tools tools/airpods_pro_3 --work ../librepods-render/pro3
```

Use 128 samples for video and 192 samples for stills. Every frame is checked for
clipping; first-frame bounds must be within five source-image pixels of the
measured popup, including mesh and antialiasing differences. Full-stream decode,
color range/BT.709 metadata, duration, loop boundary and RGBA checks still apply.

Reference: [Macwelt's photographed Pro 3 charging popup](https://www.macwelt.de/article/3006255/airpods-optimiert-laden-sinnvoll.html),
with [StereoGuide's Pro 3 review](https://stereoguide.de/kopfhoerer/noise-cancelling/apple-airpods-pro-3-im-test-klangwunder-mit-health-extras/)
as a dark-mode cross-check. Both show a connected battery-status popup, not the
initial pairing/open-case screen. These reference images are not shipped or used
as model textures. Plastic-shell normals are recalculated in the render scene,
without moving vertices or rounding case details.

Final refreshed asset validation: all 720 frames and all eight outputs passed.
Connection/island minimum margins are 36/50 px. Popup product bounds differ
by at most 1.12 source pixels.
