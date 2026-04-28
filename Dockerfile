FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

ENV PORT=10000

COPY --from=build /app/target/*.jar app.jar

EXPOSE 10000

CMD ["sh", "-c", "java -jar /app/app.jar"]
