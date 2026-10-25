# Shared paths and helpers for the testbed scripts. Sourced, not executed.

TESTBED_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$TESTBED_DIR")"

source "$TESTBED_DIR/versions.env"

CACHE_DIR="$TESTBED_DIR/.cache"
RUN_DIR="${RUN_DIR:-$TESTBED_DIR/run}"

SERVER_DIR="$RUN_DIR/server"
CLIENT_DIR="$RUN_DIR/client"

MC_MAIN_DIR="$CACHE_DIR/minecraft"
PORTABLEMC="$CACHE_DIR/venv/bin/portablemc"

# Mojang's Java runtime, installed by portablemc along with the client. The server uses it too.
JAVA="$MC_MAIN_DIR/jvm/java-runtime-epsilon/bin/java"

# Offline-mode username, shared by the client and the server's op list.
PLAYER_NAME="${PLAYER_NAME:-DhTester}"

PAPER_JAR="$CACHE_DIR/paper-$MC_VERSION-$PAPER_BUILD.jar"
DH_JAR="$CACHE_DIR/DistantHorizons-$DH_VERSION.jar"

# download <url> <destination> <sha256|sha512> <hash>
download()
{
    local url="$1" dest="$2" algorithm="$3" expected="$4"

    if [[ -f "$dest" ]] && echo "$expected  $dest" | "${algorithm}sum" --check --status; then
        return
    fi

    echo "Downloading $(basename "$dest")..."
    curl --fail --location --silent --show-error --output "$dest.part" "$url"

    if ! echo "$expected  $dest.part" | "${algorithm}sum" --check --status; then
        rm -f "$dest.part"
        echo "Checksum mismatch for $url" >&2
        exit 1
    fi

    mv "$dest.part" "$dest"
}

require_setup()
{
    if [[ ! -x "$JAVA" || ! -f "$PAPER_JAR" || ! -f "$DH_JAR" || ! -x "$PORTABLEMC" ]]; then
        echo "Testbed is not set up. Run testbed/setup.sh first." >&2
        exit 1
    fi
}
