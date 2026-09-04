"""Import Apple's AirPods 3 USDZ and build an editable, independently articulated rig.

blender -b --python build.py -- --source prepared/preview-surface.usdc --out airpods_3.blend
Geometry and textures are Apple-derived; see README.md for provenance.
"""
import argparse
import math
import sys
from pathlib import Path

import bpy
from mathutils import Matrix, Vector

p = argparse.ArgumentParser()
p.add_argument('--source', required=True)
p.add_argument('--out', required=True)
a = p.parse_args(sys.argv[sys.argv.index('--') + 1:])
bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.wm.usd_import(filepath=str(Path(a.source).resolve()))
scene = bpy.context.scene
bpy.context.view_layer.update()

# The source puts each outer earbud shell under the opposite unit root.
# Correct those two explicit memberships before building independent handles.
source_roots = {'Left': 'YSfcRDdTMqyylaC', 'Right': 'smQLlFnhKhFlHqw',
                'Body': 'LvljTjvBRnnoTcE', 'Lid': 'SLDNgqNbqubpsrP'}
groups = {k: [o for o in bpy.data.objects[n].children_recursive if o.type == 'MESH']
          for k, n in source_roots.items()}
for side, other, name in [('Left', 'Right', 'IpRxyeMlETsixdH'),
                          ('Right', 'Left', 'vgJsNTlxIhMAIEm')]:
    o = bpy.data.objects[name]
    groups[other].remove(o)
    groups[side].append(o)
meshes = [o for o in scene.objects if o.type == 'MESH']
assert set(meshes) == {o for group in groups.values() for o in group}
assert sum(map(len, groups.values())) == len(meshes)

def vertices(objects):
    return [o.matrix_world @ v.co for o in objects for v in o.data.vertices]

def center(objects):
    vs = vertices(objects)
    return Vector([(min(v[i] for v in vs) + max(v[i] for v in vs)) / 2 for i in range(3)])

# This older USDZ bakes the 110-degree open pose into vertices (no hinge
# transform). Solve its pivot from the lid's planar rim and body seam instead
# of reusing another generation's hinge coordinates or prior video pixels.
r = Matrix.Rotation(math.radians(110), 3, 'X')
rim = [r @ v for v in vertices([bpy.data.objects['nbtkpnbbbfJOgLD']])]
body_top = max(v.z for v in vertices([bpy.data.objects['IcLxygScNJOhffO']]))
ty = -(min(v.y for v in rim) + max(v.y for v in rim)) / 2
tz = body_top + .00005 - min(v.z for v in rim)
# (I - R) * pivot = translation; X is the free hinge axis.
from mathutils import Matrix as M
system = M(((1-r[1][1], -r[1][2]), (-r[2][1], 1-r[2][2])))
yz = system.inverted() @ Vector((ty, tz))
pin = Vector((0, yz.x, yz.y))
close = Matrix.Translation(pin) @ r.to_4x4() @ Matrix.Translation(-pin)
for o in groups['Lid']:
    o.matrix_world = close @ o.matrix_world
bpy.context.view_layer.update()

# Bake source transforms into meshes; each resulting handle has unit scale.
for o in meshes:
    world = o.matrix_world.copy()
    o.parent = None
    o.data.transform(world)
    o.matrix_world = Matrix.Identity(4)
for o in list(scene.objects):
    if o.type != 'MESH':
        bpy.data.objects.remove(o, do_unlink=True)
bpy.context.view_layer.update()

def empty(name, location=(0, 0, 0), parent=None):
    o = bpy.data.objects.new(name, None)
    scene.collection.objects.link(o)
    o.empty_display_type = 'PLAIN_AXES'
    o.empty_display_size = .007
    o.parent = parent
    o.location = location
    return o

def attach(objects, parent):
    bpy.context.view_layer.update()
    for o in objects:
        world = o.matrix_world.copy()
        o.parent = parent
        o.matrix_world = world

case_center = center(groups['Body'] + groups['Lid'])
case = empty('H_Case', case_center)
attach(groups['Body'], case)
lid = empty('Lid_Hinge', pin - case_center, case)
attach(groups['Lid'], lid)
case['lid_open_degrees'] = 0.0
case.id_properties_ui('lid_open_degrees').update(min=0, max=110, description='0 = closed, 110 = fully open; reconstructed source hinge axis')
driver = lid.driver_add('rotation_euler', 0).driver
v = driver.variables.new(); v.name = 'angle'; v.type = 'SINGLE_PROP'
v.targets[0].id = case; v.targets[0].data_path = '["lid_open_degrees"]'
driver.expression = '-angle * pi / 180'

