"""Import Apple's AirPods 4 USDZ and build an editable, independently articulated rig.

blender -b --python build.py -- --source apple_airpods_4.usdz --out airpods_4.blend
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

# Use the source hierarchy, never spatial thresholds: unit shells extend into
# the case's X band. These four roots partition all 64 source meshes exactly.
source_roots = {'Left': 'rgwuwCnPtZggRDa', 'Right': 'JCCUJxXiqjlfhLm',
                'Body': 'AKhEoFAHKWbkOxD', 'Lid': 'lzlFTcaWXElXzIq'}
groups = {k: [o for o in bpy.data.objects[n].children_recursive if o.type == 'MESH']
          for k, n in source_roots.items()}
# Fixed hinge plate and pin are siblings of the source body root.
groups['Body'] += [o for o in bpy.data.objects['XwJGTFQhPVGqSPo'].children_recursive if o.type == 'MESH']
meshes = [o for o in scene.objects if o.type == 'MESH']
assert set(meshes) == {o for group in groups.values() for o in group}
assert sum(map(len, groups.values())) == len(meshes)

def vertices(objects):
    return [o.matrix_world @ v.co for o in objects for v in o.data.vertices]

def center(objects):
    vs = vertices(objects)
    return Vector([(min(v[i] for v in vs) + max(v[i] for v in vs)) / 2 for i in range(3)])

pin = center([bpy.data.objects['jRsvnUPRKXAFyPU']])
# Source lid is authored at -115 degrees. Closing it about its actual hinge
# retains the original shell, inner liner, and moving hinge leaf as one unit.
close = Matrix.Translation(pin) @ Matrix.Rotation(math.radians(115), 4, 'X') @ Matrix.Translation(-pin)
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
case.id_properties_ui('lid_open_degrees').update(min=0, max=115, description='0 = closed, 115 = fully open; actual source hinge axis')
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
    collection = bpy.data.collections.new('AirPods4_' + key)
    scene.collection.children.link(collection)
    for o in objects:
        for old in list(o.users_collection): old.objects.unlink(o)
        collection.objects.link(o)
        o['source_object'] = o.name
        o.name = key + '_' + o.name

# Refine only the continuous outer shells. Subdivision stays non-destructive;
# vents, contacts, sensors and texture-bearing inserts retain source geometry.
# Recalculate normals instead of interpolating visibly banded AR normals.
for name in ['Left_bDIyXAdIMtgSBEi', 'Right_HIDXaaSAHZrqLDD',
             'Body_vuvYJBgdWMHhiIk', 'Lid_YIDVULmyvQhyjbw']:
    obj = bpy.data.objects[name]
    obj.data.normals_split_custom_set([(0, 0, 0)] * len(obj.data.loops))
    modifier = obj.modifiers.new('Outer shell surface refinement', 'SUBSURF')
    modifier.levels = 1
    modifier.render_levels = 1
    for slot in obj.material_slots:
        material = slot.material.copy()
        slot.material = material
        for node in material.node_tree.nodes:
            if node.type == 'BSDF_PRINCIPLED':
                node.inputs['Roughness'].default_value = .28
                node.inputs['Coat Weight'].default_value = .15
                node.inputs['Coat Roughness'].default_value = .2

for frame, angle in [(1, 0), (45, 115), (85, 115), (130, 0), (160, 0)]:
    case['lid_open_degrees'] = angle
    case.keyframe_insert(data_path='["lid_open_degrees"]', frame=frame)
case.animation_data.action.name = 'Case_Open_Close'
scene.frame_start = 1; scene.frame_end = 160; scene.render.fps = 60
scene.frame_set(1)
scene.unit_settings.system = 'METRIC'
scene['source_url'] = 'https://www.apple.com/105/media/us/airpods-4/2024/62a51629-9227-413a-98ae-ba9e09984c00/ar/airpods-mid.usdz'
scene['provenance'] = 'Apple official AirPods 4 AR geometry and textures; LibrePods rig and renders. No open asset license claimed.'
scene['hinge_axis_source_m'] = list(pin)
scene['mesh_count'] = len(meshes)
readme = bpy.data.texts.new('START_HERE')
readme.write('AirPods 4 — Apple-derived editable rig\n\nH_Left / H_Right: independent complete units.\nH_BudPair: optional shared animation parent.\nH_Case: case body; custom property lid_open_degrees (0..115).\nLid_Hinge: driven X-axis hinge; includes lid liner and moving hinge.\nTimeline 1..160 demonstrates opening and closing. Clear H_Case animation to pose manually.\nAll textures packed. Source/provenance: tools/airpods_4/README.md.\n')
for area in bpy.context.screen.areas if bpy.context.screen else []:
    if area.type == 'VIEW_3D':
        area.spaces.active.clip_start = .001
        area.spaces.active.region_3d.view_distance = .2
bpy.ops.file.pack_all()
Path(a.out).resolve().parent.mkdir(parents=True, exist_ok=True)
bpy.ops.wm.save_as_mainfile(filepath=str(Path(a.out).resolve()))
print('RIG_READY', len(meshes), 'meshes; hinge', tuple(pin))
