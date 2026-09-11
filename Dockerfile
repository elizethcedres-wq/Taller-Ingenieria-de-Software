# Etapa 1: Compilación
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# Etapa 2: Ejecución
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /app/target/*-jar-with-dependencies.jar app.jar

ENV MAIN_CLASS=com.mycompany.taller2.ingsoft.Taller2IngSoft

ENTRYPOINT ["sh", "-c", "java -cp app.jar $MAIN_CLASS"]