pair = empty('H_BudPair')
for side in ['Left', 'Right']:
    c = center(groups[side])
    h = empty('H_' + side, c, pair)
    attach(groups[side], h)
    # Front-facing source pose, centered on each independently editable handle.
    h.location = ((-.019 if side == 'Left' else .019), 0, 0)
    h['source_side'] = side
    h['independent_rigid_unit'] = True

case.location = (.040, 0, 0)
pair.location = (-.040, 0, 0)
for key, objects in groups.items():
    collection = bpy.data.collections.new('AirPods3_' + key)
    scene.collection.children.link(collection)
    for o in objects:
        for old in list(o.users_collection): old.objects.unlink(o)
        collection.objects.link(o)
        o['source_object'] = o.name
        o.name = key + '_' + o.name

# Refine only the earbud shells. Preserve the case shell and lid geometry
# and authored normals: their seam and lanyard aperture lack support loops,
# so subdivision rounds and dents these intentionally shallow details.
for name in ['Left_IpRxyeMlETsixdH', 'Right_vgJsNTlxIhMAIEm']:
    obj = bpy.data.objects[name]
    obj.data.normals_split_custom_set([(0, 0, 0)] * len(obj.data.loops))
    modifier = obj.modifiers.new('Outer shell surface refinement', 'SUBSURF')
    modifier.levels = 1
    modifier.render_levels = 1
# Apply studio plastic consistently to shells, recesses and seam inserts.
# Keep the sensor, grill, contact and engraving materials unchanged.
for material in bpy.data.materials:
    if material.name not in ['rMdTrbgwRkDJRbS', 'HLRWbPbKcwGdWaf']:
        continue
    for node in material.node_tree.nodes:
        if node.type == 'BSDF_PRINCIPLED':
            for socket_name in ['Emission Color', 'Roughness']:
                for link in list(node.inputs[socket_name].links):
                    material.node_tree.links.remove(link)
            node.inputs['Emission Strength'].default_value = 0
            node.inputs['Roughness'].default_value = .28
            node.inputs['Coat Weight'].default_value = .15
            node.inputs['Coat Roughness'].default_value = .2

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from apple_artwork.rear import refine_rear
refine_rear(
    hinge_names=['Body_AiJtAHNGduOEVKf', 'Body_ntjVPkXfFoOFsoU',
                 'Body_pPBGrsAoDIkILXR', 'Body_YVeuJfgobBvclEI', 'Lid_mWVSmahlSPBczjU'],
    overlay_names=['Lid_wcxYvWyHTLTIoPZ'],
    button_name='Body_IDMvzsCWsSpvOeh', button_overlays=[],
    plastic_name='rMdTrbgwRkDJRbS', shell_name='Body_IcLxygScNJOhffO')

for frame, angle in [(1, 0), (45, 110), (85, 110), (130, 0), (160, 0)]:
    case['lid_open_degrees'] = angle
    case.keyframe_insert(data_path='["lid_open_degrees"]', frame=frame)
case.animation_data.action.name = 'Case_Open_Close'
scene.frame_start = 1; scene.frame_end = 160; scene.render.fps = 60
scene.frame_set(1)
scene.unit_settings.system = 'METRIC'
scene['source_url'] = 'https://www.apple.com/105/media/us/airpods-3rd-generation/2021/3c0b27aa-a5fe-4365-a9ae-83c28d10fa21/ar/airpods_magsafe_charging_ios15.usdz'
scene['provenance'] = 'Apple official AirPods 3 AR geometry and textures; LibrePods rig and renders. No open asset license claimed.'
scene['hinge_axis_source_m'] = list(pin)
scene['mesh_count'] = len(meshes)
readme = bpy.data.texts.new('START_HERE')
readme.write('AirPods 3 — Apple-derived editable rig\n\nH_Left / H_Right: independent complete units.\nH_BudPair: optional shared animation parent.\nH_Case: case body; custom property lid_open_degrees (0..110).\nLid_Hinge: driven X-axis hinge; includes lid liner and moving hinge.\nTimeline 1..160 demonstrates opening and closing. Clear H_Case animation to pose manually.\nAll textures packed. Source/provenance: tools/airpods_3/README.md.\n')
for area in bpy.context.screen.areas if bpy.context.screen else []:
    if area.type == 'VIEW_3D':
        area.spaces.active.clip_start = .001
        area.spaces.active.region_3d.view_distance = .2
assert any(im.source == 'FILE' for im in bpy.data.images), 'Run prepare.py first: MaterialX import loses textures'
bpy.ops.file.pack_all()
Path(a.out).resolve().parent.mkdir(parents=True, exist_ok=True)
bpy.ops.wm.save_as_mainfile(filepath=str(Path(a.out).resolve()))
print('RIG_READY', len(meshes), 'meshes; hinge', tuple(pin))
