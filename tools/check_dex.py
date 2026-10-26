#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""校验包里的 classes.dex 是不是一份安卓认得出的合法 dex。

用法: python3 tools/check_dex.py <模组jar或zip>

为什么要单独验一遍：d8 退出码为 0 不代表产物可用（比如被打断会留下半个文件）。
这里按 Android 官方《Dalvik executable format》
(https://source.android.com/docs/core/runtime/dex-format) 把头部、字符串表和校验和走一遍：

* magic 必须是 "dex\\n035\\0"（安卓只认 035 这一版）；
* header_size 必须是 0x70；
* endian_tag 必须是 0x12345678（小端，REVERSE_ENDIAN_CONSTANT 0x78563412 不被支持）；
* file_size 必须等于实际字节数；
* map_off / string_ids_off 必须落在文件范围内；
* 抽查若干字符串项：uleb128 长度 + 结尾 0 字节，且整项不出界；
* adler32 校验和（跳过 magic+checksum 这 12 字节）与 SHA-1 签名（跳过 magic+checksum+signature 这 32 字节）。
"""
import hashlib
import struct
import sys
import zlib

DEX_MAGIC = b"dex\n035\x00"
HEADER_SIZE = 0x70
ENDIAN_TAG = 0x12345678


def uleb128(data, off):
    """读一个 uleb128（官方规范：最多 5 字节，高位为 1 表示还有后续字节）。"""
    result = 0
    shift = 0
    for i in range(5):
        byte = data[off + i]
        result |= (byte & 0x7F) << shift
        if byte < 0x80:
            return result, off + i + 1
        shift += 7
    raise ValueError("uleb128 超过 5 字节")


def check(data):
    """返回 (是否全部通过, [(是否通过, 说明), ...])"""
    out = []

    def say(ok, msg):
        out.append((ok, msg))

    if len(data) < HEADER_SIZE:
        return False, [(False, "文件太短（%d 字节），连 dex 头都放不下" % len(data))]

    magic = data[:8]
    say(magic == DEX_MAGIC, "magic 正确：%r（安卓只认 dex\\n035\\0）" % magic[:8])
    header_size, = struct.unpack_from("<I", data, 36)
    endian, = struct.unpack_from("<I", data, 40)
    file_size, = struct.unpack_from("<I", data, 32)
    say(header_size == HEADER_SIZE, "header_size = 0x%x（应为 0x%x）" % (header_size, HEADER_SIZE))
    say(endian == ENDIAN_TAG, "endian_tag = 0x%x（小端，安卓要求）" % endian)
    say(file_size == len(data), "file_size = %d，实际 %d 字节" % (file_size, len(data)))

    map_off, str_size, str_off, type_size, type_off, proto_size, proto_off, \
        field_size, field_off, method_size, method_off, class_size, class_off, \
        data_size, data_off = struct.unpack_from("<15I", data, 52)

    say(0 < map_off < len(data), "map_off = %d 在文件范围内" % map_off)
    say(str_size > 0, "string_ids_size = %d（不是空 dex）" % str_size)
    say(0 < str_off < len(data), "string_ids_off = %d 在文件范围内" % str_off)
    say(0 < class_off < len(data) and class_size > 0, "class_defs：%d 个类，偏移 %d" % (class_size, class_off))
    say(data_off + data_size <= len(data), "data 段 %d..%d 未越界" % (data_off, data_off + data_size))

    # 字符串表：string_id_item 指向 string_data_item = uleb128 的 UTF-16 长度
    # + MUTF-8 字节 + 一个 0 结尾。注意 MUTF-8 的**字节数不等于** UTF-16 长度
    # （非 ASCII 会编成 2~3 字节），所以要往前找结尾的 0，不能直接用长度去下标。
    if 0 < str_off < len(data) and str_size > 0:
        bad = 0
        sample = min(str_size, 4000)
        for i in range(sample):
            try:
                ptr, = struct.unpack_from("<I", data, str_off + i * 4)
                if not 0 < ptr < len(data):
                    bad += 1
                    continue
                utf16_len, start = uleb128(data, ptr)
                # MUTF-8 最长 3 字节表示一个 UTF-16 码元，多给几个字节余量
                limit = min(len(data), start + utf16_len * 3 + 4)
                term = -1
                for j in range(start, limit):
                    if data[j] == 0:
                        term = j
                        break
                if term < 0:
                    bad += 1
            except Exception:
                bad += 1
        say(bad == 0, "抽查 %d 条字符串项，全部以 0 结尾且不出界（坏 %d 条）" % (sample, bad))

    # 校验和 / 签名
    try:
        crc, = struct.unpack_from("<I", data, 8)
        got = zlib.adler32(data[12:]) & 0xFFFFFFFF
        say(crc == got, "adler32 校验和一致（%08x）" % crc)
    except Exception as exc:  # pragma: no cover
        say(False, "adler32 校验失败：%s" % exc)
    try:
        sig = data[12:32]
        got = hashlib.sha1(data[32:]).digest()
        say(sig == got, "SHA-1 签名一致（%s…）" % sig.hex()[:12])
    except Exception as exc:  # pragma: no cover
        say(False, "SHA-1 校验失败：%s" % exc)

    return all(ok for ok, _ in out), out


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        return 2
    import zipfile
    path = sys.argv[1]
    ok_all = True
    with zipfile.ZipFile(path) as z:
        names = z.namelist()
        print("包内条目数：%d" % len(names))
        cls = [n for n in names if n.endswith(".class") and not n.startswith("classes/")]
        print("  %s 包根 .class：%d 个（桌面/服务端 JVM 用）" % ("✓" if cls else "✗", len(cls)))
        ok_all = ok_all and bool(cls)
        if "classes.dex" not in names:
            print("  ✗ 没有 classes.dex，安卓端加载不到")
            return 1
        info = z.getinfo("classes.dex")
        print("  ✓ 包根 classes.dex：%d 字节（安卓 DexClassLoader 用）" % info.file_size)
        data = z.read("classes.dex")
    ok, lines = check(data)
    for good, msg in lines:
        print("  %s %s" % ("✓" if good else "✗", msg))
    print("\n结论：%s" % ("classes.dex 合法，安卓可加载" if ok and ok_all else "存在问题，安卓端可能加载失败"))
    return 0 if ok and ok_all else 1


if __name__ == "__main__":
    sys.exit(main())
