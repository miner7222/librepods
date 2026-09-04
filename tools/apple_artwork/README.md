# Shared Apple artwork presentation

`presentation.json` and `studio.py` define the production presentation for AirPods
3, 4, Pro 1, Pro 2 and Pro 3. Model-specific builds retain Apple's geometry,
independent left/right units and operable case lids. AirPods 1/2 keep their
existing AirPods 4 fallback.

## Still and moving states

The user identified two states in one AirPods 4 iPhone capture set. Wide units
belong to settings and popup stills. Compact units belong only to connected and
island movies. Do not infer state from a single screenshot or reuse a per-model
historical screenshot fit as its spacing target.

Both connected movies and layered stills use 1050 × 354, a 152.2 mm orthographic
width and a front camera. Their assembly centers are measured separately from
the compact and wide popup captures. Settings uses the same wide still layers
and scale as the popup. The settings reference's smaller unit scale is not
preserved. Measurements and source filenames are in `presentation.json`.

Case closed X/Y/Z extents are normalized to published dimensions. Each unit
receives one uniform scale based on its canonical height; its authored X/Y
proportions are retained because published width/depth axes differ from the
posed AR mesh. Independent axis stretching would deform silicone tips.
AirPods 3/4 unit center distance is canonical projected width plus 1.5 mm for
movies or 12.7 mm for stills. Movies retain that compact rule for Pro models
as well. Those are canonical-frame clearances, not guaranteed silhouette gaps
at every rotation. The island shares compact spacing.

After reviewing the shared preview, the user supplied explicit wide still
references for Pro 1, 2 and 3. `pro_still_overrides` takes precedence for those
stills: measure the visible unit-to-unit and unit-to-case edge gaps relative to
the known case width. Both gaps are about 9–10 mm. Pro 1/2 old-iOS captures are
spacing references only; current unit directions are retained. Position the
whole silhouette around the canvas center, preserving metric dimensions and
camera scale. This avoids squeezing Pro cases against the right unit.

Every movie starts at front 0°, lasts six seconds at 60 fps and makes a complete
turn. `--reference-phase` is inspection-only; omit it for production. Historical
per-model `popup-layout.json` is only used for this comparison angle.

## Surface and lighting

`material-references.json` records the additional official Apple multi-angle
reference set. Existing per-model provenance includes OS popup and rear product
references. Product pages sometimes use dark, dramatic lighting; those images
inform surface shape and reflection width, while OS captures inform brightness.

The shared studio uses broad diffuse lights and two soft reflection cards.
Cards affect glossy rays only, so their energy does not wash out the diffuse
surface. White world strength is 1.2; card energies are 1.3 and 1.1 W.
White plastic is nonmetallic, roughness 0.24 with a restrained 0.05 clear coat.
Silicone remains separate and matte. Standard view transform exposure is -0.6;
the per-model LUTs are identity. No screenshot percentile curve compensates for
a lighting mismatch. Pro 1 retains the corrected flush button and planar hinge.

## Render, encode, verify

Use each model's documented source acquisition/build commands. Render connected
and island with 360 frames and 128 samples, then assembly/buds/case/left/right
with one frame and 192 samples. Run its `encode.py` and `verify_assets.py`.
Store intermediate frames and packed blends outside this repository.

Each model yields three MP4s and six PNGs. `_buds` and `_case` are transparent
full-canvas layers; never crop them or fit them into separate weighted columns.
`_left`, `_right` and `_case_icon` are standalone Bluetooth metadata icons.
The assembly image retains its open-case presentation.

The app overlays the two still layers in both settings and popup. Individual
rings follow each unit silhouette center. The popup's combined/case centers
switch with the artwork state; settings always uses still centers. The upstream
battery-state selection and video crossfade remain intact.

Asset verification checks all 720 rendered frames, clipping, loop continuity,
full video decoding, duration/range/color metadata, transparent PNGs and shared
still/motion geometry. `verify_presentation.py` checks the measured case shift
and wide-minus-compact spacing independently of color.
