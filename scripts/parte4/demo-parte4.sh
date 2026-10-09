#!/usr/bin/env bash
set -Eeuo pipefail

# Version Ubuntu/Bash de demo-parte4.ps1.
# Prepara datos propios y prueba el recorrido hasta dos items por 3000.
# Acelera solamente la fecha de dos reservas creadas por esta ejecucion.

cd "$(dirname "${BASH_SOURCE[0]}")/../.."

BASE="http://localhost:8080/api"
WORK_DIR="$(mktemp -d -t iis-parte4-XXXXXX)"
EMAIL="demo.parte4.$(date +'%Y-%m-%d.%H-%M-%S-%3N')@example.com"
DATE_TEST="$(date -d '+3 days' +%F)"
TIME_ONE="${DATE_TEST}T14:00:00"
TIME_TWO="${DATE_TEST}T14:30:00"
RESPONSE_BODY=""
LAST_ID=""

cleanup() {
    docker compose start procesador-solicitudes facturador >/dev/null 2>&1 || true
    rm -rf -- "$WORK_DIR"
}
trap cleanup EXIT

docker_cmd() {
    docker "$@"
}

json_field() {
    local field="$1"
    python3 -c 'import json,sys; print(json.load(sys.stdin)[sys.argv[1]])' "$field" <<<"$RESPONSE_BODY"
}

request() {
    local method="$1"
    local route="$2"
    local expected="$3"
    local data="${4-}"
    local output="$WORK_DIR/response.json"
    local status
    local -a args=(
        -sS --connect-timeout 5 --max-time 20
        -X "$method" -o "$output" -w '%{http_code}'
        "${BASE}${route}"
    )

    if [[ -n "$data" ]]; then
        args+=( -H 'Content-Type: application/json' --data-binary "$data" )
    fi

    status="$(curl "${args[@]}")"
    RESPONSE_BODY="$(<"$output")"

    if [[ "$method" != "GET" ]]; then
        echo "$method $route -> HTTP $status"
        [[ -z "$RESPONSE_BODY" ]] || echo "$RESPONSE_BODY"
    fi

    if [[ "$status" != "$expected" ]]; then
        echo "Se esperaba HTTP $expected y se obtuvo HTTP $status" >&2
        [[ -z "$RESPONSE_BODY" ]] || echo "$RESPONSE_BODY" >&2
        return 1
    fi
}

wait_for_state() {
    local id="$1"
    local expected="$2"
    local timeout="${3:-90}"
    local deadline=$((SECONDS + timeout))
    local next_notice=$((SECONDS + 10))
    local state

    echo "Esperando reserva $id -> $expected (hasta $timeout segundos)..."
    while (( SECONDS < deadline )); do
        request GET "/reservas/$id" 200
        state="$(json_field estado)"

        if [[ "$state" == "$expected" ]]; then
            echo "Reserva $id -> $expected"
            return 0
        fi

        if [[ "$state" == RECHAZADO* && "$state" != "$expected" ]]; then
            echo "La reserva $id termino en $state" >&2
            return 1
        fi

        if (( SECONDS >= next_notice )); then
            echo "Reserva $id sigue en $state; esperando $expected..."
            next_notice=$((SECONDS + 10))
        fi
        sleep 2
    done

    echo "La reserva $id no llego a $expected en $timeout segundos" >&2
    return 1
}

reserve() {
    local establishment_id="$1"
    local personal_id="$2"
    local date_time="$3"
    local body

    body="$(printf '{"emailSolicitante":"%s","telefonoSolicitante":"099123456","establecimientoId":%s,"personalId":%s,"fechaHoraTurno":"%s"}' \
        "$EMAIL" "$establishment_id" "$personal_id" "$date_time")"
    request POST '/reservas' 202 "$body"
    LAST_ID="$(json_field id)"
}

echo 'Verificando Docker y servicios de la Parte 4...'
docker_cmd info --format '{{.ServerVersion}}' >/dev/null
running="$(docker_cmd compose ps --services --status running)"
for service in mysql-db mosquitto app-listener api-rest procesador-solicitudes facturador; do
    if ! grep -qx "$service" <<<"$running"; then
        echo "Falta iniciar el servicio $service antes de ejecutar la prueba" >&2
        exit 1
    fi
done
docker_cmd compose stop app-generador >/dev/null

echo 'Paso 16: crear datos de prueba'
request POST '/establecimientos' 201 "$(printf '{"nombreComercial":"Demo Parte 4 A","direccion":"Direccion de prueba","telefono":"099123456","correo":"%s","horarioApertura":"08:00:00","horarioCierre":"18:00:00"}' "$EMAIL")"
EST_A="$(json_field id)"
request POST '/establecimientos' 201 "$(printf '{"nombreComercial":"Demo Parte 4 B","direccion":"Direccion de prueba","telefono":"099123456","correo":"%s","horarioApertura":"08:00:00","horarioCierre":"18:00:00"}' "$EMAIL")"
EST_B="$(json_field id)"
request POST '/personal' 201 "$(printf '{"nombre":"Profesional demo parte 4","especialidad":"Atencion general","costoConsulta":1500,"duracionEstandar":30,"estado":true,"establecimientoId":%s}' "$EST_A")"
PERSONAL_ID="$(json_field id)"

