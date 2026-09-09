#!/usr/bin/env bash

# 区切り文字を除くひらがな・カタカナ・漢字
c_jp='(?!\p{P})[\p{Hiragana}\p{Katakana}\p{Han}]'
# c_jp + 半角ラテン文字、もしくは半角ラテン文字 + c_jp (の間に半角スペースが無い)
regex='^.*(?:'"$c_jp"'\p{Latin}+|\p{Latin}+'"$c_jp"').*$'

find . | grep -E ".+\.kt$" | while read -r path; do
  result=$(perl -C -Mutf8 -ne 'print "$.:$_" if /'"$regex"'/' "$path")
  if [[ -n $result ]]; then
    echo "$path"
    echo "$result"
  fi
done
