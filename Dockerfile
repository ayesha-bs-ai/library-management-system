FROM node:20-alpine AS styles
WORKDIR /workspace
COPY package.json package-lock.json* tailwind.config.js ./
RUN npm ci
COPY src/main/frontend ./src/main/frontend
COPY src/main/resources/templates ./src/main/resources/templates
RUN npm run build:css && ls -lh src/main/resources/static/css/

FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -DskipTests dependency:go-offline
COPY src src
COPY --from=styles /workspace/src/main/resources/static/css/app.css src/main/resources/static/css/app.css
RUN ls -lh src/main/resources/static/css/ && ./mvnw -B -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache curl
RUN addgroup -S library && adduser -S library -G library
RUN mkdir -p /app/data /app/uploads/covers && chown -R library:library /app
COPY --from=build /workspace/target/library-management-system-*.jar app.jar
USER library
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
ENV APP_UPLOAD_DIR=/app/uploads
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC"
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:${PORT:-8080}/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar /app/app.jar"]
