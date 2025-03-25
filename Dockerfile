FROM azul/zulu-openjdk-alpine:21-latest AS builder

# hadolint ignore=DL3018
RUN apk add --no-cache git wget unzip

WORKDIR /build

COPY gradle gradle

COPY gradlew build.gradle.kts gradle.properties settings.gradle.kts ./

RUN chmod a+x gradlew

RUN ./gradlew build || return 0
RUN rm -rf ./build/libs/*.jar

COPY src src

ARG VERSION=local-docker

RUN ./gradlew build -Pversion=${VERSION}

FROM azul/zulu-openjdk-alpine:21-latest AS runner

WORKDIR /app

# hadolint ignore=DL3018
RUN apk add --update --no-cache libstdc++ msttcorefonts-installer fontconfig curl tzdata && \
    cp /usr/share/zoneinfo/Asia/Tokyo /etc/localtime && \
    echo "Asia/Tokyo" > /etc/timezone && \
    apk del tzdata && \
    curl -O https://moji.or.jp/wp-content/ipafont/IPAexfont/IPAexfont00301.zip && \
    mkdir -p /usr/share/fonts/ipa && \
    unzip -o -d /usr/share/fonts/ipa/ IPAexfont00301.zip "*.ttf" && \
    update-ms-fonts

COPY --from=builder /build/build/libs/vcspeaker-*-all.jar /app

ENV VCSKT_CONFIG=/data/config.yml
ENV VCSKT_STORE=/data/store/
ENV VCSKT_CACHE=/data/cache/
ENV GOOGLE_APPLICATION_CREDENTIALS=/data/google-credential.json
ENV TZ=Asia/Tokyo

RUN FILE_NAME=$(find . -name "vcspeaker-*-all.jar" -print -quit) && \
    mv ${FILE_NAME} vcspeaker.jar

CMD ["java", "-jar", "/app/vcspeaker.jar"]
