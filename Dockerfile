FROM eclipse-temurin:17-jdk AS builder

ARG MODULE_NAME
WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY eureka-server/pom.xml eureka-server/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY hotel-service/pom.xml hotel-service/pom.xml
COPY booking-service/pom.xml booking-service/pom.xml

RUN chmod +x mvnw
RUN ./mvnw -q -pl ${MODULE_NAME} -am dependency:go-offline

COPY . .
RUN ./mvnw -q -pl ${MODULE_NAME} -am package -DskipTests

FROM eclipse-temurin:17-jre

ARG MODULE_NAME
WORKDIR /app

COPY --from=builder /workspace/${MODULE_NAME}/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
