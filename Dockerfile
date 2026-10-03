FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace
COPY . .
RUN ./mvnw --batch-mode clean package

FROM eclipse-temurin:21-jre

RUN useradd --system --create-home --uid 10001 beacon \
    && mkdir -p /app/data \
    && chown -R beacon:beacon /app

WORKDIR /app
COPY --from=build /workspace/target/email-beacon-0.1.0-SNAPSHOT.jar app.jar

USER beacon
ENV BEACON_DB_PATH=/app/data/email-beacon.db
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

