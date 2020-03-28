FROM gradle:jdk11 as intermediate
COPY . /app
WORKDIR /app
RUN gradle clean installDist

FROM openjdk:11.0-jre-slim

COPY --from=intermediate /app/build/install/seedbox-sync /app
WORKDIR /app

ENTRYPOINT [ "/app/bin/seedbox-sync" ]
CMD [ "scheduler", "-c", "/config/config.json"]