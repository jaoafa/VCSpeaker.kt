FROM azul/zulu-openjdk-alpine:17-latest

ENV APP_GIT_LINK https://github.com/jaoafa/VCSpeaker.kt

WORKDIR /app

RUN wget ${APP_GIT_LINK}/releases/latest/download/vcspeaker-kt.jar

ENV VCSKT_CONFIG /data/config.yml
ENV VCSKT_STORE /data/store/
ENV VCSKT_CACHE /data/cache/

CMD ["java", "-jar", "/app/vcspeaker-kt.jar"]
