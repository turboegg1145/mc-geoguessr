#!/usr/bin/env bash
set -e

DATA_DIR="/data"
mkdir -p "${DATA_DIR}/mods"
cd "${DATA_DIR}"

echo "=========================================================="
echo " Starting Minecraft 26.3 Fabric Server with GeoGuessr Mod "
echo "=========================================================="

# Auto-accept EULA
echo "eula=true" > eula.txt

# Create default server.properties if not present
if [ ! -f "server.properties" ]; then
    echo "Generating default server.properties..."
    cat <<EOF > server.properties
server-port=25565
online-mode=false
motd=\u00A76\u00A7lMinecraft GeoGuessr Server \u00A77(MC 26.3 Fabric)
difficulty=easy
gamemode=adventure
max-players=20
view-distance=12
simulation-distance=10
spawn-protection=0
enable-command-block=true
EOF
fi

# Copy built GeoGuessr Mod JAR into mods directory
if [ -f "/opt/geoguessr/geoguessr-1.0.0.jar" ]; then
    echo "Installing GeoGuessr Mod to ${DATA_DIR}/mods/..."
    cp -f /opt/geoguessr/geoguessr-1.0.0.jar "${DATA_DIR}/mods/"
fi

# Download Fabric API if not already present
FABRIC_API_JAR="${DATA_DIR}/mods/fabric-api-0.165.2+26.3.jar"
if [ ! -f "$FABRIC_API_JAR" ]; then
    echo "Downloading Fabric API for MC 26.3..."
    curl -sSL -o "$FABRIC_API_JAR" \
        "https://cdn.modrinth.com/data/P7dR8mSH/versions/0.165.2+26.3/fabric-api-0.165.2+26.3.jar" || true
fi

# Download Fabric Server Launcher if not present
if [ ! -f "fabric-server-launch.jar" ]; then
    echo "Downloading Fabric Server Launcher for MC 26.3..."
    curl -sSL -o fabric-server-launch.jar \
        "https://meta.fabricmc.net/v2/versions/loader/26.3/0.20.1/1.1.2/server/jar"
fi

# Configure JVM arguments
DEFAULT_JVM_ARGS="-Xms1G -Xmx${MEMORY:-2G} -XX:+UseG1GC"
JVM_ARGS="${JAVA_OPTS:-$DEFAULT_JVM_ARGS}"

echo "Starting server with memory settings: ${JVM_ARGS}"
echo "Server will listen on port 25565..."
exec java ${JVM_ARGS} -jar fabric-server-launch.jar nogui
