#!/usr/bin/env bash
set -Eeuo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/../.."

echo 'Monitor MQTT de la Parte 4. Dejar abierto durante la demostracion.'
echo 'Ctrl+C cierra el monitor; los servicios siguen funcionando.'

docker compose exec -T mosquitto \
    mosquitto_sub -h localhost -t 'turnos/#' -q 1 -v
