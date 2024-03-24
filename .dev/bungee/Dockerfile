FROM eclipse-temurin:17.0.1_12-jre-focal

ARG PROXY_PORT=25577
ARG DEBUGGER_PORT=5005

WORKDIR /app/server

ADD https://ci.md-5.net/job/BungeeCord/lastSuccessfulBuild/artifact/bootstrap/target/BungeeCord.jar /app/server

COPY ./.dev/bungee/config.yml .

RUN echo "java -Xms512M \
               -Xmx512M \
               -agentlib:jdwp=transport=dt_socket,address=*:${DEBUGGER_PORT},server=y,suspend=n  \
               -jar BungeeCord.jar" > ./start.sh && \
    chmod +x ./start.sh

CMD [ "bash", "-c", "./start.sh" ]

EXPOSE ${PROXY_PORT}
