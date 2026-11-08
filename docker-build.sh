#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================================="
echo " Building GeoGuessr Mod and Docker Container for MC 26.3  "
echo "=========================================================="

echo "[1/2] Building mod jar locally..."
./gradlew build

echo "[2/2] Building Docker images..."
docker compose build

echo "Build complete! Run './docker-run-server.sh' or 'docker compose up server' to launch."
