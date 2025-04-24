FROM openjdk:17-jdk-slim

LABEL authors="yusufokr0"

WORKDIR /app

COPY /target/kafka-db-consumer-0.0.1-SNAPSHOT.jar app.jar

CMD ["java", "-jar","app.jar"]