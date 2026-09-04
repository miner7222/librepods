"""Match case color percentiles to the actual connected-popup screenshot."""
import argparse,json
from pathlib import Path
import numpy as np
from PIL import Image
p=argparse.ArgumentParser();p.add_argument('--tools',type=Path,required=True);p.add_argument('--work',type=Path,required=True);p.add_argument('--frame',type=Path,required=True);a=p.parse_args()
f=json.loads((a.tools/'popup-layout.json').read_text())
ref=np.array(Image.open(a.work/'refs/connected-popup.jpg').convert('RGB'))
x0,y0,x1,y1=f['case_bounds_px'];ref=ref[y0:y1,x0:x1]
im=np.array(Image.open(a.frame).convert('RGBA'));mask=im[:,:,3]>128;cols=np.where(mask.any(0))[0];groups=np.split(cols,np.where(np.diff(cols)>1)[0]+1);g=groups[-1];ys=np.where(mask[:,g].any(1))[0];src=im[ys[0]:ys[-1]+1,g[0]:g[-1]+1,:3]
def samples(x):
 h,w=x.shape[:2];yy,xx=np.mgrid[:h,:w];m=((xx-w/2)/(w*.46))**4+((yy-h/2)/(h*.46))**4<1
 return x[m&(x.min(2)>120)]
levels=[5,10,50,90,98];raw=np.percentile(samples(src),levels,axis=0);target=np.percentile(samples(ref),levels,axis=0);lut=[]
for c in range(3):
 x=np.array([0,60,100,*raw[:,c],255]);y=np.array([0,60,110,*target[:,c],255]);keep=np.r_[np.diff(x)>0,True];lut.append(np.rint(np.interp(np.arange(256),x[keep],y[keep])).astype(int).tolist())
(a.tools/'grade.json').write_text(json.dumps(lut,indent=2)+'\n')
(a.tools/'color-calibration.json').write_text(json.dumps({'percentiles':levels,'raw_case_rgb':raw.tolist(),'popup_case_rgb':target.tolist(),'reference':f['source_url'],'method':'Inset superellipse mask in case bounding box; low anchors preserve dark sensors; RGB curve does not replace physical gloss lighting.'},indent=2)+'\n')
print('POPUP_COLOR_CALIBRATED',target.tolist())
