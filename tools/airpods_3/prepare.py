"""Expose the source's existing USD Preview Surface networks to Blender.

uv run --with usd-core python prepare.py --source apple_airpods_3.usdz --out prepared
Apple's 2021 USDZ also has a MaterialX output, which Blender 5.2 imports without
its texture networks. Remove only that competing output on a derivative copy;
retain original Preview Surface shaders, geometry and all texture files.
"""
import argparse
from pathlib import Path
from zipfile import ZipFile
from pxr import Usd

p = argparse.ArgumentParser()
p.add_argument('--source', type=Path, required=True)
p.add_argument('--out', type=Path, required=True)
a = p.parse_args()
a.out.mkdir(parents=True, exist_ok=True)
with ZipFile(a.source) as archive:
    for name in archive.namelist():
        assert (a.out / name).resolve().is_relative_to(a.out.resolve()), name
    root_layer = archive.namelist()[0]
    archive.extractall(a.out)
stage = Usd.Stage.Open(str(a.out / root_layer))
for prim in stage.Traverse():
    if prim.GetTypeName() == 'Material':
        prim.RemoveProperty('outputs:mtlx:surface')
target = a.out / 'preview-surface.usdc'
stage.GetRootLayer().Export(str(target))
print(target)
