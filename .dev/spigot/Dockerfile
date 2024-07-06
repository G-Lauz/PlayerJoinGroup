FROM eclipse-temurin:22.0.1_8-jdk-jammy

ARG MINECRAFT_VERSION=latest
ARG SERVER_MEMORY=1
ARG SERVER_PORT=25565
ARG DEBUGGER_PORT=5005

WORKDIR /app/build

RUN apt-get update && apt-get install -y git

# Build the Spigot server
ADD https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar /app/build
RUN java -jar BuildTools.jar --rev ${MINECRAFT_VERSION} --output-dir /app/server

# Run the Spigot server
WORKDIR /app/server

COPY .dev/spigot/*.yml .
COPY .dev/spigot/server.properties .
RUN mkdir ./plugins && \
    echo "eula=true" > ./eula.txt && \
    echo "java -Xms${SERVER_MEMORY}G \
               -Xmx${SERVER_MEMORY}G \
               -XX:+UseG1GC \
               -DIReallyKnowWhatIAmDoingISwear \
               -agentlib:jdwp=transport=dt_socket,address=*:${DEBUGGER_PORT},server=y,suspend=n  \
               -jar spigot-${MINECRAFT_VERSION}.jar nogui" > ./start.sh && \
    chmod +x ./start.sh

CMD [ "bash", "-c", "./start.sh" ]

EXPOSE ${SERVER_PORT}
