FROM openjdk:11.0-jre-slim
COPY ./build/install/seedbox-sync /app
WORKDIR /app

ENTRYPOINT [ "/app/bin/seedbox-sync" ]
CMD [ "scheduler", "-c", "/config/config.json"]