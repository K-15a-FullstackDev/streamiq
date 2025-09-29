FROM maven:3.9.8-eclipse-temurin-21 as build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/api-gateway-0.1.0.jar app.jar
EXPOSE 9000
ENTRYPOINT ["java","-jar","/app/app.jar"]
