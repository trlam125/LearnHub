#!/bin/sh
# LearnHub auto-updater for Wispbyte. Run ONLY as the server Startup Command.
# Checks the newest public GitHub Release every 15 minutes by default.
set -u

# Wispbyte does not load .env files automatically. Export configuration
# from a trusted, server-local .env before starting the Java process.
# Values containing shell special characters must be shell-quoted.
if [ -f ./.env ]; then
    set -a
    . ./.env
    set +a
fi

RELEASE_BASE="https://github.com/trlam125/LearnHub/releases/latest/download"
CHECK_INTERVAL="${CHECK_INTERVAL:-900}"
STARTUP_WAIT="${STARTUP_WAIT:-75}"
JAVA_XMX_MB="${JAVA_XMX_MB:-256}"
SERVICE_PORT="${SERVER_PORT:-${PORT:-11452}}"
JAR="main.jar"
NEW_JAR=".main.jar.new"
OLD_JAR=".main.jar.previous"
SHA_FILE=".main.jar.sha256.remote"
REJECTED_SHA_FILE=".deploy.rejected.sha256"
APP_PID=""
SLEEP_PID=""

log() { printf '[LearnHub deploy] %s\n' "$*"; }

case "$CHECK_INTERVAL" in ''|*[!0-9]*) log 'CHECK_INTERVAL must be an integer (seconds)'; exit 1 ;; esac
[ "$CHECK_INTERVAL" -ge 60 ] || { log 'Minimum CHECK_INTERVAL is 60 seconds'; exit 1; }
case "$SERVICE_PORT" in ''|*[!0-9]*) log 'Invalid port'; exit 1 ;; esac
case "$JAVA_XMX_MB" in ''|*[!0-9]*) log 'Invalid JAVA_XMX_MB'; exit 1 ;; esac
case "$STARTUP_WAIT" in ''|*[!0-9]*) log 'Invalid STARTUP_WAIT'; exit 1 ;; esac
command -v java >/dev/null 2>&1 || { log 'Java is unavailable'; exit 1; }
command -v sha256sum >/dev/null 2>&1 || { log 'sha256sum is unavailable'; exit 1; }
if command -v curl >/dev/null 2>&1; then
    FETCH_TOOL=curl
elif command -v wget >/dev/null 2>&1; then
    FETCH_TOOL=wget
else
    log 'Neither curl nor wget is installed. Cannot fetch GitHub Releases.'
    exit 1
fi

fetch() {
    url="$1"
    file="$2"
    if [ "$FETCH_TOOL" = curl ]; then
        curl -fLsS --connect-timeout 10 --max-time 150 --retry 2 --retry-delay 2 -o "$file" "$url"
    else
        wget -q --timeout=30 --tries=3 -O "$file" "$url"
    fi
}

running() {
    [ -n "$APP_PID" ] && kill -0 "$APP_PID" 2>/dev/null
}

stop_app() {
    if [ -n "$APP_PID" ]; then
        log "Stopping Java PID $APP_PID"
        kill -TERM "$APP_PID" 2>/dev/null || true
        i=0
        while running && [ "$i" -lt 15 ]; do
            sleep 1
            i=$((i + 1))
        done
        if running; then
            log 'Java did not stop gracefully; killing it'
            kill -KILL "$APP_PID" 2>/dev/null || true
        fi
        wait "$APP_PID" 2>/dev/null || true
        APP_PID=""
    fi
}

cleanup() {
    [ -z "$SLEEP_PID" ] || kill "$SLEEP_PID" 2>/dev/null || true
    stop_app
    rm -f "$NEW_JAR" "$SHA_FILE"
}
trap 'exit 143' HUP INT TERM
trap 'cleanup' EXIT

start_app() {
    log "Starting $JAR on port $SERVICE_PORT (heap max ${JAVA_XMX_MB}m)"
    java -Xms64m "-Xmx${JAVA_XMX_MB}m" -Djava.awt.headless=true \
        -jar "$JAR" --server.address=0.0.0.0 --server.port="$SERVICE_PORT" &
    APP_PID=$!
}

probe_health() {
    if [ "$FETCH_TOOL" = curl ]; then
        curl -fsS --max-time 4 "http://127.0.0.1:${SERVICE_PORT}/health" 2>/dev/null \
            | grep -q '"status"[[:space:]]*:[[:space:]]*"ok"'
    else
        wget -q -T 4 -O - "http://127.0.0.1:${SERVICE_PORT}/health" 2>/dev/null \
            | grep -q '"status"[[:space:]]*:[[:space:]]*"ok"'
    fi
}

