# 1) React derlemesi
FROM node:20-alpine AS web
WORKDIR /web
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/ ./
RUN npm run build

# 2) Spring Boot derlemesi (React çıktısı static/ içine gömülür: tek konteyner, tek adres, CORS yok)
FROM maven:3.9-eclipse-temurin-17 AS api
WORKDIR /api
COPY backend/pom.xml ./
RUN mvn -q -B dependency:go-offline
COPY backend/src ./src
COPY --from=web /web/dist ./src/main/resources/static
RUN mvn -q -B -DskipTests package

# 3) Çalışma ortamı (küçük bellekli ücretsiz katmanlara göre ayarlı)
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd -r -u 1001 app && chown app /app
COPY --from=api /api/target/*.jar app.jar
USER app
ENV JAVA_TOOL_OPTIONS="-Xmx300m -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:MaxMetaspaceSize=128m -Xss512k -XX:+ExitOnOutOfMemoryError -Xlog:gc:stdout:time"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
