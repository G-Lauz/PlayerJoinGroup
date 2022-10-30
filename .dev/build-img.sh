#!/bin/bash
docker build -f ./spigot/Dockerfile -t spigot-server:1.19.2 --build-arg MINECRAFT_VERSION=1.19.2 .
docker build -f ./bungee/Dockerfile -t bungee-server:latest .