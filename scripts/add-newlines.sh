#!/usr/bin/env bash

# .kt ファイルの末尾に改行がない場合追加
# プロジェクトのルートディレクトリで
# $ ./scripts/add-newlines.sh

find . | grep -E ".+\.kt$" | while read -r path; do
  if [[ -n $(tail -c 1 "$path") ]]; then
    echo "$path"
    echo >> "$path"
  fi
done
