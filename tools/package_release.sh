#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# 把 8.7.2 的交付物打成一个完整压缩包，并铺到 /home/user 根目录。
#
#   bash tools/package_release.sh [版本号，默认读 mod.hjson]
#
# 压缩包结构（与 8.7.1 一致）：
#   Singularity-<版本>/
#     ├── Singularity-<版本>.jar    双端安装包（桌面 class + 安卓 classes.dex）
#     ├── report/                   报告 + 无头实测读数 + 图表
#     └── source/                   完整源码（不含 build/ dist/ .git/）
# v8.7.7 起按要求不再嵌入 previous-work（旧版项目包）。
# ---------------------------------------------------------------------------
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
VER="${1:-$(grep -m1 '^version:' "$ROOT/mod.hjson" | sed 's/version: *//; s/"//g')}"
NAME="Singularity-${VER}"
TAG="v${VER//./}"   # 8.7.2 -> v872
OUT="/home/user/Singularity-v${VER}-full.zip"
TMP="/tmp/pkg-${VER}"

echo "== version ${VER} =="
[ -f "$ROOT/dist/${NAME}.jar" ] || { echo "missing $ROOT/dist/${NAME}.jar (run build.sh first)"; exit 1; }

rm -rf "$TMP"; mkdir -p "$TMP/$NAME"/{source,report/charts}

# 1) previous-work 已按 v8.7.7 要求移除（不再嵌入旧版全量包）

# 2) 源码
( cd "$ROOT" && tar cf - --exclude=./build --exclude=./dist --exclude=./.git . ) | ( cd "$TMP/$NAME/source" && tar xf - )

# 3) jar
cp "$ROOT/dist/${NAME}.jar" "$TMP/$NAME/"

# 4) 报告 / 审计 / 图
cp "$ROOT/docs/REPORT-v${VER}.md" "$TMP/$NAME/report/" 2>/dev/null || true
cp "$ROOT/docs/${TAG}/audit.txt" "$TMP/$NAME/report/audit.txt"
cp "$ROOT"/docs/${TAG}/charts/*.png "$TMP/$NAME/report/charts/"
[ -f "$ROOT/docs/${TAG}/demo.mp4" ] && cp "$ROOT/docs/${TAG}/demo.mp4" "$TMP/$NAME/report/demo.mp4"

# 5) 打包（先在 /tmp 生成，校验通过再复制到 /home/user —— 避免半截文件）
rm -f "$TMP.zip"
( cd "$TMP" && zip -qr "$TMP.zip" "$NAME" )
python3 -c "import zipfile,sys; z=zipfile.ZipFile('$TMP.zip'); assert z.testzip() is None; print('zip entries', len(z.namelist()))"
rm -f "$OUT"
cp "$TMP.zip" "$OUT"

echo "== 交付物 =="
ls -la "$OUT" "$TMP/$NAME/${NAME}.jar"
echo "files: $(unzip -l "$OUT" | tail -1 | awk '{print $2}')"

# 6) 铺到 /home/user 根目录，并清掉上一版
cp "$TMP/$NAME/${NAME}.jar" /home/user/
cp "$TMP/$NAME/report/REPORT-v${VER}.md" /home/user/ 2>/dev/null || true
cp "$TMP/$NAME/report/audit.txt" "/home/user/AUDIT-v${VER}.txt" 2>/dev/null || true
find /home/user/charts -mindepth 1 -delete 2>/dev/null || true
mkdir -p /home/user/charts
cp "$TMP/$NAME/report/charts/"*.png /home/user/charts/ 2>/dev/null || true

# 上一版的交付物必须清掉，只留当前版本
for old in /home/user/Singularity-8.7.1.jar /home/user/Singularity-v8.7.1-full.zip \
            /home/user/REPORT-v8.7.1.md /home/user/AUDIT-v8.7.1.txt \
            /home/user/Singularity-8.7.2.jar /home/user/Singularity-v8.7.2-full.zip \
            /home/user/REPORT-v8.7.2.md /home/user/AUDIT-v8.7.2.txt \
            /home/user/AUDIT-8.7.2.txt \
            /home/user/Singularity-8.7.4.jar /home/user/Singularity-v8.7.4-full.zip \
            /home/user/REPORT-v8.7.4.md /home/user/AUDIT-v8.7.4.txt; do
  [ -e "$old" ] && rm -f "$old" && echo "removed old deliverable: $old"
done

echo "== /home/user =="
ls -la /home/user | grep -iE "singularity|report|audit|charts"
du -sh /home/user/Singularity