wait_for_start() {
    elapsed=0
    while [ "$elapsed" -lt "$STARTUP_WAIT" ]; do
        running || return 1
        if probe_health; then
            return 0
        fi
        sleep 2 &
        SLEEP_PID=$!
        wait "$SLEEP_PID" || true
        SLEEP_PID=""
        elapsed=$((elapsed + 2))
    done
    log 'The /health endpoint did not become ready in time'
    return 1
}

# The newest release publishes main.jar.sha256 and main.jar.
update_if_needed() {
    rm -f "$NEW_JAR" "$SHA_FILE"
    if ! fetch "$RELEASE_BASE/main.jar.sha256" "$SHA_FILE"; then
        log 'Unable to check GitHub Release; keeping installed JAR'
        return 1
    fi
    expected=$(awk 'NR == 1 {print $1}' "$SHA_FILE")
    case "$expected" in
        ''|*[!0123456789abcdefABCDEF]*) log 'Invalid remote SHA-256'; return 1 ;;
    esac
    if [ "${#expected}" -ne 64 ]; then
        log 'Invalid remote SHA-256 length'
        return 1
    fi

    if [ -f "$REJECTED_SHA_FILE" ] && [ "$(cat "$REJECTED_SHA_FILE")" = "$expected" ]; then
        log 'Skipping release previously rejected due to startup failure'
        return 1
    fi

    if [ -f "$JAR" ]; then
        current=$(sha256sum "$JAR" | awk '{print $1}')
        if [ "$current" = "$expected" ]; then
            return 0
        fi
    fi

    log 'New release detected, downloading main.jar'
    if ! fetch "$RELEASE_BASE/main.jar" "$NEW_JAR"; then
        log 'Download failed; keeping currently installed JAR'
        return 1
    fi
    actual=$(sha256sum "$NEW_JAR" | awk '{print $1}')
    if [ "$actual" != "$expected" ]; then
        log 'SHA-256 mismatch. Update rejected.'
        return 1
    fi

    if [ -f "$JAR" ]; then
        rm -f "$OLD_JAR"
        if running; then stop_app; fi
        if ! mv "$JAR" "$OLD_JAR"; then
            log 'Cannot back up the current JAR'
            start_app
            return 1
        fi
    fi

    if ! mv "$NEW_JAR" "$JAR"; then
        log 'Cannot install new JAR'
        if [ -f "$OLD_JAR" ]; then
            mv "$OLD_JAR" "$JAR" || true
            [ -f "$JAR" ] && start_app
        fi
        return 1
    fi

    start_app
    if wait_for_start; then
        log 'Update installed; /health is responding successfully'
        rm -f "$OLD_JAR" "$REJECTED_SHA_FILE"
        return 0
    fi

    log 'New JAR exited during startup; rolling back'
    printf '%s\n' "$expected" > "$REJECTED_SHA_FILE"
    stop_app
    if [ -f "$OLD_JAR" ]; then
        mv "$OLD_JAR" "$JAR" || return 1
        start_app
        if wait_for_start; then
            log 'Rollback running'
            return 1
        fi
    fi
    log 'No healthy Java process after rollback'
    return 1
}

log "Updater started (interval=${CHECK_INTERVAL}s)"
update_if_needed || true
if ! running; then
    if [ ! -f "$JAR" ]; then
        log 'No main.jar available. Publish a GitHub Release first.'
        exit 1
    fi
    start_app
    if ! wait_for_start; then
        log 'Application exited on startup. Check database and environment variables.'
        exit 1
    fi
fi

# Periodically check without spawning another Java process.
while running; do
    remaining="$CHECK_INTERVAL"
    while [ "$remaining" -gt 0 ]; do
        step=30
        [ "$remaining" -lt "$step" ] && step="$remaining"
        sleep "$step" &
        SLEEP_PID=$!
        wait "$SLEEP_PID" || true
        SLEEP_PID=""
        running || { log 'Java stopped unexpectedly'; exit 1; }
        remaining=$((remaining - step))
    done
    update_if_needed || true
    running || { log 'Java stopped after update'; exit 1; }
done

log 'Java stopped unexpectedly'
exit 1
