#!/usr/bin/env bash
# Builds the plugin and runs a Paper server with it in the foreground. Type "stop" to shut down.
#
# Environment:
#   LEVEL_NAME   Name of the main world. Defaults to a name with a space in it,
#                since world names like that have broken level key lookups before.
#   SKIP_BUILD   Set to 1 to reuse the last built plugin jar.
#   PLUGIN_JAR   Use this plugin jar instead of building one. Implies SKIP_BUILD.
#   PLAYER_NAME  Player to make operator, so /dhs works in game. Defaults to DhTester.
#   SERVER_KEY   Sets server_key in the plugin config when it is first created.

set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

require_setup

LEVEL_NAME="${LEVEL_NAME:-DH Test World}"

if [[ -z "${PLUGIN_JAR:-}" ]]; then
    if [[ "${SKIP_BUILD:-0}" != "1" ]]; then
        (cd "$PROJECT_DIR" && ./gradlew --quiet build -x test)
    fi

    PLUGIN_JAR="$(ls -t "$PROJECT_DIR"/build/libs/1.16.5/DistantHorizonsSupport-*.jar | head -n 1)"
fi

mkdir -p "$SERVER_DIR/plugins"

rm -f "$SERVER_DIR"/plugins/DistantHorizonsSupport-*.jar
cp "$PLUGIN_JAR" "$SERVER_DIR/plugins/"

# Start from the plugin's default config, so settings that aren't overridden keep their defaults.
if [[ -n "${SERVER_KEY:-}" && ! -f "$SERVER_DIR/plugins/DHSupport/config.yml" ]]; then
    mkdir -p "$SERVER_DIR/plugins/DHSupport"

    python3 -I - "$PLUGIN_JAR" "$SERVER_DIR/plugins/DHSupport/config.yml" "$SERVER_KEY" <<'EOF'
import json, sys, zipfile

plugin_jar, config_path, server_key = sys.argv[1:]

with zipfile.ZipFile(plugin_jar) as jar:
    config = jar.read('config.yml').decode()

# A JSON string is also a valid YAML string, with any quotes escaped.
with open(config_path, 'w') as file:
    file.write(config.rstrip('\n') + '\n\nserver_key: ' + json.dumps(server_key) + '\n')
EOF
fi

echo "eula=true" > "$SERVER_DIR/eula.txt"

# Only written once, so manual changes to server.properties are kept between runs.
if [[ ! -f "$SERVER_DIR/server.properties" ]]; then
    # Non-ASCII characters are written as \uXXXX escapes, which Java's properties format always understands.
    ESCAPED_LEVEL_NAME="$(python3 -I -c 'import sys; print("".join(c if ord(c) < 128 else "\\u%04x" % ord(c) for c in sys.argv[1]))' "$LEVEL_NAME")"

    cat > "$SERVER_DIR/server.properties" <<EOF
level-name=$ESCAPED_LEVEL_NAME
online-mode=false
white-list=false
motd=DH Support testbed
spawn-protection=0
view-distance=8
EOF
fi

# Commands like /dhs need operator. In offline mode the UUID is derived from the name, like Java's UUID.nameUUIDFromBytes().
if [[ ! -f "$SERVER_DIR/ops.json" ]] || ! grep -q "\"$PLAYER_NAME\"" "$SERVER_DIR/ops.json"; then
    PLAYER_UUID="$(python3 -c '
import hashlib, sys, uuid
digest = bytearray(hashlib.md5(("OfflinePlayer:" + sys.argv[1]).encode()).digest())
digest[6] = digest[6] & 0x0f | 0x30
digest[8] = digest[8] & 0x3f | 0x80
print(uuid.UUID(bytes=bytes(digest)))
' "$PLAYER_NAME")"

    cat > "$SERVER_DIR/ops.json" <<EOF
[
  {
    "uuid": "$PLAYER_UUID",
    "name": "$PLAYER_NAME",
    "level": 4,
    "bypassesPlayerLimit": false
  }
]
EOF
fi

echo "Starting Paper $MC_VERSION with $(basename "$PLUGIN_JAR")..."

# Java encodes file paths using the locale. Without UTF-8, it can't create worlds with names like "世界".
export LC_ALL=C.UTF-8

cd "$SERVER_DIR"
exec "$JAVA" -Xms2G -Xmx4G -jar "$PAPER_JAR" --nogui
