#!/usr/bin/env python3
"""用真实 GPU 管线跑模组的引力透镜着色器，证明"扭曲的是整幅画面"。

流程与游戏里一致：
  1. 造一张"游戏场景"贴图（网格地形 + 建筑 + 传送带 + 单位）
  2. 把它当作 u_texture 喂给 BHShaders.java 里的同一份 frag
  3. 渲染输出 —— 场景被真实弯曲，而不是盖一层特效

输出 PNG / GIF 供演示。
"""
import os, sys, math, ctypes

os.environ.setdefault("EGL_PLATFORM", "surfaceless")
os.environ.setdefault("LIBGL_ALWAYS_SOFTWARE", "1")
os.environ.setdefault("GALLIUM_DRIVER", "llvmpipe")
# PyOpenGL 的上下文追踪在纯 EGL 下不可用，关掉它（必须在导入子模块前设置）
import OpenGL
OpenGL.CONTEXT_CHECKING = False

import numpy as np
from PIL import Image, ImageDraw
from OpenGL import GL
from OpenGL.EGL import *

sys.path.insert(0, os.path.dirname(__file__))
from check_shader import extract, FRAG_PREFIX, VERT_PREFIX

W, H = 480, 480
OUT = os.path.join(os.path.dirname(__file__), "..", "preview")
os.makedirs(OUT, exist_ok=True)


def gl_init():
    dpy = eglGetDisplay(EGL_DEFAULT_DISPLAY)
    a, b = ctypes.c_long(), ctypes.c_long()
    eglInitialize(dpy, a, b)
    eglBindAPI(EGL_OPENGL_ES_API)
    attribs = [EGL_SURFACE_TYPE, EGL_PBUFFER_BIT, EGL_BLUE_SIZE, 8, EGL_GREEN_SIZE, 8,
               EGL_RED_SIZE, 8, EGL_ALPHA_SIZE, 8, EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT, EGL_NONE]
    cfgs = (EGLConfig * 1)()
    n = ctypes.c_long()
    eglChooseConfig(dpy, (ctypes.c_int * len(attribs))(*attribs), cfgs, 1, n)
    ctx = eglCreateContext(dpy, cfgs[0], EGL_NO_CONTEXT, (ctypes.c_int * 3)(0x3098, 2, EGL_NONE))
    eglMakeCurrent(dpy, EGL_NO_SURFACE, EGL_NO_SURFACE, ctx)
    return dpy


def make_scene():
    """画一张假的 Mindustry 场景，用来证明被弯曲的是真实画面内容。"""
    img = Image.new("RGB", (W, H), (32, 34, 40))
    d = ImageDraw.Draw(img)

    # 地面网格（最能体现扭曲）
    step = 24
    for i in range(0, W + 1, step):
        d.line([(i, 0), (i, H)], fill=(58, 62, 72), width=1)
        d.line([(0, i), (W, i)], fill=(58, 62, 72), width=1)

    # 深色地形块
    for (bx, by, bw, bh, c) in [
        (40, 40, 90, 70, (46, 52, 46)), (330, 60, 110, 90, (52, 46, 44)),
        (60, 330, 120, 100, (44, 48, 56)), (300, 330, 130, 110, (50, 44, 52)),
    ]:
        d.rectangle([bx, by, bx + bw, by + bh], fill=c)

    # 传送带（长直线，扭曲时最明显）
    for y in (150, 330):
        d.rectangle([0, y, W, y + 10], fill=(120, 116, 100))
        for x in range(0, W, 14):
            d.rectangle([x + 3, y + 2, x + 9, y + 8], fill=(180, 174, 150))
    for x in (150, 330):
        d.rectangle([x, 0, x + 10, H], fill=(120, 116, 100))
        for y in range(0, H, 14):
            d.rectangle([x + 2, y + 3, x + 8, y + 9], fill=(180, 174, 150))

    # 建筑方块
    for (bx, by, col) in [(200, 90, (200, 170, 90)), (90, 200, (90, 170, 200)),
                          (380, 200, (170, 200, 90)), (200, 390, (200, 110, 140))]:
        d.rectangle([bx, by, bx + 34, by + 34], fill=col, outline=(20, 20, 24), width=2)
        d.rectangle([bx + 8, by + 8, bx + 26, by + 26], fill=(245, 245, 245))

    # 单位（小圆点）
    for (ux, uy) in [(120, 120), (360, 140), (140, 360), (370, 370), (250, 250)]:
        d.ellipse([ux - 7, uy - 7, ux + 7, uy + 7], fill=(230, 230, 240), outline=(30, 30, 40), width=2)

    return img


def compile_program():
    vert = VERT_PREFIX + extract("vert")
    frag = FRAG_PREFIX + extract("frag")

    def sh(kind, src):
        s = GL.glCreateShader(kind)
        GL.glShaderSource(s, src)
        GL.glCompileShader(s)
        if not GL.glGetShaderiv(s, GL.GL_COMPILE_STATUS):
            raise SystemExit(GL.glGetShaderInfoLog(s).decode())
        return s

    p = GL.glCreateProgram()
    GL.glAttachShader(p, sh(GL.GL_VERTEX_SHADER, vert))
    GL.glAttachShader(p, sh(GL.GL_FRAGMENT_SHADER, frag))
    GL.glLinkProgram(p)
    if not GL.glGetProgramiv(p, GL.GL_LINK_STATUS):
        raise SystemExit(GL.glGetProgramInfoLog(p).decode())
    return p


