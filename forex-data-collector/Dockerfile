FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .

COPY src ./src

RUN mvn clean package -DskipTests


FROM ghcr.io/graalvm/jdk-community:21

LABEL authors="yusufokr0"

WORKDIR /app

COPY --from=builder /app/target/Forex-data-collector.jar ./app.jar
COPY --from=builder /app/target/lib ./lib

ENTRYPOINT ["java", "-cp", "/app/app.jar:/app/lib/*", "com.toyota.ForexDataCollector"]

