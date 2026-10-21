#!/usr/bin/env python3

import datetime as dt
import json
import subprocess
import time
import urllib.error
import urllib.request
import uuid

BASE = "http://localhost:8081/api"
COMPOSE = [
    "docker", "compose",
    "-p", "taller-parte5-test",
    "-f", "docker-compose.test.yml",
]

def comprobar(condicion, mensaje):
    if not condicion:
        raise RuntimeError(mensaje)

def api(metodo, ruta, esperado, datos=None):
    contenido = None if datos is None else json.dumps(datos).encode()
    peticion = urllib.request.Request(
        BASE + ruta,
        data=contenido,
        method=metodo,
        headers={"Content-Type": "application/json"},
    )
    try:
        with urllib.request.urlopen(peticion, timeout=10) as respuesta:
            codigo = respuesta.status
            texto = respuesta.read().decode()
    except urllib.error.HTTPError as error:
        codigo = error.code
        texto = error.read().decode()

    comprobar(
        codigo == esperado,
        f"{metodo} {ruta}: esperado {esperado}, obtenido {codigo}: {texto}",
    )
    print(f"{metodo} {ruta} -> HTTP {codigo}", flush=True)
    return json.loads(texto) if texto.strip() else None

def esperar(reserva_id, estado, segundos=45):
    limite = time.monotonic() + segundos
    ultimo = None
    while time.monotonic() < limite:
        reserva = api("GET", f"/reservas/{reserva_id}", 200)
        ultimo = reserva["estado"]
        if ultimo == estado:
            print(f"Reserva {reserva_id} -> {estado}", flush=True)
            return
        comprobar(
            ultimo == "SOLICITADO"
            or (estado == "CANCELADO" and ultimo == "AGENDADO"),
            f"Reserva {reserva_id}: estado inesperado {ultimo}",
        )
        time.sleep(2)
    raise RuntimeError(
        f"Reserva {reserva_id}: no llegó a {estado}; último estado {ultimo}"
    )

def publicar(topic, mensaje):
    subprocess.run(
        COMPOSE + [
            "exec", "-T", "mosquitto",
            "mosquitto_pub", "-h", "localhost",
            "-t", topic, "-q", "1", "-m", mensaje,
        ],
        check=True,
        timeout=15,
    )

def sql(consulta):
    resultado = subprocess.run(
        COMPOSE + [
            "exec", "-T", "mysql-db",
            "mysql", "-uroot", "-proot",
            "--batch", "--skip-column-names",
            "turnosdb", "-e", consulta,
        ],
        check=True,
        capture_output=True,
        text=True,
        timeout=15,
    )
    return resultado.stdout.strip()

def main():
    marca = uuid.uuid4().hex[:12]
    email = f"parte5-{marca}@example.com"
    fecha = (dt.date.today() + dt.timedelta(days=10)).isoformat()

    establecimiento = api("POST", "/establecimientos", 201, {
        "nombreComercial": f"Prueba Parte5 {marca}",
        "direccion": "Direccion de pruebas 123",
        "telefono": "099123456",
        "correo": email,
        "horarioApertura": "08:00:00",
        "horarioCierre": "18:00:00",
    })
    est_id = int(establecimiento["id"])

    personal = api("POST", "/personal", 201, {
        "nombre": f"Profesional Parte5 {marca}",
        "especialidad": "Medicina general",
        "costoConsulta": 1500,
        "duracionEstandar": 30,
        "estado": True,
        "establecimientoId": est_id,
    })
    per_id = int(personal["id"])

    comprobar(
        sql(
            f"SELECT establecimiento_id FROM PERSONAL WHERE id={per_id}"
        ) == str(est_id),
        "La relación personal-establecimiento no quedó persistida",
    )
    print("OK: establecimiento y personal persistidos", flush=True)

    def reservar(hora, personal_id=per_id):
        respuesta = api("POST", "/reservas", 202, {
            "emailSolicitante": email,
            "telefonoSolicitante": "099123456",
            "establecimientoId": est_id,
            "personalId": personal_id,
            "fechaHoraTurno": f"{fecha}T{hora}",
        })
        comprobar(
            respuesta["estado"] == "SOLICITADO",
            "La reserva no se creó como SOLICITADO",
        )
        return int(respuesta["id"])

    def disponibilidad(hora, esperado):
        respuesta = api(
            "GET",
            f"/personal/{per_id}/disponibilidad"
            f"?fechaHora={fecha}T{hora}",
            200,
        )
        comprobar(
            respuesta["disponible"] is esperado,
            f"Disponibilidad incorrecta para {hora}",
        )

    disponibilidad("10:00:00", True)
    original = reservar("10:00:00")
    esperar(original, "AGENDADO")
    disponibilidad("10:00:00", False)

    duplicada = reservar("10:00:00")
    esperar(duplicada, "RECHAZADO_TURNO_OCUPADO")

    api("DELETE", f"/reservas/{original}", 202)
    esperar(original, "CANCELADO")
    disponibilidad("10:00:00", True)

    reutilizada = reservar("10:00:00")
    esperar(reutilizada, "AGENDADO")

    inexistente = 999999
    api("GET", f"/personal/{inexistente}", 404)
    invalida = reservar("11:00:00", inexistente)
    esperar(invalida, "RECHAZADO_SOLICITUD_NO_VALIDA")

    fuera = reservar("17:45:00")
    esperar(fuera, "RECHAZADO_TURNO_OCUPADO")

    for mensaje in [
        "esto-no-es-json", "null", "{}", '{"reservaId":-1}'
    ]:
        publicar("turnos/solicitudes", mensaje)

    posterior = reservar("12:00:00")
    esperar(posterior, "AGENDADO")

    for mensaje in [
        "esto-no-es-json", "null", "{}", '{"reservaId":-1}'
    ]:
        publicar("turnos/cancelar", mensaje)

    api("DELETE", f"/reservas/{posterior}", 202)
    esperar(posterior, "CANCELADO")

    comprobar(
        sql(
            f"SELECT estado FROM RESERVATURNO WHERE id={posterior}"
        ) == "CANCELADO",
        "La cancelación no quedó persistida",
    )

    logs = subprocess.run(
        COMPOSE + ["logs", "--no-color", "procesador-solicitudes"],
        check=True,
        capture_output=True,
        text=True,
        timeout=15,
    ).stdout

    comprobar(
        f"Evento recibido turnos/solicitudes reservaId={posterior}" in logs,
        "No hay evidencia de recepción MQTT de la solicitud posterior",
    )

    print(
        "\nPARTE5_FUNCIONAL_OK\n"
        f"Establecimiento: {est_id}\n"
        f"Personal: {per_id}\n"
        f"Correo de esta ejecución: {email}\n"
        "Registro, reserva, disponibilidad, doble reserva, cancelación,\n"
        "reutilización, profesional inexistente, fuera de agenda y\n"
        "recuperación tras mensajes MQTT inválidos: correctos.",
        flush=True,
    )

if __name__ == "__main__":
    try:
        main()
    except Exception as error:
        print(f"\nPARTE5_FUNCIONAL_ERROR: {error}", flush=True)
        raise SystemExit(1)
