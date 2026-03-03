# Stage 1: Build
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

# Copiar arquivos de dependências primeiro (cache layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar código fonte e compilar
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:25-jre
WORKDIR /app

# Instalar FFmpeg (necessário para extração de frames com JavaCV)
RUN apt-get update && \
    apt-get install -y --no-install-recommends ffmpeg && \
    rm -rf /var/lib/apt/lists/*

# Criar usuário não-root
RUN groupadd -r spring && useradd -r -g spring spring

# Criar diretório de processamento de vídeos
RUN mkdir -p /tmp/framedrop-video-processing && chown spring:spring /tmp/framedrop-video-processing

USER spring:spring

# Copiar JAR do stage de build
COPY --from=build /app/target/*.jar app.jar

# Expor porta
EXPOSE 8081

# Healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# Executar aplicação
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", \
  "app.jar"]
