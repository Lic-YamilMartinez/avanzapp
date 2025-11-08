FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copiamos el descriptor primero para aprovechar la cache de dependencias
COPY pom.xml .

# Copiamos el código fuente
COPY src ./src

# Empaquetamos la aplicación sin tests para acelerar la construcción del contenedor
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/avanzapp-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
