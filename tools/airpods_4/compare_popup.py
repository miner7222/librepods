"""Build a like-sized screenshot/render comparison without editing either input."""
import argparse,json
from pathlib import Path
import numpy as np
from PIL import Image,ImageDraw
p=argparse.ArgumentParser();p.add_argument('--tools',type=Path,required=True);p.add_argument('--work',type=Path,required=True);a=p.parse_args()
f=json.loads((a.tools/'popup-layout.json').read_text());x0,x1=f['popup_card_x'];cy=(f['case_bounds_px'][1]+f['case_bounds_px'][3])/2;h=(x1-x0)*354/1050
ref=Image.open(a.work/'refs'/f['reference_filename']).crop((x0,round(cy-h/2),x1,round(cy+h/2))).resize((1050,354))
rgba=np.array(Image.open(a.work/'popup-preview/0000.png'));lut=np.array(json.loads((a.tools/'grade.json').read_text()),dtype=np.uint8);rgba[:,:,:3]=np.stack([lut[c][rgba[:,:,c]] for c in range(3)],2)
im=Image.fromarray(rgba);can=Image.new('RGB',(1050,760),'white');can.paste(ref,(0,25));can.paste(im,(0,405),im);dr=ImageDraw.Draw(can);dr.text((12,7),'Connected popup reference',fill='black');dr.text((12,385),'Calibrated Blender render',fill='black');can.save(a.work/'popup-comparison.png')
