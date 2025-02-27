#!/usr/bin/env bash

# List all processors in the order of priority

PROCESSORS_DIR="./src/main/kotlin/com/jaoafa/vcspeaker/tts/processors"

find "$PROCESSORS_DIR" -type f ! -name 'BaseProcessor.kt' | while read -r file; do
  priority=$(awk -F'=' '/override val priority =/ {print $2}' "$file")
  echo "$priority $(basename "$file")"
done | sort -n | while read -r priority file; do
  echo "$priority: $file"
done