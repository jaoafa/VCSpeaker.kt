#!/usr/bin/env bash

find . | grep -E ".+\.kt$" | while read -r path; do
  if [[ -n $(tail -c 1 "$path") ]]; then
    echo "$path"
    echo >> "$path"
  fi
done
