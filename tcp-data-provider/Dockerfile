FROM openjdk:17-jdk-slim

LABEL authors="yusufokr0"

WORKDIR /app

COPY target/tcp-data-provider.jar /app/tcp-data-provider.jar

EXPOSE 8090

ENTRYPOINT ["java","-jar","tcp-data-provider.jar"]