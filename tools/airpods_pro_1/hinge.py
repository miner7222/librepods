"""Replace the AR hinge's overlapping relief with the flush A2190 face.
Reference: refs/rear-real.jpg, genuine 1st-generation case rear photograph.
Keep the original outline and divide the face into fixed end tabs and a moving
center leaf, so the lid still opens independently. No changes to case seams.
"""
import math
import bpy
from mathutils import Vector

def refine_hinge(case,lid):
 names=['Body_HvzkffviRwvvBmY','Body_VHeHHZVPajgAAyT','Lid_LezdHfgHZdGavyM',
        'Body_BClAyZEsZaNRhgy','Lid_eSMTyLcxzNuiwha']
 for name in names:
  o=bpy.data.objects[name];o.hide_render=True;o.hide_set(True);o['excluded_ar_overlay']=True
 mat=bpy.data.materials['Rear_Hinge_Satin_Aluminum']
 bs=mat.node_tree.nodes.get('Principled BSDF');bs.inputs['Metallic'].default_value=.15;bs.inputs['Roughness'].default_value=.72
 # Source-space dimensions of the original back plate. The finished face is
 # planar; only a 0.025 mm bevel softens its perimeter at close inspection.
 x0,x1=-.013345,.013348;z0,z1=-.002571,.002895;r=.00105
 outline=[]
 for cx,cz,start in [(x1-r,z1-r,0),(x0+r,z1-r,90),(x0+r,z0+r,180),(x1-r,z0+r,270)]:
  for i in range(17):
   a=math.radians(start+i*90/16);outline.append((cx+r*math.cos(a),cz+r*math.sin(a)))
 def clip(poly,axis,bound,keep):
  out=[]
  for a,b in zip(poly,poly[1:]+poly[:1]):
   ia=(a[axis]-bound)*keep>=0;ib=(b[axis]-bound)*keep>=0
   if ia:out.append(a)
   if ia!=ib:
    t=(bound-a[axis])/(b[axis]-a[axis]);out.append(tuple(a[k]+t*(b[k]-a[k]) for k in range(2)))
  return out
 # Lower full-width bar and upper central leaf belong to the moving lid.
 seam=-.00086;gap=.000035;split=.0074
 regions=[('Lower','Lid',lid,[(1,seam-gap,-1)]),
 ('Center','Lid',lid,[(1,seam-gap,1),(0,-split+gap,1),(0,split-gap,-1)]),
 ('LeftTab','Body',case,[(1,seam+gap,1),(0,-split-gap,-1)]),
 ('RightTab','Body',case,[(1,seam+gap,1),(0,split+gap,1)])]
 # Case translation changed after source meshes were parented; use its current
 # transform to locate source geometry, matching the original attached shell.
 original=bpy.data.objects['Body_HvzkffviRwvvBmY']
 offset=original.matrix_world.translation
 for name,group,parent,cuts in regions:
  poly=outline[:]
  for axis,bound,keep in cuts:poly=clip(poly,axis,bound,keep)
  points=[Vector((x,.01091,z))+offset for x,z in poly]
  n=len(points);vertices=points+[p-Vector((0,.00012,0)) for p in points]
  faces=[tuple(range(n-1,-1,-1)),tuple(range(n,2*n))]+[(i,(i+1)%n,(i+1)%n+n,i+n) for i in range(n)]
  mesh=bpy.data.meshes.new('Planar hinge '+name);mesh.from_pydata(vertices,[],faces);mesh.update();mesh.materials.append(mat)
  o=bpy.data.objects.new(group+'_FlushHinge_'+name,mesh);bpy.data.collections['Pro1_'+group].objects.link(o)
  world=o.matrix_world.copy();o.parent=parent;o.matrix_world=world
  bevel=o.modifiers.new('Microscopic edge bevel','BEVEL');bevel.width=.000025;bevel.segments=3
  o['rear_finish']='planar satin hinge; source footprint; independent fixed tabs and moving leaf'
