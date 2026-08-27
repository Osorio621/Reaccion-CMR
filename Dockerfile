# ── Etapa 1: Build ──────────────────────────────────────────────────────────
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copiar solo el pom primero para aprovechar cache de capas
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copiar el codigo fuente y compilar
COPY src ./src
RUN mvn clean package -DskipTests -q

# ── Etapa 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Crear usuario no-root por seguridad
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copiar el JAR generado en la etapa de build
COPY --from=build /app/target/crm-api-0.0.1-SNAPSHOT.jar app.jar

# Ajustar permisos
RUN chown appuser:appgroup app.jar

USER appuser

# Puerto expuesto
EXPOSE 8080

# Opciones JVM optimizadas para contenedores
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
