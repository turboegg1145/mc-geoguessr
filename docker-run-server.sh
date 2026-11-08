#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "Starting Minecraft 26.3 Fabric Server with GeoGuessr Mod..."
docker compose up -d server
echo "Server started in background! Attaching logs (Ctrl+C to detach):"
docker compose logs -f server
