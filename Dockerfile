# syntax=docker/dockerfile:1

################################################################################
# Stage 1: Builder - Builds the GeoGuessr Fabric Mod JAR for Minecraft 26.3
################################################################################
FROM eclipse-temurin:25-jdk AS builder

WORKDIR /app

# Install build prerequisites
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    git \
    unzip \
    && rm -rf /var/lib/apt/lists/*

# Copy gradle wrapper and project configurations
COPY gradlew gradlew.bat settings.gradle gradle.properties build.gradle ./
COPY gradle/ gradle/

# Cache gradle wrapper distribution
RUN chmod +x ./gradlew && ./gradlew --version

# Copy source code and resources
COPY src/ src/

# Build the mod and execute tests
RUN ./gradlew check build --no-daemon

################################################################################
# Stage 2: Minecraft 26.3 Fabric Dedicated Server with GeoGuessr Mod
################################################################################
FROM eclipse-temurin:25-jdk AS server

LABEL maintainer="Antigravity Team"
LABEL description="Minecraft 26.3 Fabric Server with GeoGuessr Mod pre-installed"

WORKDIR /data

# Install runtime utilities
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    bash \
    procps \
    && rm -rf /var/lib/apt/lists/*

# Copy built mod JAR from builder stage
RUN mkdir -p /opt/geoguessr /data/mods
COPY --from=builder /app/build/libs/geoguessr-1.0.0.jar /opt/geoguessr/

# Copy entrypoint script
COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN chmod +x /usr/local/bin/docker-entrypoint.sh

# Minecraft server port
EXPOSE 25565

# Persistent data directory (worlds, configs, player data)
VOLUME ["/data"]

ENV MEMORY=2G
ENV JAVA_OPTS="-Xms1G -Xmx2G -XX:+UseG1GC"

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]

################################################################################
# Stage 3: Interactive Dev Environment & VS Code Devcontainer
################################################################################
FROM eclipse-temurin:25-jdk AS dev

WORKDIR /workspace

RUN apt-get update && apt-get install -y --no-install-recommends \
    bash \
    curl \
    git \
    make \
    procps \
    unzip \
    vim \
    && rm -rf /var/lib/apt/lists/*

# Setup non-root dev user
ARG USERNAME=developer
ARG USER_UID=1000
ARG USER_GID=$USER_UID

RUN groupadd --gid $USER_GID $USERNAME \
    && useradd --uid $USER_UID --gid $USER_GID -m $USERNAME \
    && mkdir -p /workspace /home/$USERNAME/.gradle \
    && chown -R $USERNAME:$USERNAME /workspace /home/$USERNAME

USER $USERNAME

CMD ["/bin/bash"]
