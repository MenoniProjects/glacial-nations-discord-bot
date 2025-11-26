#!/bin/sh

docker build --build-arg JAR_FILE=target/glacial-nations-discord-bot.jar -t ghcr.io/menoniprojects/glacial-nations-discord-bot:dev .
docker push ghcr.io/menoniprojects/glacial-nations-discord-bot:dev
