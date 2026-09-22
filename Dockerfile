FROM node:20-alpine AS styles
WORKDIR /workspace
COPY package.json package-lock.json* tailwind.config.js ./
RUN npm ci
COPY src/main/frontend ./src/main/frontend
COPY src/main/resources/templates ./src/main/resources/templates
RUN npm run build:css

FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -DskipTests dependency:go-offline
COPY src src
COPY --from=styles /workspace/src/main/resources/static/css/app.css src/main/resources/static/css/app.css
RUN ./mvnw -B clean package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S library && adduser -S library -G library
COPY --from=build /workspace/target/library-management-system-*.jar app.jar
USER library
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
