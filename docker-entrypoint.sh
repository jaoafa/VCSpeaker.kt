#!/usr/bin/env bash

# Run VCSpeaker and handle the update process
#
# Update flow:
# 1. Current (vcspeaker.jar) starts and runs normally
# 2. When update is detected, Current spawns Latest (update-*.jar) via nohup
# 3. Current and Latest communicate via API to transfer state
# 4. Current exits with code 0
# 5. This script waits for Latest to finish (or keeps running)

java -jar /app/vcspeaker.jar "$@"
exit_code=$?

echo "VCSpeaker exited with code: $exit_code"

if [[ $exit_code -eq 0 ]]; then
    update_pid=$(pgrep -f "update-[0-9]+\.jar" || true)

    if [[ -n "$update_pid" ]]; then
        echo "Update process detected (PID: $update_pid). Waiting for it to complete..."

        # Wait for the update process to finish
        # This keeps the entrypoint alive so Docker doesn't restart the container
        while kill -0 "$update_pid" 2>/dev/null; do
            sleep 5
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