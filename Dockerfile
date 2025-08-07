# Stage 1: Build React
FROM node:18-alpine AS react-build
WORKDIR /app

COPY reactviewer/package*.json ./
RUN npm install

COPY reactviewer/ ./
RUN npm run build

# Stage 2: Build Spring Boot with React files embedded
FROM openjdk:21-jdk-slim AS spring-build
WORKDIR /app

# Copy Spring Boot source
COPY stone/ ./

# copy React build files
COPY --from=react-build /app/dist/ src/main/resources/static/

# Make mvnw executable and build
RUN chmod +x ./mvnw && ./mvnw clean package -DskipTests

# Stage 3: Runtime - just the JAR
FROM openjdk:21-jdk-slim
WORKDIR /app

# Copy the built JAR file (contains static files)
COPY --from=spring-build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

