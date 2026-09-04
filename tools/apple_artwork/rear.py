"""Photo-referenced rear hardware refinement for Apple's older AR models."""
import math
import bpy
from mathutils import Vector


def refine_rear(*, hinge_names, overlay_names, button_name, button_overlays, plastic_name, shell_name):
    # AR overlays occupy almost the same surface as the underlying hinge/button.
    # Their transparency, baked lighting and normal maps cause dark patches in
    # Cycles. Retain them for source provenance, but exclude them from rendering.
    for name in overlay_names + button_overlays:
        obj = bpy.data.objects[name]
        obj.hide_render = True
        obj.hide_set(True)
        obj['excluded_ar_overlay'] = True

    metal = bpy.data.materials.new('Rear_Hinge_Satin_Aluminum')
    metal.use_nodes = True
    node = metal.node_tree.nodes.get('Principled BSDF')
    node.inputs['Base Color'].default_value = (.8, .8, .8, 1)
    node.inputs['Metallic'].default_value = .35
    node.inputs['Roughness'].default_value = .62
    for name in hinge_names:
        obj = bpy.data.objects[name]
        obj.data.materials.clear(); obj.data.materials.append(metal)
        # Use geometric normals on the broad flat hardware faces. Averaged
        # normals and baked normal maps make a planar hinge look dented.
        obj.data.normals_split_custom_set([(0, 0, 0)] * len(obj.data.loops))
        for polygon in obj.data.polygons:
            polygon.use_smooth = False
        obj['rear_finish'] = 'uniform satin aluminum; no baked normal/emission maps'

    obj = bpy.data.objects[button_name]
    world = obj.matrix_world.copy()
    points = [world @ v.co for v in obj.data.vertices]
    cx = (min(v.x for v in points) + max(v.x for v in points)) / 2
    cz = (min(v.z for v in points) + max(v.z for v in points)) / 2
    front = max(v.y for v in points)
    radius = max(v.x for v in points) - cx
    # Preserve the source center and diameter. Replace its 1.4–1.5 mm rounded
    # sidewall with a flush face and a 0.03 mm edge bevel, matching rear photos.
    shell = bpy.data.objects[shell_name]
    # Preserve the flat shell around the button aperture under subdivision.
    # An uncreased 90-degree hole rim was pulling the surrounding panel inward.
    rim = set()
    for vertex in shell.data.vertices:
        q = shell.matrix_world @ vertex.co
        rad = math.hypot(q.x - cx, q.z - cz)
        if rad < radius + .0002 and q.y > front - .00008:
            rim.add(vertex.index)
    crease = shell.data.attributes.get('crease_edge') or shell.data.attributes.new('crease_edge', 'FLOAT', 'EDGE')
    for edge in shell.data.edges:
        if all(index in rim for index in edge.vertices):
            crease.data[edge.index].value = 1.0
            edge.use_edge_sharp = True
    shell['button_aperture_creased'] = len(rim)
    rings = [(radius, front - .00003), (radius - .00003, front)]
    inv = world.inverted(); vertices = []; faces = []; segments = 128
    for rad, y in rings:
        for i in range(segments):
            angle = 2 * math.pi * i / segments
            vertices.append(inv @ Vector((cx + rad * math.cos(angle), y, cz + rad * math.sin(angle))))
    for i in range(segments):
        j = (i + 1) % segments
        faces.append((i, segments+i, segments+j, j))
    faces.append(tuple(range(2*segments-1, segments-1, -1)))
    mesh = bpy.data.meshes.new('Flush_Pairing_Button')
    mesh.from_pydata(vertices, [], faces); mesh.update()
    obj.data = mesh
    mesh.materials.append(bpy.data.materials[plastic_name])
    for polygon in mesh.polygons: polygon.use_smooth = False
    obj['rear_refinement'] = 'flush face; 0.03 mm bevel; source center and diameter retained'
    obj['button_face_y_world_m'] = front
