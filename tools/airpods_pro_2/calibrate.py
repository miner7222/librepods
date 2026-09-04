"""Calibrate the shared neutral grade from the final connection front render.

python3 tools/airpods_pro_2/calibrate.py --frame /path/to/connected/0000.png
White surfaces target the Pro 3 pipeline's 215/235/244 percentiles. Previous
Pro 2 artwork is not used. Dark sensors remain dark through fixed low anchors.
"""
import argparse
import json
from pathlib import Path
import numpy as np
from PIL import Image

p = argparse.ArgumentParser()
p.add_argument('--frame', type=Path, required=True)
a = p.parse_args()
rgba = np.array(Image.open(a.frame).convert('RGBA'))
white = rgba[:, :, :3][(rgba[:, :, 3] > 250) & (rgba[:, :, :3].min(axis=2) > 150)]
percentiles = np.percentile(white, [10, 50, 90], axis=0)
lut = []
for channel in range(3):
    x = [0, 80, 120, 150, *percentiles[:, channel], 255]
    assert all(b > a for a, b in zip(x, x[1:]))
    lut.append(np.rint(np.interp(np.arange(256), x, [0, 80, 130, 175, 215, 235, 244, 255])).astype(int).tolist())
Path(__file__).with_name('grade.json').write_text(json.dumps(lut, indent=2) + '\n')
print('Raw white p10/p50/p90:', percentiles.tolist())
