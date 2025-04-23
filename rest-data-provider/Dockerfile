FROM openjdk:17-jdk-slim

LABEL authors="yusufokr0"

WORKDIR /app

COPY target/restdataprovider-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8092

ENTRYPOINT ["java", "-jar","app.jar"]