echo 'Paso 17: probar reserva y horario ocupado'
reserve "$EST_A" "$PERSONAL_ID" "$TIME_ONE"; VALID_ID="$LAST_ID"
wait_for_state "$VALID_ID" AGENDADO
reserve "$EST_A" "$PERSONAL_ID" "$TIME_ONE"; OCCUPIED_ID="$LAST_ID"
wait_for_state "$OCCUPIED_ID" RECHAZADO_TURNO_OCUPADO

docker_cmd compose stop procesador-solicitudes >/dev/null
BATCH_TIME="${DATE_TEST}T16:00:00"
reserve "$EST_A" "$PERSONAL_ID" "$BATCH_TIME"; BATCH_A="$LAST_ID"
reserve "$EST_A" "$PERSONAL_ID" "$BATCH_TIME"; BATCH_B="$LAST_ID"
docker_cmd compose start procesador-solicitudes >/dev/null
wait_for_state "$BATCH_A" AGENDADO
wait_for_state "$BATCH_B" RECHAZADO_TURNO_OCUPADO

echo 'Paso 18: probar solicitudes invalidas'
reserve 9223372036854775807 "$PERSONAL_ID" "$TIME_TWO"; NOT_FOUND_ID="$LAST_ID"
wait_for_state "$NOT_FOUND_ID" RECHAZADO_SOLICITUD_NO_VALIDA
reserve "$EST_B" "$PERSONAL_ID" "$TIME_TWO"; FOREIGN_ID="$LAST_ID"
wait_for_state "$FOREIGN_ID" RECHAZADO_SOLICITUD_NO_VALIDA
PAST_TIME="$(date -d '-1 day' +%F)T14:00:00"
reserve "$EST_A" "$PERSONAL_ID" "$PAST_TIME"; OLD_ID="$LAST_ID"
wait_for_state "$OLD_ID" RECHAZADO_SOLICITUD_NO_VALIDA

echo 'Paso 19: cancelar y reutilizar el horario'
request DELETE "/reservas/$VALID_ID" 202
wait_for_state "$VALID_ID" CANCELADO
reserve "$EST_A" "$PERSONAL_ID" "$TIME_ONE"; REUSED_ID="$LAST_ID"
wait_for_state "$REUSED_ID" AGENDADO
reserve "$EST_A" "$PERSONAL_ID" "$TIME_TWO"; SECOND_ID="$LAST_ID"
wait_for_state "$SECOND_ID" AGENDADO

echo 'Paso 20: mostrar estados en MySQL'
SQL="SELECT id,emailSolicitante,personal_solicitado_id,establecimiento_solicitado_id,personal_id,fechaHoraTurno,estado FROM RESERVATURNO WHERE emailSolicitante='$EMAIL' ORDER BY id;"
docker_cmd compose exec -T mysql-db mysql -uroot -proot -D turnosdb -e "$SQL"

echo 'Paso 21: acelerar solamente las dos reservas propias'
docker_cmd compose stop procesador-solicitudes facturador >/dev/null
FIN_TEST="$(date -d 'yesterday 10:00' +'%Y-%m-%dT%H:%M:%S')"
FIN_TEST_TWO="$(date -d 'yesterday 10:30' +'%Y-%m-%dT%H:%M:%S')"
SQL="UPDATE RESERVATURNO SET fechaHoraTurno='$FIN_TEST' WHERE id=$REUSED_ID AND emailSolicitante='$EMAIL' AND estado='AGENDADO'; UPDATE RESERVATURNO SET fechaHoraTurno='$FIN_TEST_TWO' WHERE id=$SECOND_ID AND emailSolicitante='$EMAIL' AND estado='AGENDADO';"
docker_cmd compose exec -T mysql-db mysql -uroot -proot -D turnosdb -e "$SQL"

echo 'Paso 22: observar ATENDIDO'
docker_cmd compose start procesador-solicitudes >/dev/null
wait_for_state "$REUSED_ID" ATENDIDO
wait_for_state "$SECOND_ID" ATENDIDO
docker_cmd compose logs --tail 80 procesador-solicitudes

