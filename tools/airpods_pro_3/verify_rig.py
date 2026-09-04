"""Check articulation and dimensions against actual mesh vertices in Blender.

blender -b airpods_pro_3.blend --python verify_rig.py -- report.json
"""
import json
import math
import sys
from pathlib import Path

import bpy
from mathutils import Vector

scene = bpy.context.scene
case = bpy.data.objects['H_Case']
left = bpy.data.objects['H_Left']; right = bpy.data.objects['H_Right']
lid = bpy.data.objects['Lid_Hinge']
case.animation_data_clear(); case['lid_open_degrees'] = 0
case.update_tag(refresh={'OBJECT'})
bpy.context.view_layer.update()

def matrices(objects):
    return {o.name: o.matrix_world.copy() for o in objects}

def unchanged(before):
    return all(max(abs(bpy.data.objects[n].matrix_world[i][j] - m[i][j])
                   for i in range(4) for j in range(4)) < 1e-7 for n, m in before.items())

body = list(bpy.data.collections['Pro3_Body'].objects)
lid_parts = list(bpy.data.collections['Pro3_Lid'].objects)
left_parts = list(bpy.data.collections['Pro3_Left'].objects)
right_parts = list(bpy.data.collections['Pro3_Right'].objects)
assert len(body + lid_parts + left_parts + right_parts) == 55
assert all(o.parent == left for o in left_parts)
assert all(o.parent == right for o in right_parts)
assert all(o.parent == lid for o in lid_parts)

for moving, fixed in [(left, right_parts + body + lid_parts), (right, left_parts + body + lid_parts)]:
    before = matrices(fixed)
    pos = moving.location.copy(); rot = moving.rotation_euler.copy()
    moving.location += Vector((.012, .003, .005)); moving.rotation_euler.z += .4
    bpy.context.view_layer.update()
    assert unchanged(before), f'{moving.name} affected another assembly'
    moving.location = pos; moving.rotation_euler = rot
    bpy.context.view_layer.update()

pin = lid.matrix_world.translation.copy()
fixed = matrices(body + left_parts + right_parts)
closed = matrices(lid_parts)
for angle in [0, 15, 30, 60, 90, 115, 60, 0]:
    case['lid_open_degrees'] = angle
    case.update_tag(refresh={'OBJECT'})
    bpy.context.view_layer.update()
    assert unchanged(fixed), f'lid moved a fixed assembly at {angle}'
    assert (lid.matrix_world.translation - pin).length < 1e-7
    assert abs(lid.rotation_euler.x + math.radians(angle)) < 1e-6, (angle, lid.rotation_euler.x)
assert unchanged(closed), 'lid did not return exactly to its closed pose'

vs = [o.matrix_world @ v.co for o in body + lid_parts for v in o.data.vertices]
dims = [max(v[i] for v in vs) - min(v[i] for v in vs) for i in range(3)]
# Published case dimensions: 62.2 x 21.8 x 47.2 mm. AR mesh excludes some
# manufacturing tolerances; allow 0.4 mm, without changing the source geometry.
assert all(abs(x - y) < .0004 for x, y in zip(dims, [.0622, .0218, .0472])), dims
assert all(im.packed_file for im in bpy.data.images if im.source == 'FILE'), 'unpacked texture'
report = {'meshes': 55, 'parts': {k: len(bpy.data.collections['Pro3_' + k].objects)
                               for k in ['Left', 'Right', 'Body', 'Lid']},
          'closed_case_dimensions_mm': [round(x * 1000, 4) for x in dims],
          'independent_units': True, 'hinge_fixed_axis': True,
          'closed_pose_round_trip': True, 'packed_textures': True}
Path(sys.argv[sys.argv.index('--') + 1]).write_text(json.dumps(report, indent=2) + '\n')
print('RIG_VERIFIED', report)
