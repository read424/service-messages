####
# Dockerfile multi-stage para Quarkus con mejores prácticas
# Optimizado para producción con JVM mode
#
# Build: docker build -t service-messages:latest .
# Run:   docker run -p 8089:8089 service-messages:latest
####

# =============================================================================
# Stage 1: Build
# =============================================================================
FROM maven:3.9-eclipse-temurin-21 AS build

# Build arguments para GitHub Packages (avro-schemas)
ARG GITHUB_USERNAME
ARG GITHUB_TOKEN

WORKDIR /app

# Configurar Maven settings para GitHub Packages
RUN mkdir -p /root/.m2 && \
    echo '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" \
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" \
      xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 \
      https://maven.apache.org/xsd/settings-1.0.0.xsd"> \
      <servers> \
        <server> \
          <id>github</id> \
          <username>'${GITHUB_USERNAME}'</username> \
          <password>'${GITHUB_TOKEN}'</password> \
        </server> \
      </servers> \
    </settings>' > /root/.m2/settings.xml

# Cache de dependencias Maven (solo se invalida si cambia pom.xml)
COPY pom.xml .

# Descargar dependencias offline para mejor cache
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn dependency:go-offline -B -q

# Copiar código fuente
COPY src ./src

# Build de la aplicación
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn package -DskipTests -Dquarkus.package.jar.type=fast-jar -B -q

# =============================================================================
# Stage 2: Runtime
# =============================================================================
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:1.21

# Labels OCI estándar
LABEL org.opencontainers.image.title="service-messages" \
      org.opencontainers.image.description="Microservicio de mensajes - Quarkus" \
      org.opencontainers.image.version="1.0.0-SNAPSHOT" \
      org.opencontainers.image.vendor="WALREX" \
      org.opencontainers.image.source="https://github.com/walrex/service-messages" \
      maintainer="read424@gmail.com"

# Configuración de entorno base
ENV LANGUAGE='en_US:en' \
    LANG='en_US.UTF-8' \
    LC_ALL='en_US.UTF-8'

# =============================================================================
# Variables de entorno de la aplicación
# =============================================================================

# Quarkus Profile (usar application-prod.yml por defecto)
ENV QUARKUS_PROFILE=prod

# PostgreSQL
ENV DB_HOST=localhost \
    DB_PORT=5432 \
    DB_NAME=erp_tlm_2021 \
    DB_USERNAME=postgres \
    DB_PASSWORD=postgres

# Redis
ENV REDIS_HOST=redis://localhost:6379

# Consul Service Discovery
ENV CONSUL_HOST=localhost \
    CONSUL_PORT=8500 \
    SERVICE_HOST=localhost

# Kafka
ENV KAFKA_BOOTSTRAP_SERVERS=localhost:9092 \
    SCHEMA_REGISTRY_URL=http://localhost:8081/apis/registry/v2

# Copiar artefactos del build (4 capas separadas para mejor cache)
COPY --from=build --chown=185:root /app/target/quarkus-app/lib/ /deployments/lib/
COPY --from=build --chown=185:root /app/target/quarkus-app/*.jar /deployments/
COPY --from=build --chown=185:root /app/target/quarkus-app/app/ /deployments/app/
COPY --from=build --chown=185:root /app/target/quarkus-app/quarkus/ /deployments/quarkus/

# Puerto de la aplicación
EXPOSE 8089

# Usuario no-root (185 es el usuario por defecto en UBI)
USER 185

# Variables de entorno JVM
# El script run-java.sh calcula automáticamente la memoria óptima
ENV JAVA_APP_JAR="/deployments/quarkus-run.jar" \
    JAVA_OPTS_APPEND="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"

# Health check usando el endpoint de Quarkus
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -sf http://localhost:8089/q/health/ready || exit 1

# Usar el script run-java.sh que optimiza automáticamente la JVM
ENTRYPOINT [ "/opt/jboss/container/java/run/run-java.sh" ]