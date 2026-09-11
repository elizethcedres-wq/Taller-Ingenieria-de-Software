#!/bin/bash

echo "======================================"
echo "      SUSCRIPTOR MQTT - RESERVAS"
echo "======================================"
echo
echo "Topic: turnos/reserva"
echo "Presione Ctrl+C para finalizar."
echo

docker compose exec mosquitto \
  mosquitto_sub \
  -h localhost \
  -t 'turnos/reserva' \
  -q 1 \
  -v
