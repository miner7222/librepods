"""Render the AirPods 4 rig to transparent PNGs; encode.py makes Android assets."""
import argparse
import math
import json
import sys
from pathlib import Path

import bpy
from mathutils import Vector

p = argparse.ArgumentParser()
p.add_argument('--model', required=True)
p.add_argument('--out', required=True)
p.add_argument('--mode', choices=['connected', 'island', 'case', 'buds', 'left', 'right', 'assembly', 'hinge'], default='connected')
p.add_argument('--frames', type=int, default=360)
p.add_argument('--samples', type=int, default=64)
p.add_argument('--preview', action='store_true', help='Render four quarter-turns only')
p.add_argument('--save-scene', help='Save this render scene as a separate blend file')
a = p.parse_args(sys.argv[sys.argv.index('--') + 1:])
bpy.ops.wm.open_mainfile(filepath=str(Path(a.model).resolve()))
scene = bpy.context.scene
case = bpy.data.objects['H_Case']; pair = bpy.data.objects['H_BudPair']
left = bpy.data.objects['H_Left']; right = bpy.data.objects['H_Right']
case.animation_data_clear(); case['lid_open_degrees'] = 0.0
case.rotation_euler = (0, 0, 0); pair.rotation_euler = (0, 0, 0)
case.location = (.032211, 0, 0); pair.location = (-.032495, 0, 0)
left.location = (-.010465, 0, 0); right.location = (.010465, 0, 0)
width, height, ortho = 1050, 354, .158284
target = Vector((0, 0, 0)); elev = 12

layout = json.loads(Path(__file__).with_name('popup-layout.json').read_text())
phase = 0
if a.mode == 'connected':
    ortho = layout['ortho_m']; elev = layout['elevation_degrees']
    case.location = (layout['case_x_m'], 0, layout['case_z_m'])
    pair.location = (layout['pair_x_m'], 0, layout['pair_z_m'])
    left.location.x = -layout['half_spacing_m']; right.location.x = layout['half_spacing_m']
    phase = math.radians(layout['phase_degrees'])

def hide_group(key):
    for o in bpy.data.collections['AirPods4_' + key].objects: o.hide_render = True

if a.mode in ['island', 'buds', 'left', 'right']:
    hide_group('Body'); hide_group('Lid')
    pair.location = (0, 0, 0)
    width = height = 418 if a.mode == 'island' else 1200
    ortho = .059061 if a.mode == 'island' else .064
    left.location.x = -.0095; right.location.x = .0095
    if a.mode == 'island':
        target.z = -.0011
    if a.mode in ['left', 'right']:
        hide_group('Right' if a.mode == 'left' else 'Left')
        (left if a.mode == 'left' else right).location.x = 0
        ortho = .046
elif a.mode == 'case':
    hide_group('Left'); hide_group('Right')
    case.location = (0, 0, 0)
    width = height = 805; ortho = .0515; elev = 12
elif a.mode in ['assembly', 'hinge']:
    case.location = (0, 0, 0)
    pair.location = (0, 0, .058)
    left.location.x = -.017; right.location.x = .017
    case['lid_open_degrees'] = 115
    width = height = 900; ortho = .12; target.z = .025; elev = 15
    if a.mode == 'hinge':
        hide_group('Left'); hide_group('Right')
        ortho = .085; target.z = .008

# Preserve geometry; tune glossy plastic, not silicone or sensor materials.
for material in bpy.data.materials:
    if material.name.startswith(('QKSRoscgZluRYQw', 'WwrjthXqFCMqrXz')):
        for node in material.node_tree.nodes:
            if node.type == 'BSDF_PRINCIPLED':
                node.inputs['Roughness'].default_value = .18
                node.inputs['Coat Weight'].default_value = .15
                node.inputs['Coat Roughness'].default_value = .14

# Earbud highlights are softer than the case's long strip reflections.
for side in ['Left', 'Right']:
    for obj in bpy.data.collections['AirPods4_' + side].objects:
        for slot in obj.material_slots:
            if slot.material and slot.material.name.startswith(('QKSRoscgZluRYQw', 'WwrjthXqFCMqrXz')):
                slot.material = slot.material.copy()
                node = next(n for n in slot.material.node_tree.nodes if n.type == 'BSDF_PRINCIPLED')
                node.inputs['Roughness'].default_value = .32
                node.inputs['Coat Weight'].default_value = .08

