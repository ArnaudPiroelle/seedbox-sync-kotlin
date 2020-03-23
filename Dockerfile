FROM openjdk:11

WORKDIR /app
COPY . /app
RUN ./gradlew installDist
CMD [ "/app/build/install/seedbox-sync/bin/seedbox-sync", "sync", "-c", "/config/config.json" ]