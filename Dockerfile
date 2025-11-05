####
# Dockerfile multi-stage para Quarkus
# Optimizado para producción con JVM mode
####

## Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copiar archivos de dependencias primero (cache layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar código fuente
COPY src ./src

# Build de la aplicación (sin tests para builds más rápidos)
RUN mvn package -DskipTests -Dquarkus.package.type=fast-jar

## Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Instalar curl para health checks
RUN apk add --no-cache curl

# Crear usuario no-root
RUN addgroup -S quarkus && adduser -S quarkus -G quarkus
USER quarkus

# Copiar el JAR y dependencias desde el stage de build
COPY --from=build --chown=quarkus:quarkus /app/target/quarkus-app/lib/ ./lib/
COPY --from=build --chown=quarkus:quarkus /app/target/quarkus-app/*.jar ./
COPY --from=build --chown=quarkus:quarkus /app/target/quarkus-app/app/ ./app/
COPY --from=build --chown=quarkus:quarkus /app/target/quarkus-app/quarkus/ ./quarkus/

# Exponer puerto
EXPOSE 8089

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8089/q/health || exit 1

# Configuración JVM optimizada
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -Dquarkus.http.host=0.0.0.0"

# Comando de inicio
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar quarkus-run.jar"]
