FROM eclipse-temurin:17.0.1_12-jre-focal

ARG MINECRAFT_VERSION=latest
ARG SERVER_MEMORY=1
ARG SERVER_PORT=25565

WORKDIR /app/build

RUN apt-get update && apt-get install -y \ 
    git \
    wget

# Build the Spigot server
RUN wget -O BuildTools.jar https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar
RUN java -jar BuildTools.jar --rev ${MINECRAFT_VERSION} --output-dir /app/server

# Run the Spigot server
WORKDIR /app/server

ENV MEMORY ${SERVER_MEMORY}
ENV VERSION ${MINECRAFT_VERSION}
RUN mkdir ./plugins && \
    echo "eula=true" > ./eula.txt
CMD [ "bash", "-c", "java -Xms${MEMORY}G -Xmx${MEMORY}G -XX:+UseG1GC -jar spigot-${VERSION}.jar nogui" ]

EXPOSE ${SERVER_PORT}