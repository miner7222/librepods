"""Grade transparent Blender output and encode 60 fps, full-range BT.709 clips.

python3 encode.py --renders /path/to/a3-apple --out /path/to/a3-apple/assets
Requires numpy, Pillow, and ffmpeg. Keeps source renders untouched.
"""
import argparse
import json
import subprocess
import tempfile
from pathlib import Path

import numpy as np
from PIL import Image

p = argparse.ArgumentParser()
p.add_argument('--renders', type=Path, required=True)
p.add_argument('--out', type=Path, required=True)
p.add_argument('--stills-only', action='store_true')
a = p.parse_args()
a.out.mkdir(parents=True, exist_ok=True)
lut = np.array(json.loads(Path(__file__).with_name('grade.json').read_text()), dtype=np.uint8)

def grade(path):
    rgba = np.array(Image.open(path).convert('RGBA'))
    rgba[:, :, :3] = np.stack([lut[c][rgba[:, :, c]] for c in range(3)], axis=2)
    return rgba

video_specs = [] if a.stills_only else [('connected', 'connected', (255, 255, 255)),
                       ('connected', 'connected_night', (29, 28, 31)),
                       ('island', 'island', (0, 0, 0))]
for mode, name, bg in video_specs:
    frames = sorted((a.renders / mode).glob('*.png'))
    assert len(frames) == 360, f'{mode}: expected 360 frames, got {len(frames)}'
    assert [f.stem for f in frames] == [f'{i:04d}' for i in range(360)]
    with tempfile.TemporaryDirectory(prefix='a3-apple-encode-') as tmp:
        for frame in frames:
            rgba = grade(frame)
            alpha = rgba[:, :, 3:] / 255.0
            rgb = np.rint(rgba[:, :, :3] * alpha + np.array(bg) * (1 - alpha)).astype(np.uint8)
            Image.fromarray(rgb).save(Path(tmp) / frame.name)
        subprocess.run(['ffmpeg', '-v', 'error', '-y', '-framerate', '60', '-i', f'{tmp}/%04d.png',
                        '-vf', 'scale=in_range=full:out_range=full:out_color_matrix=bt709,format=yuv420p',
                        '-c:v', 'libx264', '-preset', 'slow', '-crf', '17',
                        '-color_range', 'pc', '-colorspace', 'bt709', '-color_primaries', 'bt709',
                        '-color_trc', 'bt709',
                        # Some ffmpeg builds inherit unspecified PNG transfer
                        # metadata; pin the H.264 VUI as well as container tags.
                        '-bsf:v', 'h264_metadata=video_full_range_flag=1:colour_primaries=1:transfer_characteristics=1:matrix_coefficients=1',
                        '-movflags', '+faststart',
                        str(a.out / f'airpods_3_{name}.mp4')], check=True)
        print('ENCODED', name, flush=True)

for mode, suffix in [('assembly', ''), ('buds', '_buds'), ('case', '_case'), ('left', '_left'), ('right', '_right')]:
    rgba = grade(a.renders / mode / '0000.png')
    image = Image.fromarray(rgba)
    if mode in ['buds', 'left', 'right']:
        # Match the existing settings artwork's useful content area, rather
        # than letting a large transparent square shrink the product in Fit.
        bounds = image.getchannel('A').getbbox()
        product = image.crop(bounds)
        if mode == 'buds':
            canvas = Image.new('RGBA', (round(product.width / .78), round(product.height / .72)))
        else:
            canvas = Image.new('RGBA', (product.width + 4, product.height + 4))
        canvas.paste(product, ((canvas.width - product.width) // 2, (canvas.height - product.height) // 2))
        image = canvas
    image.save(a.out / f'airpods_3{suffix}.png')
    print('STILL', mode, flush=True)
