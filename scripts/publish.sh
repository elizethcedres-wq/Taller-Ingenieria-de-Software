#!/bin/bash

EMAIL="${1:-prueba@mail.com}"
TELEFONO="${2:-099123456}"
PERSONAL_ID="${3:-2}"
FECHA_HORA="${4:-2026-10-11T14:00}"

echo "======================================"
echo "       PUBLICANDO RESERVA MQTT"
echo "======================================"
echo
echo "Email:       $EMAIL"
echo "Teléfono:    $TELEFONO"
echo "Personal ID:  $PERSONAL_ID"
echo "Fecha/hora:  $FECHA_HORA"
echo

docker compose exec mosquitto \
  mosquitto_pub \
  -h localhost \
  -t turnos/reserva \
  -q 1 \
  -m "{
    \"emailSolicitante\": \"$EMAIL\",
    \"telefonoSolicitante\": \"$TELEFONO\",
    \"personalId\": $PERSONAL_ID,
    \"fechaHoraTurno\": \"$FECHA_HORA\"
  }"

if [ $? -eq 0 ]; then
    echo
    echo "Mensaje publicado correctamente."
else
    echo
    echo "Error al publicar el mensaje."
    exit 1
fi