def render(prog, tex, fbo, holes, t):
    """holes: [(wx, wy, radius, strength)] 世界坐标；相机覆盖 0..W, 0..H"""
    GL.glBindFramebuffer(GL.GL_FRAMEBUFFER, fbo)
    GL.glViewport(0, 0, W, H)
    GL.glClearColor(0, 0, 0, 1)
    GL.glClear(GL.GL_COLOR_BUFFER_BIT)
    GL.glUseProgram(prog)

    GL.glActiveTexture(GL.GL_TEXTURE0)
    GL.glBindTexture(GL.GL_TEXTURE_2D, tex)
    GL.glUniform1i(GL.glGetUniformLocation(prog, "u_texture"), 0)
    GL.glUniform2f(GL.glGetUniformLocation(prog, "u_campos"), 0.0, 0.0)
    GL.glUniform2f(GL.glGetUniformLocation(prog, "u_camsize"), float(W), float(H))
    GL.glUniform1f(GL.glGetUniformLocation(prog, "u_time"), t)
    GL.glUniform1i(GL.glGetUniformLocation(prog, "u_count"), len(holes))
    if holes:
        flat = np.array(holes, dtype=np.float32).flatten()
        GL.glUniform4fv(GL.glGetUniformLocation(prog, "u_holes[0]"), len(holes), flat)

    # 全屏三角形带
    verts = np.array([-1, -1, 0, 0,  1, -1, 1, 0,  -1, 1, 0, 1,  1, 1, 1, 1], dtype=np.float32)
    pos = GL.glGetAttribLocation(prog, "a_position")
    uv = GL.glGetAttribLocation(prog, "a_texCoord0")
    vbo = GL.glGenBuffers(1)
    GL.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo)
    GL.glBufferData(GL.GL_ARRAY_BUFFER, verts.nbytes, verts, GL.GL_STATIC_DRAW)
    # 用 raw 入口，绕开 PyOpenGL 的 contextdata 追踪（纯 EGL 下不可用）
    from OpenGL.raw.GL.VERSION.GL_2_0 import glVertexAttribPointer as rawVAP
    GL.glEnableVertexAttribArray(pos)
    rawVAP(pos, 2, GL.GL_FLOAT, GL.GL_FALSE, 16, ctypes.c_void_p(0))
    GL.glEnableVertexAttribArray(uv)
    rawVAP(uv, 2, GL.GL_FLOAT, GL.GL_FALSE, 16, ctypes.c_void_p(8))
    GL.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4)
    GL.glDeleteBuffers(1, [vbo])

    buf = GL.glReadPixels(0, 0, W, H, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE)
    arr = np.frombuffer(buf, dtype=np.uint8).reshape(H, W, 4)
    return Image.fromarray(arr[::-1, :, :3])


def main():
    gl_init()
    print("GPU:", GL.glGetString(GL.GL_RENDERER).decode())
    prog = compile_program()
    print("着色器编译链接成功")

    scene = make_scene()
    scene.save(os.path.join(OUT, "lens_scene_before.png"))

    data = np.array(scene.convert("RGBA"), dtype=np.uint8)[::-1]
    tex = GL.glGenTextures(1)
    GL.glBindTexture(GL.GL_TEXTURE_2D, tex)
    GL.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR)
    GL.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR)
    GL.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE)
    GL.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE)
    GL.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGBA, W, H, 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, data)

    ctex = GL.glGenTextures(1)
    GL.glBindTexture(GL.GL_TEXTURE_2D, ctex)
    GL.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGBA, W, H, 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, None)
    GL.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR)
    GL.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR)
    fbo = GL.glGenFramebuffers(1)
    GL.glBindFramebuffer(GL.GL_FRAMEBUFFER, fbo)
    GL.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0, GL.GL_TEXTURE_2D, ctex, 0)
    assert GL.glCheckFramebufferStatus(GL.GL_FRAMEBUFFER) == GL.GL_FRAMEBUFFER_COMPLETE

    # 单个黑洞静图
    img = render(prog, tex, fbo, [(240, 240, 26, 1.25)], 0)
    img.save(os.path.join(OUT, "lens_scene_after.png"))
    print("已输出 lens_scene_before/after.png")

    # 对比图（前后并排）
    cmp = Image.new("RGB", (W * 2 + 12, H), (0, 0, 0))
    cmp.paste(scene, (0, 0))
    cmp.paste(img, (W + 12, 0))
    d = ImageDraw.Draw(cmp)
    d.text((10, 10), "BEFORE (raw scene)", fill=(255, 255, 255))
    d.text((W + 22, 10), "AFTER (real lensing)", fill=(255, 220, 160))
    cmp.save(os.path.join(OUT, "lens_compare.png"))
    print("已输出 lens_compare.png")

    # 黑洞成长动画：半径从小到大（演示炮塔新机制）
    frames = []
    N = 30
    for i in range(N):
        f = i / (N - 1)
        grow = f ** 0.62
        close = 1.0 - max(0.0, (f - 0.88) / 0.12)
        r = (3.5 + (30.0 - 3.5) * grow) * close
        frames.append(render(prog, tex, fbo, [(240, 240, r, 1.25)], i * 4.0))
    frames[0].save(os.path.join(OUT, "lens_growing.gif"), save_all=True,
                   append_images=frames[1:], duration=80, loop=0, optimize=True)
    print("已输出 lens_growing.gif")

    # 多个黑洞同屏（验证性能路径：一个 pass 处理多个）
    multi = render(prog, tex, fbo, [
        (130, 150, 17, 1.1), (350, 140, 13, 1.0),
        (150, 350, 12, 0.9), (360, 360, 19, 1.2),
    ], 30)
    multi.save(os.path.join(OUT, "lens_multi.png"))
    print("已输出 lens_multi.png (4 个黑洞，单 pass)")


if __name__ == "__main__":
    main()
