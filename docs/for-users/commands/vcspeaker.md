# vcspeaker

VCSpeaker を操作するためのコマンドです。  
このコマンドを使用して、テキストチャンネルの読み上げ設定や、Bot の再起動などを行うことができます。

## 使い方

### restart

VCSpeaker を再起動します。

```text
/vcspeaker restart
```

### settings

VCSpeaker の設定を行います。この設定は、サーバごとに保存されます。

```text
/vcspeaker settings [channel] [prefix] [speaker] [emotion] [emotion-level] [pitch] [speed] [volume] [auto-join]
```

- `[channel]`: 読み上げるテキストチャンネル
- `[prefix]`: チャットコマンドのプレフィックス
- `[speaker]`: 話者
- `[emotion]`: 感情
- `[emotion-level]`: 感情レベル (1 から 4)
- `[pitch]`: ピッチ (50% から 200%)
- `[speed]`: 速度 (50% から 200%)
- `[volume]`: 音量 (50% から 200%)
- `[auto-join]`:ボイスチャンネルに自動で入退室するかどうか

## 注意事項

- `restart` コマンドを使用すると、Bot は即座に再起動されます。これにより、一時的に Bot が使用不可能になることがあります。
- `settings` コマンドで設定を変更する際、指定しなかったオプションは現在の設定を維持します。全ての設定を一度に変更する必要はありません。
