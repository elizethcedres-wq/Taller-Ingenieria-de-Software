#!/bin/bash

echo "======================================"
echo "       RESERVAS EN LA BASE DE DATOS"
echo "======================================"
echo

docker compose exec mysql-db \
  mysql \
  -uroot \
  -proot \
  -D turnosdb \
  -e 'SELECT id, fechaReserva, emailSolicitante, telefonoSolicitante, personal_id, fechaHoraTurno FROM RESERVATURNO ORDER BY fechaHoraTurno;'
