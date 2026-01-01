#!/usr/bin/env bash

# Run VCSpeaker and handle the update process
#
# Update flow:
# 1. Current (vcspeaker.jar) starts and runs normally
# 2. When update is detected, Current spawns Latest (update-*.jar)
# 3. Current and Latest communicate via API to transfer state
# 4. Current exits with code 0
# 5. This script waits for Latest to finish (or keeps running)

java -jar /app/vcspeaker.jar "$@"
exit_code=$?

echo "VCSpeaker exited with code: $exit_code"

if [[ $exit_code -eq 0 ]]; then
    find_update_pids() {
        update_pids=()
        local pid

        while IFS= read -r pid; do
            if [[ -z "$pid" ]]; then
                continue
            fi

            if is_zombie_pid "$pid"; then
                continue
            fi

            update_pids+=("$pid")
        done < <(pgrep -f "update-.*\.jar" || true)
    }

    is_zombie_pid() {
        local pid=$1

        if [[ ! -r "/proc/$pid/stat" ]]; then
            return 0
        fi

        local state
        state=$(awk '{print $3}' "/proc/$pid/stat" 2>/dev/null || true)

        [[ "$state" == "Z" ]]
    }

    # Retry loop: wait up to 5 seconds for update process to appear
    update_pids=()
    for _ in {1..5}; do
        find_update_pids
        if [[ ${#update_pids[@]} -gt 0 ]]; then
            break
        fi
        sleep 1
    done

    if [[ ${#update_pids[@]} -gt 0 ]]; then
        echo "Update process detected (PID: ${update_pids[*]}). Waiting for it to complete..."

        # Wait for the update process to finish
        # This keeps the entrypoint alive so Docker doesn't restart the container
        while [[ ${#update_pids[@]} -gt 0 ]]; do
            find_update_pids
            if [[ ${#update_pids[@]} -gt 0 ]]; then
                sleep 1
            fi
        done

        echo "Update process finished."
    else
        echo "No update process found. Normal shutdown."
    fi

    exit 0
else
    echo "VCSpeaker exited with error."
    exit $exit_code
fi
