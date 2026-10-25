# Testbed

A real Paper server running the plugin, and a real Fabric client running Distant Horizons, for manual and end-to-end testing.

Pinned versions live in `versions.env`.

## Usage

```sh
testbed/setup.sh    # Once, and after bumping versions.env. Downloads into testbed/.cache.
testbed/server.sh   # Builds the plugin and runs the server in the foreground. Type "stop" to quit.
testbed/client.sh   # In a second terminal. Opens a game window and joins the server.
```

The server runs in offline mode with the whitelist off, and the client logs in offline as `DhTester`, who is made operator so `/dhs` works in game. Set `PLAYER_NAME` for both scripts to use another name.

By default the main world is called `DH Test World`. The space in the name tests level key sanitizing. Set `LEVEL_NAME` before the first `server.sh` run to use another name. `server.properties` is only written when it doesn't exist, so to change it later, edit the file or delete `testbed/run`.

Set `SKIP_BUILD=1` to start the server without rebuilding the plugin.

## Automated test

```sh
testbed/test.sh
```

Starts a server and client in a fresh world under `testbed/run/test`, runs four checks, shuts both down and exits non-zero if any check fails. A run takes about two minutes, and needs a desktop session since the client opens a window.

1. **Login**: the client gets a level init for the overworld with the expected level key.
2. **LOD generation**: `dhs status` shows the player using DH, with a generation speed above zero.
3. **Custom biome**: the world gets a datapack that makes `dhtest:test_biome`, a copy of plains, the only overworld biome. The plugin's stored LODs must contain that key and not `minecraft:custom`, and the server must not warn that custom biomes are unsupported.
4. **Dimension switch**: teleporting to the nether gives a level init with the nether's level key.

Each check also fails if the server logged exceptions, or if the plugin sent the player an error message in chat. Handler exceptions only show up in chat, not in the server log.

The run directory is deleted after a passing run, and kept after a failure for its `server.out` and `client.out` logs. Set `KEEP_RUN=1` to always keep it, and `PLUGIN_JAR` to test a prebuilt jar, such as an older version.

## Files

- `testbed/.cache`: launcher, game files, Java runtime, Paper and DH jars. Shared between runs.
- `testbed/run/server`, `testbed/run/client`: server and client game directories, including worlds, configs and logs.

Both are gitignored. Delete `testbed/run` for a fresh world and fresh configs.

## Useful log lines

- Client, `testbed/run/client/logs/latest.log`: `Level init received for [...]: server key [...], level key [...]`
- Server console: `dhs status` shows DH players and generation speed.
