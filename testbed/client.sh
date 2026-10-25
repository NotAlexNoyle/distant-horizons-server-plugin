#!/usr/bin/env bash
# Starts a Fabric client with Distant Horizons and joins the testbed server.
#
# Environment:
#   PLAYER_NAME  Offline-mode username. Defaults to DhTester. Use the same value for server.sh.
#   SERVER_HOST  Defaults to localhost.
#   SERVER_PORT  Defaults to 25565.

set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

require_setup

mkdir -p "$CLIENT_DIR/mods"

rm -f "$CLIENT_DIR"/mods/DistantHorizons-*.jar
cp "$DH_JAR" "$CLIENT_DIR/mods/"

# Skip first-launch screens and keep running when the window loses focus.
if [[ ! -f "$CLIENT_DIR/options.txt" ]]; then
    cat > "$CLIENT_DIR/options.txt" <<EOF
onboardAccessibility:false
pauseOnLostFocus:false
skipMultiplayerWarning:true
joinedFirstServer:true
EOF
fi

exec "$PORTABLEMC" --main-dir "$MC_MAIN_DIR" start "fabric:$MC_VERSION" \
    --mc-dir "$CLIENT_DIR" \
    --username "$PLAYER_NAME" \
    --join-server "${SERVER_HOST:-localhost}" \
    --join-server-port "${SERVER_PORT:-25565}"
