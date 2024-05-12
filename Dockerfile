FROM azul/zulu-openjdk-alpine:17-latest as builder

# hadolint ignore=DL3018
RUN apk add --no-cache git wget unzip

WORKDIR /build

COPY gradle gradle

COPY gradlew build.gradle.kts gradle.properties settings.gradle.kts ./

RUN chmod a+x gradlew
RUN ./gradlew build || return 0

COPY src src

RUN ./gradlew build

FROM azul/zulu-openjdk-alpine:17-latest as runner

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

COPY --from=builder /build/build/libs/vcspeaker-*.jar /app/vcspeaker-kt.jar

ENV VCSKT_CONFIG /data/config.yml
ENV VCSKT_STORE /data/store/
ENV VCSKT_CACHE /data/cache/
ENV GOOGLE_APPLICATION_CREDENTIALS /data/google-credential.json
ENV TZ Asia/Tokyo

CMD ["java", "-jar", "/app/vcspeaker-kt.jar"]
