FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

COPY src ./src
COPY pom.xml .

RUN mvn clean package -DskipTests


FROM openjdk:17-jdk-slim

LABEL authors="yusufokr0"

WORKDIR /app

COPY --from=builder /app/target/tcp-data-provider.jar app.jar

CMD ["java", "-jar", "app.jar"]