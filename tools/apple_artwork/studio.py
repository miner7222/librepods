"""Shared metric layout and physical studio for all Apple-derived models.

Keep source rig geometry separate from presentation. The camera, assembly
centers and compact/wide spacing are measured from one AirPods 4 reference set.
All surfaces use physical materials; no screenshot-specific percentile LUT.
"""
import json,math
from pathlib import Path
import bpy
from mathutils import Vector

SPECS={
 'airpods_3':([54.4,21.38,46.4],30.79,'AirPods3_'),
 'airpods_4':([50.1,21.2,46.2],30.2,'AirPods4_'),
 'airpods_pro_1':([60.6,21.7,45.2],30.9,'Pro1_'),
 'airpods_pro_2':([60.6,21.7,45.2],30.9,'Pro2_'),
 'airpods_pro_3':([62.2,21.8,47.2],30.9,'Pro3_'),
}

def bounds(handle):
 inv=handle.matrix_world.inverted();vs=[]
 for obj in handle.children_recursive:
  if obj.type=='MESH' and not obj.get('excluded_ar_overlay'):
   vs.extend(inv@obj.matrix_world@v.co for v in obj.data.vertices)
 return [max(v[i] for v in vs)-min(v[i] for v in vs) for i in range(3)]

def apply(model,mode):
 cfg=json.loads(Path(__file__).with_name('presentation.json').read_text())
 scene=bpy.context.scene;case=bpy.data.objects['H_Case'];pair=bpy.data.objects['H_BudPair']
 left=bpy.data.objects['H_Left'];right=bpy.data.objects['H_Right']
 dimensions,height,prefix=SPECS[model]
 # Normalize in the closed canonical rig, before any animation or projection.
 opened=case['lid_open_degrees'];case['lid_open_degrees']=0;case.update_tag(refresh={'OBJECT'});bpy.context.view_layer.update()
 size=bounds(case)
 case.scale=[mm/1000/actual for mm,actual in zip(dimensions,size)]
 # Source earbud width/depth describe a posed device, so scaling those AABBs
 # independently would distort silicone tips. Use its verified stem-height
 # scale uniformly and preserve Apple's authored three-dimensional proportions.
 for h in [left,right]:
  s=height/1000/bounds(h)[2];h.scale=(s,s,s)
 case['lid_open_degrees']=opened;case.update_tag(refresh={'OBJECT'})
 unit_width=max(bounds(h)[0]*h.scale.x for h in [left,right])
 if mode in ['connected','buds','case']:
  case.location=cfg['case_center_m' if mode=='connected' else 'still_case_center_m'];pair.location=cfg['pair_center_m' if mode=='connected' else 'still_pair_center_m']
  half=cfg['compact_half_spacing_m'] if mode=='connected' else cfg['wide_half_spacing_m']
  half += (unit_width-.0183)/2
  left.location=(-half,0,0);right.location=(half,0,0)
  width,height_px=cfg['canvas_px'];ortho=cfg['ortho_width_m'];elev=cfg['camera_elevation_degrees'];target=Vector((0,0,0))
 elif mode=='island':
  pair.location=(0,0,0);half=cfg['compact_half_spacing_m']
  half += (unit_width-.0183)/2
  left.location=(-half,0,0);right.location=(half,0,0)
  width=height_px=418;ortho=cfg['island_ortho_width_m'];elev=0;target=Vector((0,0,0))
 else:
  width=height_px=None;ortho=elev=target=None
 # The user supplied explicit wide Pro references after reviewing the shared
 # composition. Measure visible edge gaps against physical case width, keeping
 # current unit orientation and metric scale independent of older iOS poses.
 if mode in ['buds','case'] and model in cfg.get('pro_still_overrides',{}):
  ref=cfg['pro_still_overrides'][model]
  bpy.context.view_layer.update()
  def horizontal_extent(h):
   xs=[(o.matrix_world@v.co).x for o in h.children_recursive
       if o.type=='MESH' and not o.get('excluded_ar_overlay') for v in o.data.vertices]
   return min(xs),max(xs)
  extents=[horizontal_extent(h) for h in [left,right]]
  widths=[hi-lo for lo,hi in extents]
  gap=ref['unit_gap_m'];case_gap=ref['case_gap_m']
  for h,(lo,hi),w,sign in zip([left,right],extents,widths,[-1,1]):
   offset=(lo+hi)/2-h.matrix_world.translation.x
   h.location.x=sign*(gap+w)/2-offset
  pair_width=sum(widths)+gap;case_width=dimensions[0]/1000
  # Center the complete silhouette, retaining a small 1 mm optical offset.
  pair.location.x=-(case_width+case_gap)/2+.001-(widths[1]-widths[0])/2
  case.location.x=(pair_width+case_gap)/2+.001
  scene['still_unit_gap_m']=gap;scene['still_case_gap_m']=case_gap
 # Remove previous generation-specific softboxes and reflection cards.
 for o in list(scene.objects):
  if o.type=='LIGHT':bpy.data.objects.remove(o,do_unlink=True)
 scene.world.node_tree.nodes['Background'].inputs[1].default_value=1.2
 def area(name,loc,energy,size,aspect=1):
  d=bpy.data.lights.new(name,'AREA');d.energy=energy;d.shape='RECTANGLE';d.size=size;d.size_y=size*aspect
  o=bpy.data.objects.new(name,d);scene.collection.objects.link(o);o.location=loc
  o.rotation_euler=(-o.location).to_track_quat('-Z','Y').to_euler()
 area('Shared_Key',(-.18,-.35,.38),1.1,.65)
 area('Shared_Fill',(.35,-.2,.12),.6,.8)
 area('Shared_Rim',(0,.3,.3),.5,.65)
 for name in ['Shared_Key','Shared_Fill','Shared_Rim']:
  bpy.data.objects[name].visible_glossy=False
 area('Shared_Reflection_Left',(-.24,-.4,.03),1.3,.13,3.5)
 area('Shared_Reflection_Right',(.24,-.4,.03),1.1,.13,3.5)
 for name in ['Shared_Reflection_Left','Shared_Reflection_Right']:
  bpy.data.objects[name].visible_diffuse=False
 # Broad soft illumination and modest coat give stable highlights through a
 # complete turn rather than narrow bright stripes at one chosen angle.
 plastic_prefixes=('QKSRoscgZluRYQw','WwrjthXqFCMqrXz','AWTzhuHRqcuHHac','rMdTrbgwRkDJRbS','HLRWbPbKcwGdWaf','cgUPbAakHcrHzXB')
 for mat in bpy.data.materials:
  if not mat.use_nodes or not mat.name.startswith(plastic_prefixes):continue
  for node in mat.node_tree.nodes:
   if node.type!='BSDF_PRINCIPLED':continue
   for socket in ['Base Color','Metallic','Roughness','Coat Weight','Coat Roughness','Emission Color']:
    for link in list(node.inputs[socket].links):mat.node_tree.links.remove(link)
   node.inputs['Base Color'].default_value=(.90,.91,.93,1)
   node.inputs['Metallic'].default_value=0
   node.inputs['Roughness'].default_value=.24
   node.inputs['Coat Weight'].default_value=.05
   node.inputs['Coat Roughness'].default_value=.23
   node.inputs['Emission Strength'].default_value=0
 # Correct material assignments where old AR sources shared silicone/plastic.
 tips={'airpods_pro_1':['Left_FUtvjnzouomCSDN','Right_HJTmepUhNpWfTur']}
 for name in tips.get(model,[]):
  for slot in bpy.data.objects[name].material_slots:
   slot.material=slot.material.copy()
   node=next(n for n in slot.material.node_tree.nodes if n.type=='BSDF_PRINCIPLED')
   node.inputs['Roughness'].default_value=.55;node.inputs['Coat Weight'].default_value=0
 scene['presentation_contract']='tools/apple_artwork/presentation.json'
 scene['presentation_mode']=mode
 scene['unit_width_m']=unit_width
 scene['unit_center_distance_m']=right.location.x-left.location.x
 scene['case_dimensions_mm']=dimensions
 scene['earbud_scale_method']='uniform canonical stem height; authored width/depth preserved'
 bpy.context.view_layer.update()
 return width,height_px,ortho,elev,target
