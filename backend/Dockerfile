# VELOX API image. Build:  docker build -t velox-api backend
# Two stages: build with the JDK, run on a small JRE as a non-root user.
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /src
COPY backend/mvnw backend/pom.xml backend/spotbugs-exclude.xml ./backend/
COPY backend/.mvn ./backend/.mvn
COPY database ./database
RUN cd backend && chmod +x mvnw && ./mvnw -q -B dependency:go-offline
COPY backend/src ./backend/src
RUN cd backend && ./mvnw -q -B -DskipTests package && cp target/velox-backend-*.jar /src/app.jar

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S velox && adduser -S velox -G velox && mkdir -p /app/uploads && chown velox:velox /app/uploads
WORKDIR /app
COPY --from=build /src/app.jar app.jar
USER velox
ENV PORT=8080 VELOX_API_DOCS=false JAVA_OPTS="-XX:MaxRAMPercentage=75"
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --retries=3 CMD wget -qO- http://localhost:8080/api/zones >/dev/null || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
