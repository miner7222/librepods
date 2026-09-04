# AirPods 4 — official-source Blender artwork

This replaces the Falah3D CC BY AirPods 4 images and videos with renders from
Apple's official AirPods 4 AR USDZ. The mesh and textures are Apple-derived;
this is not a model created from scratch. The source is pinned by URL and SHA-256
in `provenance.json`, together with six official reference photographs.

Sources: [Apple AirPods 4](https://www.apple.com/airpods-4/) and
[technical specifications](https://www.apple.com/airpods-4/specs/).
The fetched AR model includes the charging-case speaker. As before, the standard
and ANC AirPods 4 classes share the same artwork resources.

The new files belong to `android/app/src/main/res-apple/`. They are not assigned
CC BY or GPL rights. `replaced-artwork.json` preserves the former asset's credit
and license as historical provenance. That obsolete credit is removed from the
app's shipped AboutLibraries data after the CC BY artwork is replaced.

## Deliverables and model controls

Local workspace: `/Users/jsp/Git/librepods-render/a4-apple/`.

- `airpods_4.blend`: editable model with packed textures and opening/closing action.
- `airpods_4_studio.blend`: connection rendering camera, lights and rig.
- `refs/`: official USDZ and reference photos, plus previous-render comparisons.
- `assets/`: three MP4 videos and five transparent PNGs.
- `rig-verification.json`, `asset-verification.json`: verification results.

| Handle | Parts and behavior |
|---|---|
| `H_Left` | 15 meshes; complete left unit, independently movable |
| `H_Right` | 15 meshes; complete right unit, independently movable |
| `H_BudPair` | Shared turntable parent used by app animations |
| `H_Case` | 27 fixed body meshes; `lid_open_degrees` property |
| `Lid_Hinge` | 7 lid meshes, including liner and moving hinge hardware |

The timeline opens and closes the lid over frames 1–160. The property spans
0° (closed) to 115° (fully open). Clear `H_Case`'s action to pose it manually.
Python changes to the property require `case.update_tag(refresh={'OBJECT'})`
before updating the view layer. The UI updates it automatically.

The actual source pin center is `(0, 0.009603270, 0.010934789)` meters.
The source lid is authored 115° open. The fixed hinge plate and pin are grouped
with the body; only the moving hardware belongs to the lid.

Four outer shells use one level of non-destructive subdivision and recalculated
normals to remove the AR mesh's visible surface bands. Their material has
roughness 0.28 and a light clear coat. Sensors, vents, contacts, texture inserts
and interior parts retain their source geometry. Evaluated closed-case bounds
are 50.214 × 21.232 × 46.060 mm, within 0.4 mm of Apple's 50.1 × 21.2 × 46.2 mm.

## App resources

All eight existing resource names are retained, so AirPods 4, AirPods 4 ANC and
the existing standard-generation fallback mapping keep resolving correctly.
The old files in `res/` are removed when the new `res-apple/` files are installed;
there must be no duplicate resource name/qualifier combinations.

| Resource under `res-apple/` | Output |
|---|---|
| `raw/airpods_4_connected.mp4` | 1050×354; white `#ffffff` |
| `raw-night/airpods_4_connected.mp4` | 1050×354; dark `#1d1c1f` |
| `raw/airpods_4_island.mp4` | 418×418; black |
| `drawable/airpods_4.png` | Open case and floating units, RGBA |
| `drawable/airpods_4_buds.png` | Paired units, RGBA with settings margins |
| `drawable/airpods_4_case.png` | Closed case, 805×805 RGBA |
| `drawable/airpods_4_left.png`, `..._right.png` | Independent units, tightly framed RGBA |

Videos remain 6 seconds / 360 frames / 60 fps, H.264, full-range BT.709 with
explicit H.264 VUI metadata and faststart. The pair rotates about a shared axis;
the case rotates about its own center. The first and last frames differ by 1°.

The connection camera keeps the previous 0.158284 m orthographic width and 12°
elevation. The first-frame unit silhouettes remain near 0.2281 and 0.36095 of
video width. The inward-leaning bulbs' stems preserve the previous clip's
positions (approximately 0.2024 and 0.3862), and the app's ring layout is unchanged.
Silhouette centers are not interchangeable with stem centers.
The case remains near 0.7035. Island framing matches the prior clip's
approximately 266×198 px product bounds. A shared color grade preserves black
inserts while bringing white surfaces near 235/255 in both themes.

## Reproduce

Requires Blender 5.2 (tested 5.2.1), Python with numpy/Pillow and ffmpeg.
Run from the repository root:

```bash
python3 tools/airpods_4/fetch.py --out ../librepods-render/a4-apple/refs
blender -b --python-exit-code 1 --python tools/airpods_4/build.py -- \
  --source ../librepods-render/a4-apple/refs/apple_airpods_4.usdz \
  --out ../librepods-render/a4-apple/airpods_4.blend
blender -b ../librepods-render/a4-apple/airpods_4.blend --python-exit-code 1 \
  --python tools/airpods_4/verify_rig.py -- ../librepods-render/a4-apple/rig-verification.json

for mode in connected island; do
  blender -b --python-exit-code 1 --python tools/airpods_4/render.py -- \
    --model ../librepods-render/a4-apple/airpods_4.blend \
    --out ../librepods-render/a4-apple/$mode --mode "$mode" --frames 360 --samples 64
done
for mode in assembly buds case left right; do
  blender -b --python-exit-code 1 --python tools/airpods_4/render.py -- \
    --model ../librepods-render/a4-apple/airpods_4.blend \
    --out ../librepods-render/a4-apple/$mode --mode "$mode" --frames 1 --samples 96
done
python3 tools/airpods_4/encode.py \
  --renders ../librepods-render/a4-apple --out ../librepods-render/a4-apple/assets
python3 tools/airpods_4/verify_assets.py \
  --renders ../librepods-render/a4-apple --assets ../librepods-render/a4-apple/assets \
  --report ../librepods-render/a4-apple/asset-verification.json
```

Use `--preview` for four quarter-turns and `--save-scene /path/to/studio.blend`
to save a render studio. `--mode hinge --preview --frames 120` produces closed,
half-open and fully open inspection views. The Blender MCP was used to inspect
the interactive model in addition to headless rendering.

## Validation — 2026-09-05

All 720 transparent animation frames were checked. Minimum margins are 11 px
for connection and 72 px for island; loop-boundary changes are smaller than the
largest ordinary adjacent-frame changes. All three MP4s fully decode, contain
360 frames at 60 fps, and carry full-range BT.709 metadata. Still images have
transparent RGBA backgrounds. Exact hashes and measurements are recorded in
`asset-verification.json` and `rig-verification.json`.

The Android `assembleFossDebug` and `lintVitalFossRelease` tasks passed.
`testFossDebugUnitTest` is up to date with 49 passing tests, zero failures/errors.
The APK contains eight AirPods 4 assets and retains all eight Pro 3 assets.
No old AirPods 4 resource duplicates or obsolete Falah3D/CC BY AboutLibraries
entry remain in the APK. A connected-device UI run was not performed.


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
python3 tools/airpods_4/calibrate_popup.py --tools tools/airpods_4 \
  --work ../librepods-render/a4-apple --frame ../librepods-render/a4-apple/popup-preview/0000.png
python3 tools/airpods_4/compare_popup.py --tools tools/airpods_4 --work ../librepods-render/a4-apple
```

Use 128 samples for video and 192 samples for stills. Every frame is checked for
clipping; first-frame bounds must be within five source-image pixels of the
measured popup, including mesh and antialiasing differences. Full-stream decode,
color range/BT.709 metadata, duration, loop boundary and RGBA checks still apply.

Reference: the user-provided original
`/Users/jsp/Git/librepods-ref/ios27/orig/C125D2F4-D5C8-483E-B439-29323B205A0B.png`.
Copy it unchanged to `refs/connected-popup.png` before recalibration. The other
original popup (`EE500C6F-6A9F-4ABA-8C51-7CFB3E2B75CB.png`) provides a second
rotation-phase visual cross-check. Local screenshots are not included in the app.

Final refreshed asset validation: all 720 frames and all eight outputs passed.
Connection/island minimum margins are 17/72 px. Popup product bounds differ
by at most 2.93 source pixels.
