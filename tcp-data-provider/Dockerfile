FROM openjdk:17-jdk-slim

LABEL authors="yusufokr0"

WORKDIR /app

COPY target/tcp-data-provider.jar app.jar

EXPOSE 8090

ENTRYPOINT ["java","-jar","app.jar"]