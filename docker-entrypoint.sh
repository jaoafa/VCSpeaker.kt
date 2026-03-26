#!/usr/bin/env bash

# Run VCSpeaker and handle the update process
#
# Update flow:
# 1. Current (vcspeaker.jar) starts and runs normally
# 2. When update is detected, Current spawns Latest (update.jar) via ProcessBuilder
# 3. Current and Latest communicate via API to transfer state
# 4. Current exits with code 0
# 5. This script waits for Latest to finish (or keeps running)
# 6. Latest 自身がアップデートした場合、スクリプトは新しいプロセスを追跡し続ける（連鎖アップデート対応）

# プロセスがゾンビ状態かどうかを判定する
is_zombie_pid() {
    local pid=$1

    # /proc/<pid>/stat が読めない場合はゾンビではない
    if [[ ! -r "/proc/$pid/stat" ]]; then
        return 1
    fi

    local state
    state=$(awk '{print $3}' "/proc/$pid/stat" 2>/dev/null || true)

    # state が取得できなかった場合もゾンビではない
    if [[ -z "$state" ]]; then
        return 1
    fi

    [[ "$state" == "Z" ]]
}

# アップデートプロセス (update.jar) の PID を取得する
# ゾンビプロセスおよび無効なプロセスは除外する
find_update_pid() {
    local pid
    while IFS= read -r pid; do
        if [[ -z "$pid" ]]; then
            continue
        fi

        # ゾンビプロセスはスキップ
        if is_zombie_pid "$pid"; then
            continue
        fi

        echo "$pid"
        return 0
    done < <(pgrep -f "update\.jar" 2>/dev/null || true)
    return 1
}

java -jar /app/vcspeaker.jar "$@"
exit_code=$?

echo "VCSpeaker exited with code: $exit_code"

if [[ $exit_code -eq 0 ]]; then
    # 連鎖アップデートを追跡するループ
    # update.jar が次の update.jar を起動してから終了する場合にも対応する
    chain_update=0
    while true; do
        # アップデートプロセスを検出するまで待機
        # 初回は最大 5 秒（プロセス起動時間を考慮）、連鎖アップデート時は最大 3 秒
        max_attempts=5
        if [[ $chain_update -eq 1 ]]; then
            max_attempts=3
        fi

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

        # アップデートプロセスが終了するまで待機する
        # コンテナを生かし続けることで Docker による不要な再起動を防ぐ
        while kill -0 "$update_pid" 2>/dev/null; do
            sleep 1
        done

        echo "Update process (PID: $update_pid) finished. Checking for chain updates..."
        chain_update=1
    done

    exit 0
else
    echo "VCSpeaker exited with error."
    exit $exit_code
fi
