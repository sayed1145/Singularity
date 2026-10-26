#!/usr/bin/env python3
"""用真实 OpenGL(EGL 离屏) 上下文编译模组里的 GLSL，确保着色器不会在手机上炸掉。

从 BHShaders.java 里抽出 vert/frag 源码，按 Arc 的 Shader 预处理规则加上前缀，
然后真正走 glCompileShader + glLinkProgram。
"""
import re, sys, os, ctypes

# 必须在导入 OpenGL 之前设置：用 Mesa 软件渲染 + 无显示设备模式
os.environ.setdefault("EGL_PLATFORM", "surfaceless")
os.environ.setdefault("LIBGL_ALWAYS_SOFTWARE", "1")
os.environ.setdefault("GALLIUM_DRIVER", "llvmpipe")

from OpenGL import EGL
from OpenGL import GL
from OpenGL.EGL import (
    eglGetDisplay, eglInitialize, eglChooseConfig, eglCreateContext,
    eglMakeCurrent, eglBindAPI, eglCreatePbufferSurface,
    EGL_DEFAULT_DISPLAY, EGL_NO_CONTEXT, EGL_SURFACE_TYPE, EGL_PBUFFER_BIT,
    EGL_BLUE_SIZE, EGL_GREEN_SIZE, EGL_RED_SIZE, EGL_ALPHA_SIZE, EGL_DEPTH_SIZE,
    EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT, EGL_NONE, EGL_WIDTH, EGL_HEIGHT,
    EGL_OPENGL_ES_API, EGLConfig,
)

SRC = os.path.join(os.path.dirname(__file__), "..", "src", "blackhole", "BHShaders.java")


def extract(name):
    """从 Java 文件里抽出 text block 形式的着色器源码。"""
    s = open(SRC, encoding="utf-8").read()
    m = re.search(r'String\s+' + name + r'\s*=\s*"""\n(.*?)\n\s*""";', s, re.S)
    if not m:
        raise SystemExit(f"找不到着色器源码: {name}")
    body = m.group(1)
    # Java text block 会去掉公共缩进
    lines = body.split("\n")
    indents = [len(l) - len(l.lstrip()) for l in lines if l.strip()]
    pad = min(indents) if indents else 0
    return "\n".join(l[pad:] if len(l) >= pad else l for l in lines)


def make_context():
    dpy = eglGetDisplay(EGL_DEFAULT_DISPLAY)
    major, minor = ctypes.c_long(), ctypes.c_long()
    if not eglInitialize(dpy, major, minor):
        raise SystemExit("eglInitialize 失败")
    eglBindAPI(EGL_OPENGL_ES_API)
    attribs = [
        EGL_SURFACE_TYPE, EGL_PBUFFER_BIT,
        EGL_BLUE_SIZE, 8, EGL_GREEN_SIZE, 8, EGL_RED_SIZE, 8,
        EGL_ALPHA_SIZE, 8, EGL_DEPTH_SIZE, 8,
        EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
        EGL_NONE,
    ]
    cfgs = (EGLConfig * 1)()
    n = ctypes.c_long()
    arr = (ctypes.c_int * len(attribs))(*attribs)
    if not eglChooseConfig(dpy, arr, cfgs, 1, n) or n.value == 0:
        raise SystemExit("eglChooseConfig 失败")
    ctx_attribs = (ctypes.c_int * 3)(0x3098, 2, EGL_NONE)  # EGL_CONTEXT_CLIENT_VERSION=2
    ctx = eglCreateContext(dpy, cfgs[0], EGL_NO_CONTEXT, ctx_attribs)
    if not ctx:
        raise SystemExit("eglCreateContext 失败")
    # surfaceless 平台无需 surface
    from OpenGL.EGL import EGL_NO_SURFACE
    if not eglMakeCurrent(dpy, EGL_NO_SURFACE, EGL_NO_SURFACE, ctx):
        raise SystemExit("eglMakeCurrent 失败")
    return dpy


# Arc/Shader.java 的预处理：GLES 下补精度限定符
VERT_PREFIX = ""
FRAG_PREFIX = """#ifdef GL_ES
precision mediump float;
precision mediump int;
#else
#define lowp
#define mediump
#define highp
#endif
"""


def compile_one(kind, src, label):
    sid = GL.glCreateShader(kind)
    GL.glShaderSource(sid, src)
    GL.glCompileShader(sid)
    ok = GL.glGetShaderiv(sid, GL.GL_COMPILE_STATUS)
    log = GL.glGetShaderInfoLog(sid)
    if isinstance(log, bytes):
        log = log.decode(errors="replace")
    if not ok:
        print(f"\n❌ {label} 编译失败:\n{log}")
        numbered = "\n".join(f"{i+1:3d}| {l}" for i, l in enumerate(src.split("\n")))
        print(numbered)
        return None
    if log.strip():
        print(f"⚠️  {label} 警告: {log.strip()}")
    print(f"✅ {label} 编译通过")
    return sid


def main():
    make_context()
    print("GL_VERSION :", GL.glGetString(GL.GL_VERSION).decode())
    print("GLSL       :", GL.glGetString(GL.GL_SHADING_LANGUAGE_VERSION).decode())
    print()

    vert = extract("vert")
    frag = extract("frag")

    vs = compile_one(GL.GL_VERTEX_SHADER, VERT_PREFIX + vert, "vertex shader")
    fs = compile_one(GL.GL_FRAGMENT_SHADER, FRAG_PREFIX + frag, "fragment shader")
    if not vs or not fs:
        sys.exit(1)

    prog = GL.glCreateProgram()
    GL.glAttachShader(prog, vs)
    GL.glAttachShader(prog, fs)
    GL.glLinkProgram(prog)
    if not GL.glGetProgramiv(prog, GL.GL_LINK_STATUS):
        log = GL.glGetProgramInfoLog(prog)
        print("❌ 链接失败:", log.decode() if isinstance(log, bytes) else log)
        sys.exit(1)
    print("✅ 程序链接通过")

    # 检查 Java 侧设置的 uniform 是否都真实存在
    print("\n--- uniform 检查（Java 侧会 set 这些）---")
    expected = ["u_texture", "u_campos", "u_camsize", "u_time", "u_count", "u_holes"]
    missing = []
    for u in expected:
        loc = GL.glGetUniformLocation(prog, u)
        # 数组 uniform 要查 [0]
        if loc == -1 and u == "u_holes":
            loc = GL.glGetUniformLocation(prog, "u_holes[0]")
        status = "OK" if loc != -1 else "缺失/被优化掉"
        print(f"  {u:<12} loc={loc:<4} {status}")
        if loc == -1:
            missing.append(u)

    if missing:
        print(f"\n⚠️  注意: {missing} 未找到。若是未被使用会被 GLSL 优化掉，setUniform 时可能报错。")
    print("\n🎉 着色器验证完成 —— 可以安全上机。")


if __name__ == "__main__":
    main()
