FROM eclipse-temurin:17.0.1_12-jre-focal

ARG PROXY_PORT=25565

WORKDIR /app/bungeecord

ADD https://ci.md-5.net/job/BungeeCord/lastSuccessfulBuild/artifact/bootstrap/target/BungeeCord.jar /app/bungeecord

COPY ./.dev/bungee/config.yml .

ENV PORT ${PROXY_PORT}
CMD [ "bash", "-c", "java -Xms512M -Xmx512M -jar BungeeCord.jar" ]
EXPOSE ${PORT}