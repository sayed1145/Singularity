#!/usr/bin/env python3
"""Frame PNGs (from preview.Anim) -> GIF.

usage: gif.py <frameDir> <out.gif> [--fps N] [--bg RRGGBB] [--scale K]

If the frame directory contains a lens.txt sidecar (written by Anim for the singularity forge) the v6.1
gravitational-lens fragment shader (src/blackhole/BHShaders.java) is re-implemented here in numpy and applied to
every frame, so the GIF shows the same deflection / photon ring / Doppler disk the game renders on the GPU.
"""
import sys, os, glob, math
import numpy as np
from PIL import Image

def lens(img, hx, hy, rs, strength, ppu, half, t):
    """img: float32 HxWx3 (0..1, already composited on an opaque background). Mirrors the GLSL main()."""
    h, w, _ = img.shape
    ys, xs = np.mgrid[0:h, 0:w].astype(np.float32)
    wx = (xs + 0.5) / ppu - half          # world x
    wy = half - (ys + 0.5) / ppu          # world y (up)
    dx, dy = wx - hx, wy - hy
    dist = np.sqrt(dx * dx + dy * dy)
    reach = rs * 9.0
    inside = (dist <= reach) & (dist >= 1e-4)
    d = np.maximum(dist, 1e-4)
    dirx, diry = dx / d, dy / d
    b = np.maximum(d, rs * 0.55)
    defl = (2.0 * rs * rs) / (b * b) * rs * 1.55 * strength
    photon = rs * 1.5
    tt = 1.0 - np.clip((d - photon) / (photon * 1.2), 0.0, 1.0)
    defl = np.where(d < photon * 2.2, defl * (1.0 + tt * tt * 3.6), defl)
    def smoothstep(e0, e1, x):
        s = np.clip((x - e0) / (e1 - e0), 0.0, 1.0)
        return s * s * (3 - 2 * s)
    fade = 1.0 - smoothstep(reach * 0.55, reach, d)
    defl = defl * fade * inside
    offx = -dirx * defl + (-diry) * defl * 0.42 * strength
    offy = -diry * defl + (dirx) * defl * 0.42 * strength
    dark = (1.0 - smoothstep(rs * 0.92, rs * 1.06, d)) * inside
    rw = rs * 0.16
    ang = np.arctan2(dy, dx)
    shimmer = 0.82 + 0.18 * np.sin(ang * 3.0 + t * 0.09) + 0.10 * np.sin(ang * 7.0 - t * 0.05)
    ring = (1.0 - smoothstep(0.0, rw, np.abs(d - photon))) * fade * strength * shimmer * inside
    dr = d - rs * 1.9
    band = np.exp(-dr * dr / (rs * rs * 1.1))
    doppler = 0.55 + 0.45 * np.sin(ang + t * 0.02)
    disk = band * fade * strength * doppler * inside
    # sample the source at the deflected world position (bilinear)
    sx = (wx + offx + half) * ppu - 0.5
    sy = (half - (wy + offy)) * ppu - 0.5
    sx = np.clip(sx, 0, w - 1.001); sy = np.clip(sy, 0, h - 1.001)
    x0 = np.floor(sx).astype(int); y0 = np.floor(sy).astype(int)
    fx = (sx - x0)[..., None]; fy = (sy - y0)[..., None]
    c = (img[y0, x0] * (1 - fx) * (1 - fy) + img[y0, x0 + 1] * fx * (1 - fy)
         + img[y0 + 1, x0] * (1 - fx) * fy + img[y0 + 1, x0 + 1] * fx * fy)
    c = c * (1.0 - np.clip(dark, 0, 1))[..., None]
    disk = np.clip(disk, 0, 1)[..., None]
    c = c + np.array([1.0, 0.52, 0.16], np.float32) * disk * 0.30
    ring = np.clip(ring, 0, 1)[..., None]
    c = c + np.array([1.0, 0.74, 0.40], np.float32) * ring * 0.90
    return np.clip(c, 0, 1)

def main():
    args = sys.argv[1:]
    src, out = args[0], args[1]
    fps = 12; bg = (0x2c, 0x2c, 0x34); scale = 1.0
    i = 2
    while i < len(args):
        if args[i] == '--fps': fps = int(args[i + 1]); i += 2
        elif args[i] == '--bg': bg = tuple(int(args[i + 1][k:k + 2], 16) for k in (0, 2, 4)); i += 2
        elif args[i] == '--scale': scale = float(args[i + 1]); i += 2
        else: raise SystemExit('unknown arg ' + args[i])
    files = sorted(glob.glob(os.path.join(src, '[0-9][0-9][0-9].png')))
    lens_params = None
    lp = os.path.join(src, 'lens.txt')
    if os.path.isfile(lp):
        with open(lp) as f:
            a = f.readline().split(); loop = float(f.readline())
        lens_params = dict(hx=float(a[0]), hy=float(a[1]), rs=float(a[2]), strength=float(a[3]), ppu=float(a[4]), half=float(a[5]), frames=int(a[6]), loop=loop)
    frames = []
    for n, fn in enumerate(files):
        im = Image.open(fn).convert('RGBA')
        back = Image.new('RGBA', im.size, bg + (255,))
        back.alpha_composite(im)
        rgb = np.asarray(back.convert('RGB'), np.float32) / 255.0
        if lens_params:
            p = lens_params
            t = n * p['loop'] / p['frames']
            rgb = lens(rgb, p['hx'], p['hy'], p['rs'], p['strength'], p['ppu'], p['half'], t)
        fr = Image.fromarray((rgb * 255 + 0.5).astype(np.uint8), 'RGB')
        if scale != 1.0:
            fr = fr.resize((int(fr.width * scale), int(fr.height * scale)), Image.LANCZOS)
        frames.append(fr)
    # one shared adaptive palette for the whole clip (no per-frame palette flicker), no dithering noise
    sheet = Image.new('RGB', (frames[0].width, frames[0].height * min(len(frames), 8)))
    for k in range(min(len(frames), 8)):
        sheet.paste(frames[k * len(frames) // min(len(frames), 8)], (0, k * frames[0].height))
    pal = sheet.quantize(colors=255, method=Image.Quantize.MEDIANCUT)
    q = [f.quantize(palette=pal, dither=Image.Dither.NONE) for f in frames]
    q[0].save(out, save_all=True, append_images=q[1:], duration=int(round(1000 / fps)), loop=0, optimize=False, disposal=1)
    print('%s: %d frames %dx%d, %.2f MB' % (out, len(q), q[0].width, q[0].height, os.path.getsize(out) / 1e6))

if __name__ == '__main__':
    main()
