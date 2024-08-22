# Etapa 1: Compilación de la aplicación
FROM maven:3.8.8-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Construcción de la imagen
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/app-antifraud-microservice-kafka-1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]