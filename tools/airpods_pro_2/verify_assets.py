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
    marker_path = a.renders / mode / 'render-complete.json'
    if marker_path.exists():
        marker = json.loads(marker_path.read_text())
        assert marker['mode'] == mode and marker['frames'] == 360, marker
        assert marker['samples'] >= 128, marker
    else:
        # Older render scripts logged each successful frame instead of a marker.
        log = (a.renders / f'{mode}.log').read_text()
        completed = [int(line.split()[-1]) for line in log.splitlines()
                     if line.startswith(f'FRAME_READY {mode} ')]
        assert completed == list(range(360)), (mode, completed)
        assert 'Blender quit' in log and 'Traceback' not in log
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

import sys
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from apple_artwork.verify_presentation import verify
report['presentation'] = verify(a.renders, 'airpods_pro_2')

for name, size, bg in [('connected', (1050, 354), (255, 255, 255)),
                       ('connected_night', (1050, 354), (29, 28, 31)),
                       ('island', (418, 418), (0, 0, 0))]:
    f = a.assets / f'airpods_pro_2_{name}.mp4'
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

for suffix in ['', '_buds', '_case', '_left', '_right', '_case_icon']:
    f = a.assets / f'airpods_pro_2{suffix}.png'
    im = Image.open(f)
    assert im.mode == 'RGBA'
    alpha = np.array(im)[:, :, 3]
    assert alpha.min() == 0 and alpha.max() == 255
    assert im.getchannel('A').getbbox() is not None
    report['stills'][f.name] = {'size': im.size, 'alpha_bounds': im.getchannel('A').getbbox(),
                               'sha256': hashlib.sha256(f.read_bytes()).hexdigest()}
a.report.write_text(json.dumps(report, indent=2) + '\n')
print(json.dumps(report, indent=2))
