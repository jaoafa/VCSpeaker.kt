#!/usr/bin/env bash

# reload 機能のテスト用に jar を build して ./updates/test.jar に移動する
# プロジェクトのルートディレクトリで
# $ ./scripts/build-reload-test-jar.sh

./gradlew build -Pversion=test
mv ./build/libs/vcspeaker-test-all.jar ./updates/test.jar
