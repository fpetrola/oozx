import sys, colorsys
from PIL import Image
SHARE = float(sys.argv[2]) if len(sys.argv) > 2 else 0.5
src = sys.argv[1]
PAL = open('machine/devices/spec256/src/main/resources/spec256/spec256.pal','rb').read()
pal = [tuple(PAL[i*3:i*3+3]) for i in range(256)]
hsv = [colorsys.rgb_to_hsv(*[c/255 for c in p]) for p in pal]

def darker(i, share):
    """The same colour with the light taken out of it, looked for across the whole palette rather
    than only down its own ramp of eight. Hue is what must not move - matching by plain RGB
    distance jumps hue and flattens a lit cave mouth into one green - so hue is what is weighed
    heavily, and the many ramps that climb out of black are what give the steps in between."""
    h, s, v = hsv[i]
    if v == 0: return i
    want = (h, s, v*share)
    def cost(j):
        hj, sj, vj = hsv[j]
        dh = min(abs(hj-h), 1-abs(hj-h)) * (s*sj)      # a grey has no hue to keep
        return dh*dh*24 + (sj-s)**2 + (vj-want[2])**2*4
    return min(range(192), key=cost)

b = open(src,'rb').read()
out = bytes(darker(v, SHARE) for v in b)
open('doc/jsw/jsw.b00','wb').write(out)
im = Image.new('RGB',(320,200)); px = im.load()
for y in range(200):
    for x in range(320): px[x,y] = pal[out[y*320+x]]
im.save('doc/jsw/fondo-puesto.png')
def lum(c): return 0.299*c[0]+0.587*c[1]+0.114*c[2]
a = sum(lum(pal[v]) for v in b)/len(b); z = sum(lum(pal[v]) for v in out)/len(out)
print('x%.2f   luminancia %.0f -> %.0f  (%.0f%%)  tonos %d -> %d'
      % (SHARE, a, z, 100*z/a, len(set(b)), len(set(out))))
