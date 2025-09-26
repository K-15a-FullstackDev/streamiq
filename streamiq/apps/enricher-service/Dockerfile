FROM maven:3.9.8-eclipse-temurin-21 as build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/enricher-service-0.1.0.jar app.jar
EXPOSE 9001
ENTRYPOINT ["java","-jar","/app/app.jar"]
