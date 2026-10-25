#!/usr/bin/env bash
# Runs a real server and client in a fresh world, and checks that:
#   1. Login: the client gets a level init with the expected level key.
#   2. LOD generation: the server sees the player using DH and generates LODs.
#   3. Dimension switch: moving to the nether gives a new level init, without errors.
# After each step, the server log is checked for exceptions, and the client chat for errors the
# plugin reports to the player.
#
# Needs a desktop session, as the client opens a window. Exits non-zero if any check fails.
#
# Environment:
#   LEVEL_NAME   Name of the main world. Defaults to "DH Test World".
#   SKIP_BUILD   Set to 1 to reuse the last built plugin jar.
#   PLUGIN_JAR   Test this plugin jar instead of building one.
#   KEEP_RUN     Set to 1 to keep testbed/run/test after a passing run. It is always kept after a failure.

set -uo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

require_setup

export RUN_DIR="$TESTBED_DIR/run/test"
export LEVEL_NAME="${LEVEL_NAME:-DH Test World}"
export PLAYER_NAME

SERVER_DIR="$RUN_DIR/server"
CLIENT_DIR="$RUN_DIR/client"

SERVER_OUT="$RUN_DIR/server.out"
CLIENT_OUT="$RUN_DIR/client.out"
CONSOLE="$RUN_DIR/console"

LEVEL_KEY="$(echo "$LEVEL_NAME" | sed 's/[^a-zA-Z0-9_-]//g')"

SERVER_PID=""
CLIENT_PID=""
FAILURES=0

#===========#
# reporting #
#===========#

pass()
{
    echo "PASS  $1"
}

fail()
{
    echo "FAIL  $1"
    [[ $# -gt 1 ]] && echo "      $2"
    FAILURES=$((FAILURES + 1))
}

abort()
{
    fail "$@"
    exit 1
}

#=========#
# helpers #
#=========#

# Log text without ANSI colour codes, from an optional line number onwards.
log_text()
{
    local file="$1" from="${2:-1}"

    tail -n "+$from" "$file" 2>/dev/null | sed 's/\x1b\[[0-9;]*m//g'
}

line_count()
{
    wc -l < "$1" 2>/dev/null || echo 0
}

# wait_for <file> <regex> <timeout seconds> [from line]
wait_for()
{
    local file="$1" pattern="$2" timeout="$3" from="${4:-1}"
    local deadline=$((SECONDS + timeout))

    while ((SECONDS < deadline)); do
        if log_text "$file" "$from" | grep -qE "$pattern"; then
            return 0
        fi

        if [[ -n "$SERVER_PID" ]] && ! kill -0 "$SERVER_PID" 2>/dev/null; then
            abort "Server exited unexpectedly" "See $SERVER_OUT"
        fi

        if [[ -n "$CLIENT_PID" ]] && ! kill -0 "$CLIENT_PID" 2>/dev/null; then
            abort "Client exited unexpectedly" "See $CLIENT_OUT"
        fi

        sleep 1
    done

    return 1
}

console()
{
    echo "$1" >&3
}

# Fails the given check if the server has logged exceptions or errors since the given line.
check_server_errors()
{
    local check="$1" from="$2"
    local errors

    errors="$(log_text "$SERVER_OUT" "$from" | grep -E "Exception|ERROR\]|Cannot invoke" | head -n 5)"

    if [[ -n "$errors" ]]; then
        fail "$check: server logged errors" "$(echo "$errors" | head -n 1)"
        return 1
    fi

    return 0
}

# Fails the given check if the plugin has sent the player error messages since the given line.
# PluginMessageProxy catches exceptions from message handlers and sends them to the player's chat,
# so they never reach the server log. DH's own client messages have a colour code and don't match.
check_client_errors()
{
    local check="$1" from="$2"
    local errors

    errors="$(log_text "$CLIENT_OUT" "$from" | grep -aE "\[CHAT\] (§c)?Distant Horizons: " | head -n 5)"

    if [[ -n "$errors" ]]; then
        fail "$check: plugin sent errors to the player" "$(echo "$errors" | head -n 1 | sed -E 's/.*\[CHAT\] //')"
        return 1
    fi

    return 0
}

stop_all()
{
    if [[ -n "$CLIENT_PID" ]] && kill -0 "$CLIENT_PID" 2>/dev/null; then
        kill -- "-$CLIENT_PID" 2>/dev/null
    fi

    if [[ -n "$SERVER_PID" ]] && kill -0 "$SERVER_PID" 2>/dev/null; then
        console "stop" 2>/dev/null

        for _ in $(seq 1 60); do
            kill -0 "$SERVER_PID" 2>/dev/null || break
            sleep 1
        done

        kill -- "-$SERVER_PID" 2>/dev/null
    fi

    exec 3>&- 2>/dev/null

    if ((FAILURES == 0)) && [[ "${KEEP_RUN:-0}" != "1" ]]; then
        rm -rf "$RUN_DIR"
    else
        echo "Logs kept in $RUN_DIR"
    fi
}

#=======#
# setup #
#=======#

rm -rf "$RUN_DIR"
mkdir -p "$RUN_DIR"
mkfifo "$CONSOLE"

trap stop_all EXIT
trap 'exit 130' INT TERM

echo "Starting server with world \"$LEVEL_NAME\" (expected level key: $LEVEL_KEY)..."

# setsid puts each side in its own process group, so cleanup can stop everything they started.
setsid "$TESTBED_DIR/server.sh" < "$CONSOLE" > "$SERVER_OUT" 2>&1 &
SERVER_PID=$!

# Hold the console pipe open for the whole run. Closing it would end the server's input.
exec 3> "$CONSOLE"

wait_for "$SERVER_OUT" 'Done \(' 300 || abort "Server did not start within 300 seconds" "See $SERVER_OUT"
check_server_errors "Server startup" 1 || exit 1

#==========#
# 1. login #
#==========#

echo "Starting client..."

SERVER_MARK=$(line_count "$SERVER_OUT")

setsid "$TESTBED_DIR/client.sh" > "$CLIENT_OUT" 2>&1 &
CLIENT_PID=$!

if ! wait_for "$SERVER_OUT" "$PLAYER_NAME.*joined the game" 300 "$SERVER_MARK"; then
    abort "Login: client did not join within 300 seconds" "$(log_text "$SERVER_OUT" "$SERVER_MARK" | grep -E "lost connection" | head -n 1)"
fi

if ! wait_for "$CLIENT_OUT" "Level init received for \[minecraft:overworld\]" 60; then
    abort "Login: client got no level init for the overworld within 60 seconds"
fi

# Give the client time to send its first LOD requests.
sleep 10

ACTUAL_KEY="$(log_text "$CLIENT_OUT" | grep -oE "Level init received for \[minecraft:overworld\]: .*level key \[[^]]*\]" | tail -n 1 | sed -E 's/.*level key \[([^]]*)\]/\1/')"

