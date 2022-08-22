#!/bin/bash
docker build -f ./spigot/Dockerfile spigot-server:latest --build-arg MINECRAFT_VERSION=1.19.2 .
docker build -f ./bungee/Dockerfile bungee-server:latest .