FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

COPY src ./src
COPY pom.xml .

RUN mvn clean package -DskipTests


FROM openjdk:17-jdk-slim

LABEL authors="yusufokr0"

WORKDIR /app

COPY --from=builder /app/target/rest-data-provider-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8092

CMD ["java", "-jar", "app.jar"]





