# title

ボイスチャンネルのチャンネル名を一時的に変更します。

## 使い方

タイトルを設定します。  
指定したチャンネル、またはユーザーが現在参加しているボイスチャンネルのタイトルを変更します。  
チャンネルから全員が退出すると、タイトルはリセットされます。

```text
/title <title> [channel]
```

- `<title>`: 設定するタイトル
- `[channel]`: タイトルを設定するチャンネル。指定しない場合は、コマンドを実行したユーザーが現在参加しているボイスチャンネルが対象になります。

## 注意事項

- チャンネル名の変更にはレートリミットが適用されるため、名前が反映されるまでに時間がかかる場合があります。
- チャンネルから全員が退出すると、設定したタイトルはリセットされます。

## 関連するページ

- [Title 機能について](../features/title.md)
