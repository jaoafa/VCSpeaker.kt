#!/usr/bin/env bash

# 日本語と半角 Latin 文字が混在しており、その間に半角スペースが無い行を検出する (.kt ファイルのみ)
# このようなlineがdetectされる
#
# 警告の無視設定 :
# // whitespace ignore ... この行を無視する
# // whitespace ignore-nextline ... 次の行を無視する
# // whitespace ignore-file ... このファイルを無視する
# // whitespace ignore-begin ... この行から一括で無視する
# // whitespace ignore-end ... ひとつ上の行で無視範囲を終わる

# 区切り文字を除くひらがな・カタカナ・漢字
c_jp='(?!\p{P})[\p{Hiragana}\p{Katakana}\p{Han}]'
# c_jp + 半角ラテン文字、もしくは半角ラテン文字 + c_jp (の間に半角スペースが無い)
regex='^.*(?:'"$c_jp"'\p{Latin}+|\p{Latin}+'"$c_jp"').*$'

ignore_thisline_flag='ignore'
ignore_nextline_flag='ignore-nextline'
ignore_file_flag='ignore-file'
ignore_begin_flag='ignore-begin'
ignore_end_flag='ignore-end'

function flag_comment_regex() {
  echo '^.*\/\/\swhitespace\s'"$1"'\s*$'
}

is_error_found=0

while IFS= read -r path; do
  result=$(perl -C -Mutf8 -ne '
    $ignore_file = 1 if /'"$(flag_comment_regex "$ignore_file_flag")"'/;

    next if /'"$(flag_comment_regex "$ignore_thisline_flag")"'/;

    if ($ignore_nextline) {
      $ignore_nextline = 0;
      next;
    }
    $ignore_nextline = 1 if /'"$(flag_comment_regex "$ignore_nextline_flag")"'/;

    $ignore_range = 1 if /'"$(flag_comment_regex "$ignore_begin_flag")"'/;
    $ignore_range = 0 if /'"$(flag_comment_regex "$ignore_end_flag")"'/;
    next if $ignore_range;

    print "$.:$_" if /'"$regex"'/;
    END {
      exit($ignore_file ? 1 : 0)
    }
  ' "$path")
  ignore_file=$?
  if [[ -n $result && $ignore_file -eq 0 ]]; then
    is_error_found=1
    echo "${path#./}"
    echo "$result"
    echo ""
  fi
done < <(find . | grep -E ".+\.kt$")

if [[ $is_error_found -eq 0 ]]; then
  echo "No violations found."
  exit 0
else
  exit 1
fi
