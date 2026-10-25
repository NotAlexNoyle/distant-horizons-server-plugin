#!/usr/bin/env bash
# Downloads everything the testbed needs into testbed/.cache. Safe to re-run.

set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

mkdir -p "$CACHE_DIR"

if [[ ! -x "$PORTABLEMC" ]] || ! "$PORTABLEMC" --version 2>/dev/null | grep -q "$PORTABLEMC_VERSION"; then
    echo "Installing portablemc $PORTABLEMC_VERSION..."
    python3 -m venv "$CACHE_DIR/venv"
    "$CACHE_DIR/venv/bin/pip" install --quiet --disable-pip-version-check "portablemc==$PORTABLEMC_VERSION"
fi

echo "Installing Minecraft $MC_VERSION with Fabric, and its Java runtime..."
"$PORTABLEMC" --main-dir "$MC_MAIN_DIR" start "fabric:$MC_VERSION" --dry > /dev/null

download "$PAPER_URL" "$PAPER_JAR" sha256 "$PAPER_SHA256"
download "$DH_URL" "$DH_JAR" sha512 "$DH_SHA512"

echo "Testbed is ready."
