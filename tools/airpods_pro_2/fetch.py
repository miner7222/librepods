"""Download the pinned official Apple source and image references."""
import argparse
import hashlib
import json
import urllib.request
from pathlib import Path

p = argparse.ArgumentParser()
p.add_argument('--out', type=Path, required=True)
a = p.parse_args()
a.out.mkdir(parents=True, exist_ok=True)
sources = json.loads(Path(__file__).with_name('provenance.json').read_text())
for source in [sources['geometry']] + sources['reference_images']:
    target = a.out / source['filename']
    if not target.exists():
        with urllib.request.urlopen(source['url'], timeout=60) as response:
            data = response.read()
        assert hashlib.sha256(data).hexdigest() == source['sha256'], f"Source changed: {source['url']}"
        target.write_bytes(data)
    assert hashlib.sha256(target.read_bytes()).hexdigest() == source['sha256'], f'Unexpected file: {target}'
    print(target)
