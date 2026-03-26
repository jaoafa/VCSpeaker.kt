#!/usr/bin/env bash

# Run VCSpeaker and handle the update process
#
# Update flow:
# 1. Current (vcspeaker.jar) starts and runs normally
# 2. When update is detected, Current spawns Latest (update-<version>.jar) via ProcessBuilder
# 3. Current and Latest communicate via API to transfer state
# 4. Current exits with code 0
# 5. This script waits for Latest to finish (or keeps running)
# 6. If Latest itself updates, the script tracks the new process (chain updates)

# Returns 0 if the given PID is a zombie process, 1 otherwise
is_zombie_pid() {
    local pid=$1

    # If /proc/<pid>/stat is not readable, the process is not a zombie
    if [[ ! -r "/proc/$pid/stat" ]]; then
        return 1
    fi

    local state
    state=$(awk '{print $3}' "/proc/$pid/stat" 2>/dev/null || true)

    # If state cannot be read, treat as not a zombie
    if [[ -z "$state" ]]; then
        return 1
    fi

    [[ "$state" == "Z" ]]
}

# Returns the PID of the update process (update-<version>.jar), excluding zombies and invalid processes
find_update_pid() {
    local pid
    while IFS= read -r pid; do
        if [[ -z "$pid" ]]; then
            continue
        fi

        # Skip zombie processes
        if is_zombie_pid "$pid"; then
            continue
        fi

        echo "$pid"
        return 0
    done < <(pgrep -f "update-.*\.jar" 2>/dev/null || true)
    return 1
}

java -jar /app/vcspeaker.jar "$@"
exit_code=$?

echo "VCSpeaker exited with code: $exit_code"

if [[ $exit_code -eq 0 ]]; then
    # Loop to track chain updates:
    # handles the case where update-<v1>.jar spawns update-<v2>.jar before exiting
    chain_update=0
    while true; do
        # Wait up to 5 seconds for an update process to appear (accounts for JVM startup time)
        max_attempts=5

        update_pid=""
        for _ in $(seq 1 "$max_attempts"); do
            update_pid=$(find_update_pid || true)
            if [[ -n "$update_pid" ]]; then
                break
            fi
            sleep 1
        done

        if [[ -z "$update_pid" ]]; then
            if [[ $chain_update -eq 1 ]]; then
                echo "No further update process found. Normal shutdown."
            else
                echo "No update process found. Normal shutdown."
            fi
            break
        fi

        echo "Update process detected (PID: $update_pid). Waiting for it to complete..."

        # Wait for the update process to finish
        # This keeps the entrypoint alive so Docker doesn't restart the container
        # Try wait first to properly reap child processes
        if ! wait "$update_pid" 2>/dev/null; then
            # Fall back to polling if wait fails (e.g. process is not a child)
            while kill -0 "$update_pid" 2>/dev/null; do
                # Break out of the loop if the process has become a zombie
                if is_zombie_pid "$update_pid"; then
                    wait "$update_pid" 2>/dev/null || true
                    break
                fi
                sleep 1
            done
        fi

        echo "Update process (PID: $update_pid) finished. Checking for chain updates..."
        chain_update=1
    done

    exit 0
else
    echo "VCSpeaker exited with error."
    exit $exit_code
fi
