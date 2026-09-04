"""Verify shared motion/still spacing and an unambiguous full-canvas contract."""
import json
from pathlib import Path
import numpy as np
from PIL import Image

def verify(renders,model):
 cfg=json.loads(Path(__file__).with_name('presentation.json').read_text())
 def mask(path):
  im=Image.open(path);assert im.size==tuple(cfg['canvas_px']), (path,im.size)
  return np.array(im)[:,:,3]>16
 moving=mask(renders/'connected/0000.png');buds=mask(renders/'buds/0000.png');case=mask(renders/'case/0000.png')
 assert not (buds&case).any(), 'still unit overlaps case'
 def box(m):
  y,x=np.where(m);return [int(x.min()),int(y.min()),int(x.max()),int(y.max())]
 b=box(buds);c=box(case)
 # Case retains its scale while moving by the measured state-specific offset.
 xs=np.where(moving.any(0))[0];groups=np.split(xs,np.where(np.diff(xs)>1)[0]+1)
 assert len(groups) in [2,3]
 cc=groups[-1];m=moving.copy();m[:,:cc[0]]=False
 cb=box(m)
 override=cfg.get('pro_still_overrides',{}).get(model)
 shift=(cfg['still_case_center_m'][0]-cfg['case_center_m'][0])/cfg['ortho_width_m']*1050
 if override:
  assert abs((c[2]-c[0])-(cb[2]-cb[0]))<=2,(c,cb)
  assert abs(c[1]-cb[1])<=2 and abs(c[3]-cb[3])<=2,(c,cb)
 else:
  assert max(abs(c[i]-cb[i]-(shift if i in [0,2] else 0)) for i in range(4))<=2,(c,cb,shift)
 m=moving.copy();m[:,cc[0]:]=False;mb=box(m)
 wider=(b[2]-b[0])-(mb[2]-mb[0])
 expected=2*(cfg['wide_half_spacing_m']-cfg['compact_half_spacing_m'])/cfg['ortho_width_m']*1050
 if override:
  columns=np.where(buds.any(0))[0];parts=np.split(columns,np.where(np.diff(columns)>1)[0]+1)
  assert len(parts)==2
  unit_gap=int(parts[1][0]-parts[0][-1]-1);case_gap=c[0]-b[2]-1
  for measured,key in [(unit_gap,'unit_gap_m'),(case_gap,'case_gap_m')]:
   target=override[key]/cfg['ortho_width_m']*1050
   assert abs(measured-target)<=3,(model,key,measured,target)
  assert wider>0,(model,wider)
  expected=None
 else:
  assert abs(wider-expected)<4,(wider,expected)
 assert b[0]>1 and c[2]<1048
 return {'contract':'tools/apple_artwork/presentation.json','front_start_degrees':0,
         'still_bud_bounds':b,'still_case_bounds':c,'motion_bud_bounds':mb,
         'still_width_increase_px':wider,'expected_width_increase_px':expected,
         'shared_still_canvas':[1050,354],'screenshot_role':'User supplied wide Pro still reference' if override else 'AirPods4 shared wide still reference'}
