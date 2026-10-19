# Single-stage image. Expects the jar to be built first with
# `./mvnw clean package -DskipTests`, which puts it in ./target.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/currency-converter-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
