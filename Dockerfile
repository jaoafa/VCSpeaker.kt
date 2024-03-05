FROM azul/zulu-openjdk-alpine:17-latest as builder

# hadolint ignore=DL3018
RUN apk add --no-cache git wget unzip

WORKDIR /build

COPY gradle gradle
COPY src src
COPY gradlew build.gradle.kts gradle.properties settings.gradle.kts ./

RUN chmod a+x gradlew && \
  ./gradlew build

FROM azul/zulu-openjdk-alpine:17-latest as runner

WORKDIR /app

# hadolint ignore=DL3018
RUN apk add --no-cache libstdc++

COPY --from=builder /build/build/libs/vcspeaker-*.jar /app/vcspeaker-kt.jar

ENV VCSKT_CONFIG /data/config.yml
ENV VCSKT_STORE /data/store/
ENV VCSKT_CACHE /data/cache/
ENV GOOGLE_APPLICATION_CREDENTIALS /data/google-credential.json

CMD ["java", "-jar", "/app/vcspeaker-kt.jar"]