if [[ "$ACTUAL_KEY" == "$LEVEL_KEY" ]]; then
    if log_text "$CLIENT_OUT" | grep -q "invalid level key"; then
        fail "Login: client rejected the level key"
    elif check_server_errors "Login" "$SERVER_MARK" && check_client_errors "Login" 1; then
        pass "Login: overworld level key is $ACTUAL_KEY"
    fi
else
    fail "Login: wrong level key" "Expected $LEVEL_KEY, got $ACTUAL_KEY"
fi

#===================#
# 2. LOD generation #
#===================#

echo "Waiting for LOD generation..."

SERVER_MARK=$(line_count "$SERVER_OUT")
CLIENT_MARK=$(line_count "$CLIENT_OUT")
LOD_OK=0
DEADLINE=$((SECONDS + 120))

while ((SECONDS < DEADLINE)); do
    STATUS_MARK=$(line_count "$SERVER_OUT")
    console "dhs status"
    sleep 5

    STATUS="$(log_text "$SERVER_OUT" "$((STATUS_MARK + 1))")"
    SPEED="$(echo "$STATUS" | grep -oE "Current generation speed: [0-9.]+" | tail -n 1 | grep -oE "[0-9.]+$")"

    if echo "$STATUS" | grep -qE "using Distant Horizons: .*$PLAYER_NAME" \
        && [[ -n "$SPEED" ]] && awk "BEGIN { exit !($SPEED > 0) }"; then
        LOD_OK=1
        break
    fi
done

if ((LOD_OK)); then
    check_server_errors "LOD generation" "$SERVER_MARK" \
        && check_client_errors "LOD generation" "$CLIENT_MARK" \
        && pass "LOD generation: $SPEED chunks per second"
else
    fail "LOD generation: no generation for $PLAYER_NAME within 120 seconds" "Last speed: ${SPEED:-none}"
    check_client_errors "LOD generation" "$CLIENT_MARK"
fi

#=====================#
# 3. dimension switch #
#=====================#

echo "Switching to the nether..."

SERVER_MARK=$(line_count "$SERVER_OUT")
CLIENT_MARK=$(line_count "$CLIENT_OUT")

# Creative, so the player can't suffocate if they land inside netherrack.
console "gamemode creative $PLAYER_NAME"
console "execute in minecraft:the_nether run tp $PLAYER_NAME 0 100 0"

if wait_for "$CLIENT_OUT" "Level init received for \[minecraft:the_nether\]" 60 "$CLIENT_MARK"; then
    NETHER_KEY="$(log_text "$CLIENT_OUT" "$CLIENT_MARK" | grep -oE "Level init received for \[minecraft:the_nether\]: .*level key \[[^]]*\]" | tail -n 1 | sed -E 's/.*level key \[([^]]*)\]/\1/')"

    # Give the client time to send LOD requests for the new level.
    sleep 20

    if [[ "$NETHER_KEY" != "${LEVEL_KEY}_nether" ]]; then
        fail "Dimension switch: wrong nether level key" "Expected ${LEVEL_KEY}_nether, got $NETHER_KEY"
    elif check_server_errors "Dimension switch" "$SERVER_MARK" && check_client_errors "Dimension switch" "$CLIENT_MARK"; then
        pass "Dimension switch: nether level key is $NETHER_KEY"
    fi
else
    fail "Dimension switch: client got no level init for the nether within 60 seconds"
fi

#=========#
# summary #
#=========#

if ((FAILURES > 0)); then
    echo "$FAILURES check(s) failed."
    exit 1
fi

echo "All checks passed."