scene.world = bpy.data.worlds.new('White_Studio')
scene.world.use_nodes = True
scene.world.node_tree.nodes['Background'].inputs[0].default_value = (1, 1, 1, 1)
scene.world.node_tree.nodes['Background'].inputs[1].default_value = .65

def area(name, loc, energy, size):
    data = bpy.data.lights.new(name, 'AREA')
    data.energy = energy; data.shape = 'RECTANGLE'; data.size = size; data.size_y = size * .65
    obj = bpy.data.objects.new(name, data); scene.collection.objects.link(obj)
    obj.location = loc
    obj.rotation_euler = (Vector((0, 0, 0)) - obj.location).to_track_quat('-Z', 'Y').to_euler()

area('Key_Softbox', (0, -.35, .42), 2.3, .9)
area('Left_Fill', (-.42, -.18, .1), 1.1, .8)
area('Right_Fill', (.42, -.18, .1), 1.1, .8)
area('Back_Rim', (0, .4, .3), 1.2, .9)
# Symmetric tall reflection cards match the connected-popup case highlights.
for name, x in [('Left_Reflection', -.20), ('Right_Reflection', .20)]:
    area(name, (x, -.42, .03), .75, .13)
    bpy.data.lights[name].size_y = .55
# Broad fill illuminates the product without adding a second box-shaped glare.
for name in ['Key_Softbox', 'Left_Fill', 'Right_Fill', 'Back_Rim']:
    bpy.data.objects[name].visible_glossy = False
camdata = bpy.data.cameras.new('Product_Orthographic'); camdata.type = 'ORTHO'; camdata.ortho_scale = ortho
camdata.clip_start = .001; camdata.clip_end = 10
cam = bpy.data.objects.new('Product_Orthographic', camdata); scene.collection.objects.link(cam)
angle = math.radians(elev)
cam.location = target + Vector((0, -.55 * math.cos(angle), .55 * math.sin(angle)))
cam.rotation_euler = (math.pi / 2 - angle, 0, 0); scene.camera = cam
scene.render.engine = 'CYCLES'
prefs = bpy.context.preferences.addons['cycles'].preferences
try:
    prefs.compute_device_type = 'METAL'; prefs.get_devices()
    for device in prefs.devices: device.use = device.type != 'CPU'
    scene.cycles.device = 'GPU'
except Exception as e: print('Using CPU:', e)
scene.cycles.samples = a.samples; scene.cycles.use_denoising = True
scene.render.use_persistent_data = True
scene.render.resolution_x = width; scene.render.resolution_y = height; scene.render.resolution_percentage = 100
scene.render.film_transparent = True
scene.render.image_settings.file_format = 'PNG'; scene.render.image_settings.color_mode = 'RGBA'
scene.view_settings.view_transform = 'Standard'; scene.view_settings.look = 'None'
scene.view_settings.exposure = -.85
scene.render.fps = 60
scene.frame_start = 1; scene.frame_end = a.frames
Path(a.out).mkdir(parents=True, exist_ok=True)
if a.save_scene:
    bpy.ops.wm.save_as_mainfile(filepath=str(Path(a.save_scene).resolve()))
indices = [0, a.frames // 4, a.frames // 2, 3 * a.frames // 4] if a.preview else range(a.frames)
for i in indices:
    theta = 2 * math.pi * i / a.frames + phase
    if a.mode in ['connected', 'island']:
        pair.rotation_euler.z = theta; case.rotation_euler.z = theta
    elif a.mode == 'hinge':
        case['lid_open_degrees'] = 115 * (1 - math.cos(theta)) / 2
    case.update_tag(refresh={'OBJECT'})
    bpy.context.view_layer.update()
    scene.render.filepath = str(Path(a.out).resolve() / f'{i:04d}.png')
    bpy.ops.render.render(write_still=True)
    print('FRAME_READY', a.mode, i, flush=True)
# Successful completion marker prevents partial rerenders from passing as a full batch.
(Path(a.out) / 'render-complete.json').write_text(json.dumps({'mode': a.mode, 'frames': len(indices), 'samples': a.samples}))
