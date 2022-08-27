FROM eclipse-temurin:17.0.1_12-jre-focal

ARG PROXY_PORT=25577

WORKDIR /app/server

ADD https://ci.md-5.net/job/BungeeCord/lastSuccessfulBuild/artifact/bootstrap/target/BungeeCord.jar /app/server

COPY ./.dev/bungee/config.yml .

CMD [ "bash", "-c", "java -Xms512M -Xmx512M -jar BungeeCord.jar" ]

EXPOSE ${PROXY_PORT}
