#!/usr/bin/env bash
set -euo pipefail

IMAGE_NAME="delight-account-service:dl"
CONTAINER_NAME="delight-account-service"

echo "[1/3] Building image ${IMAGE_NAME} with BUILD_ENV=dl..."
podman build --build-arg BUILD_ENV=dl -t "${IMAGE_NAME}" .

echo "[2/3] Removing existing container (if any): ${CONTAINER_NAME}"
podman rm -f "${CONTAINER_NAME}" >/dev/null 2>&1 || true

echo "[3/3] Running container ${CONTAINER_NAME} on port 8080, using envVariables_docker_local"
mkdir -p logs
podman run -d \
  --name "${CONTAINER_NAME}" \
  -p 8080:8080 \
  --env-file envVariables_docker_local \
  -v "$(pwd)/logs:/app/logs:Z" \
  "${IMAGE_NAME}"

echo "Done. Logs: podman logs -f ${CONTAINER_NAME}"
echo "Swagger UI: http://localhost:8080/swagger-ui.html"

