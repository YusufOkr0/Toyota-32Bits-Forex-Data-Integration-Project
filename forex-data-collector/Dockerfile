FROM ghcr.io/graalvm/jdk-community:21.0.2

LABEL authors="yusufokr0"

WORKDIR /app

COPY target/lib ./lib
COPY target/Forex-data-collector.jar ./app.jar

ENTRYPOINT ["java","-cp","app.jar:lib/*","com.toyota.ForexDataCollector"]