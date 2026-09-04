"""Verify full frame sequences, loop boundaries and encoded Android resources."""
import argparse
import hashlib
import json
import subprocess
from pathlib import Path

import numpy as np
from PIL import Image

p = argparse.ArgumentParser()
p.add_argument('--renders', type=Path, required=True)
p.add_argument('--assets', type=Path, required=True)
p.add_argument('--report', type=Path, required=True)
a = p.parse_args()
report = {'sequences': {}, 'videos': {}, 'stills': {}}
for mode, expected_size in [('connected', (1050, 354)), ('island', (418, 418))]:
    frames = sorted((a.renders / mode).glob('*.png'))
    assert [f.stem for f in frames] == [f'{i:04d}' for i in range(360)]
    margins = []; differences = []; first = previous = None
    for f in frames:
        im = Image.open(f).convert('RGBA')
        assert im.size == expected_size
        rgba = np.array(im)
        y, x = np.where(rgba[:, :, 3] > 16)
        assert len(x), f'Empty: {f}'
        margins.append(int(min(x.min(), y.min(), im.width - 1 - x.max(), im.height - 1 - y.max())))
        assert margins[-1] > 1, f'Clipped: {f}'
        # Compare premultiplied colors so transparent RGB cannot distort the check.
        visible = rgba[:, :, :3].astype(np.float32) * (rgba[:, :, 3:] / 255)
        if previous is not None: differences.append(float(np.abs(visible - previous).mean()))
        if first is None: first = visible
        previous = visible
    seam_delta = float(np.abs(previous - first).mean())
    assert seam_delta < max(differences) * 1.5, (mode, seam_delta, max(differences))
    report['sequences'][mode] = {'frames': 360, 'minimum_margin_px': min(margins),
                                 'loop_delta': seam_delta, 'maximum_adjacent_delta': max(differences)}

layout = json.loads(Path(__file__).with_name('popup-layout.json').read_text())
mask = np.array(Image.open(a.renders / 'connected/0000.png'))[:, :, 3] > 16
columns = np.where(mask.any(axis=0))[0]
groups = np.split(columns, np.where(np.diff(columns) > 1)[0] + 1)
assert len(groups) == 3
centers = [(int(g[0]) + int(g[-1])) / 2 / 1050 for g in groups]
measured = []
for cols in [np.concatenate(groups[:2]), groups[2]]:
    ys = np.where(mask[:, cols].any(axis=1))[0]
    measured.append([(int(cols[-1])-int(cols[0])+1)/1050, (int(ys[-1])-int(ys[0])+1)/1050])
expected = layout['target_bounds_normalized']
actual = measured[1] + measured[0]
card_width = layout['popup_card_x'][1] - layout['popup_card_x'][0]
residual = [(x-y)*card_width for x,y in zip(actual,expected)]
assert max(map(abs,residual)) < 5, residual
report['popup_measurement'] = {'silhouette_centers': centers, 'dimension_residual_reference_px': residual,
                               'reference': layout['reference_filename']}

for name, size, bg in [('connected', (1050, 354), (255, 255, 255)),
                       ('connected_night', (1050, 354), (29, 28, 31)),
                       ('island', (418, 418), (0, 0, 0))]:
    f = a.assets / f'airpods_4_{name}.mp4'
    probe = json.loads(subprocess.check_output(['ffprobe', '-v', 'error', '-show_streams', '-show_format', '-of', 'json', str(f)]))
    assert len(probe['streams']) == 1
    stream = probe['streams'][0]
    assert stream['codec_name'] == 'h264'
    assert (stream['width'], stream['height']) == size
    assert stream['r_frame_rate'] == '60/1' and int(stream['nb_frames']) == 360
    assert stream['color_range'] == 'pc' and stream['color_space'] == 'bt709'
    assert stream['color_primaries'] == 'bt709' and stream['color_transfer'] == 'bt709'
    assert abs(float(probe['format']['duration']) - 6) < .01
    # Decode the entire stream, then inspect a decoded RGB corner for range mistakes.
    subprocess.run(['ffmpeg', '-v', 'error', '-xerror', '-i', str(f), '-f', 'null', '-'], check=True)
    raw = subprocess.check_output(['ffmpeg', '-v', 'error', '-i', str(f), '-frames:v', '1', '-pix_fmt', 'rgb24', '-f', 'rawvideo', '-'])
    decoded = np.frombuffer(raw, dtype=np.uint8).reshape(size[1], size[0], 3)
    corner = decoded[:8, :8].mean(axis=(0, 1))
    assert np.abs(corner - bg).max() <= 2, (name, corner, bg)
    report['videos'][name] = {'size': size, 'frames': 360, 'fps': 60, 'seconds': 6,
                               'decoded_background_rgb': corner.tolist(),
                               'sha256': hashlib.sha256(f.read_bytes()).hexdigest()}

for suffix in ['', '_buds', '_case', '_left', '_right']:
    f = a.assets / f'airpods_4{suffix}.png'
    im = Image.open(f)
    assert im.mode == 'RGBA'
    alpha = np.array(im)[:, :, 3]
    assert alpha.min() == 0 and alpha.max() == 255
    assert im.getchannel('A').getbbox() is not None
    report['stills'][f.name] = {'size': im.size, 'alpha_bounds': im.getchannel('A').getbbox(),
                               'sha256': hashlib.sha256(f.read_bytes()).hexdigest()}
a.report.write_text(json.dumps(report, indent=2) + '\n')
print(json.dumps(report, indent=2))
