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
RUN apk add --no-cache libstdc++ msttcorefonts-installer fontconfig && \
    update-ms-fonts && \
    apk add --update --no-cache tzdata
ENV TZ=Asia/Tokyo
RUN apk del tzdata

COPY --from=builder /build/build/libs/vcspeaker-*.jar /app/vcspeaker-kt.jar

ENV VCSKT_CONFIG /data/config.yml
ENV VCSKT_STORE /data/store/
ENV VCSKT_CACHE /data/cache/
ENV GOOGLE_APPLICATION_CREDENTIALS /data/google-credential.json

CMD ["java", "-jar", "/app/vcspeaker-kt.jar"]
