#!/usr/bin/env bash

java -jar /app/vcspeaker.jar

function process_check() {
    pid=$1

    if kill -0 "$pid" 2> /dev/null; then
        return 0 # process is running
    else
        return 1 # is not
    fi
}

if [[ "$!" -ne 0 ]]; then
    echo "VCSpeaker exited due to an error."
    exit 1
else
    echo "Reload triggered."
    while :; do
      pid=$(pgrep -n "update-[0-9]+\.jar")
      if [[ -e $(process_check "$pid") ]]; then
        exit 0
      fi
    done
fi