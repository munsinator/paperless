FROM eclipse-temurin:25-jdk-ubi10-minimal

# paperlessREST
WORKDIR /api

# copies src code from target to /paperlessREST
COPY . .

EXPOSE 81

CMD ["java", "-jar", "/api-0.0.1-SNAPSHOT.jar"]