#!/bin/sh

docker build --build-arg JAR_FILE=target/glacial-discord-bot.jar -t registry.menoni.net/menoni/glacial-discord:dev .
docker push registry.menoni.net/menoni/glacial-discord:dev