echo 'Paso 23: observar FACTURADO'
docker_cmd compose start facturador >/dev/null
wait_for_state "$REUSED_ID" FACTURADO 330
wait_for_state "$SECOND_ID" FACTURADO 330
SQL="SELECT c.id,c.email,f.id AS factura_id,f.anio,f.mes,f.total,i.id AS item_id,i.reserva_id,i.importe FROM CLIENTE c JOIN FACTURA f ON f.cliente_id=c.id JOIN ITEMFACTURA i ON i.factura_id=f.id WHERE c.email='$EMAIL' ORDER BY f.id,i.id;"
docker_cmd compose exec -T mysql-db mysql -uroot -proot -D turnosdb -e "$SQL"

echo 'Paso 24: comprobar que no repite la factura'
docker_cmd compose restart facturador >/dev/null
sleep 5
docker_cmd compose exec -T mysql-db mysql -uroot -proot -D turnosdb -e "$SQL"
docker_cmd compose stop facturador >/dev/null
RETRY_SQL="UPDATE RESERVATURNO SET estado='ATENDIDO' WHERE id=$REUSED_ID AND emailSolicitante='$EMAIL' AND estado='FACTURADO';"
docker_cmd compose exec -T mysql-db mysql -uroot -proot -D turnosdb -e "$RETRY_SQL"
docker_cmd compose start facturador >/dev/null
wait_for_state "$REUSED_ID" FACTURADO 330
docker_cmd compose exec -T mysql-db mysql -uroot -proot -D turnosdb -e "$SQL"

echo 'Paso 25: comprobar importes y agrupacion'
SUMMARY_SQL="SELECT COUNT(*),SUM(i.importe) FROM ITEMFACTURA i JOIN FACTURA f ON i.factura_id=f.id JOIN CLIENTE c ON f.cliente_id=c.id WHERE c.email='$EMAIL';"
read -r ITEM_COUNT ITEM_SUM < <(docker_cmd compose exec -T mysql-db mysql -uroot -proot -N -B -D turnosdb -e "$SUMMARY_SQL")
[[ "$ITEM_COUNT" == 2 ]] || { echo 'Se esperaban dos items' >&2; exit 1; }
[[ "$ITEM_SUM" == '3000.00' || "$ITEM_SUM" == '3000' ]] || { echo "El importe esperado es 3000 y se obtuvo $ITEM_SUM" >&2; exit 1; }

GROUP_SQL="SELECT COUNT(DISTINCT c.id),COUNT(DISTINCT f.id),COUNT(i.id) FROM CLIENTE c JOIN FACTURA f ON f.cliente_id=c.id JOIN ITEMFACTURA i ON i.factura_id=f.id WHERE c.email='$EMAIL';"
read -r CLIENT_COUNT INVOICE_COUNT GROUP_ITEM_COUNT < <(docker_cmd compose exec -T mysql-db mysql -uroot -proot -N -B -D turnosdb -e "$GROUP_SQL")
[[ "$CLIENT_COUNT" == 1 && "$INVOICE_COUNT" == 1 && "$GROUP_ITEM_COUNT" == 2 ]] || { echo 'Se esperaban un cliente, una factura y dos items' >&2; exit 1; }

INVOICE_SQL="SELECT f.total,f.anio,f.mes FROM FACTURA f JOIN CLIENTE c ON c.id=f.cliente_id WHERE c.email='$EMAIL';"
read -r INVOICE_TOTAL INVOICE_YEAR INVOICE_MONTH < <(docker_cmd compose exec -T mysql-db mysql -uroot -proot -N -B -D turnosdb -e "$INVOICE_SQL")
EXPECTED_YEAR="$(date -d "$FIN_TEST" +%Y)"
EXPECTED_MONTH="$((10#$(date -d "$FIN_TEST" +%m)))"
[[ "$INVOICE_TOTAL" == '3000.00' || "$INVOICE_TOTAL" == '3000' ]] || { echo 'La factura debe sumar 3000' >&2; exit 1; }
[[ "$INVOICE_YEAR" == "$EXPECTED_YEAR" && "$INVOICE_MONTH" == "$EXPECTED_MONTH" ]] || { echo 'La factura no corresponde al periodo de los turnos' >&2; exit 1; }

OWN_SQL="SELECT COUNT(*) FROM ITEMFACTURA i JOIN FACTURA f ON f.id=i.factura_id JOIN CLIENTE c ON c.id=f.cliente_id WHERE c.email='$EMAIL' AND i.reserva_id IN ($REUSED_ID,$SECOND_ID);"
OWN_ITEMS="$(docker_cmd compose exec -T mysql-db mysql -uroot -proot -N -B -D turnosdb -e "$OWN_SQL" | tr -d '[:space:]')"
[[ "$OWN_ITEMS" == 2 ]] || { echo 'Los items no corresponden a las dos reservas de esta prueba' >&2; exit 1; }

echo 'PARTE4_OK: un cliente, una factura, dos items de 1500, total 3000.'
echo "Email de esta prueba: $EMAIL"
echo "Reservas facturadas: $REUSED_ID, $SECOND_ID"
