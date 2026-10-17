FROM eclipse-temurin:25-jdk-ubi10-minimal
WORKDIR /app

# Build locally with ./mvnw.cmd clean package -DskipTests before building the image.
COPY target/api-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
