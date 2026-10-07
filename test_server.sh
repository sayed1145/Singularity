#!/usr/bin/env bash
# v8.4 official Mindustry v160.5 integration tests; replaces obsolete restoration-era probes.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
exec python3 "$ROOT/tests/run.py" "$@"
