FROM eclipse-temurin:21-jdk-jammy
WORKDIR /app
COPY target/zap-0.0.1-SNAPSHOT.jar zap.jar
ENTRYPOINT ["java", "-jar", "zap.jar"]