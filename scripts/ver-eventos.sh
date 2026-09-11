#!/bin/bash

echo "======================================"
echo "       MONITOR DE EVENTOS MQTT"
echo "======================================"
echo
echo "Escuchando todos los tópicos turnos/#"
echo
echo "Presione Ctrl+C para finalizar."
echo

docker compose exec mosquitto \
  mosquitto_sub \
  -h localhost \
  -t 'turnos/#' \
  -q 1 \
  -v
