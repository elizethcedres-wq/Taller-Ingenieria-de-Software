# Sistema de Reserva de Turnos — Parte 4

## Descripción

Sistema distribuido para administrar establecimientos, personal y reservas de turnos.

La aplicación expone una API REST desarrollada con Spring Boot y utiliza procesos asíncronos para validar solicitudes, gestionar el ciclo de vida de los turnos y generar la facturación.

## Funcionalidades

- CRUD de establecimientos.
- CRUD de personal.
- Solicitud y consulta de reservas.
- Consulta de disponibilidad.
- Cancelación lógica de reservas.
- Procesamiento asíncrono de solicitudes.
- Control del ciclo de estados de los turnos.
- Registro de clientes.
- Facturación mensual automática.
- Persistencia en MySQL.
- Documentación OpenAPI y Swagger.

## Tecnologías

- Java 21
- Maven
- Spring Boot
- Spring Data JPA
- EclipseLink
- MySQL 8
- Eclipse Mosquitto
- Eclipse Paho MQTT
- Docker
- Docker Compose
- OpenAPI 3
- Swagger UI

## Arquitectura

El sistema está compuesto por los siguientes servicios:

### `mysql-db`

Base de datos MySQL que almacena:

- Establecimientos
- Personal
- Reservas
- Clientes
- Facturas
- Ítems de factura

### `mosquitto`

Broker MQTT utilizado para la comunicación asíncrona.

### `api-rest`

API REST de Spring Boot disponible en:

```text
http://localhost:8080/api
```

Swagger UI:

```text
http://localhost:8080/api/swagger-ui/index.html
```

### `app-listener`

Consumidor MQTT encargado de procesar operaciones como la cancelación de reservas y otras funcionalidades basadas en topics.

### `procesador-solicitudes`

Proceso periódico que analiza las reservas con estado `SOLICITADO`.

Valida:

- Existencia del establecimiento.
- Existencia del personal.
- Relación entre personal y establecimiento.
- Fecha y hora solicitadas.
- Horario del establecimiento.
- Disponibilidad del personal.

Según el resultado, asigna alguno de estos estados:

- `AGENDADO`
- `RECHAZADO_SOLICITUD_NO_VALIDA`
- `RECHAZADO_TURNO_OCUPADO`

También cambia a `ATENDIDO` los turnos agendados cuya fecha y hora ya transcurrieron.

### `facturador`

Proceso periódico que:

- Busca turnos con estado `ATENDIDO`.
- Registra o reutiliza al cliente según su correo.
- Crea o reutiliza la factura mensual del cliente.
- Agrega un ítem por cada turno atendido.
- Actualiza el total de la factura.
- Cambia la reserva al estado `FACTURADO`.

### `app-generador`

Generador de solicitudes utilizado para demostraciones y datos de prueba.

## Ciclo de estados

```text
SOLICITADO
  ├── AGENDADO
  │     ├── CANCELADO
  │     └── ATENDIDO
  │            └── FACTURADO
  ├── RECHAZADO_SOLICITUD_NO_VALIDA
  └── RECHAZADO_TURNO_OCUPADO
```

## Modelo de facturación

- Un cliente puede tener varias facturas.
- Cada cliente posee una factura por mes y año.
- Una factura contiene uno o más ítems.
- Cada reserva puede generar como máximo un ítem de factura.
- El importe del ítem corresponde al costo de consulta del personal.

Restricciones principales:

```text
CLIENTE: UNIQUE(email)
FACTURA: UNIQUE(cliente_id, anio, mes)
ITEMFACTURA: UNIQUE(reserva_id)
```

## Requisitos

- Docker
- Docker Compose
- Java 21 y Maven, únicamente si se ejecutan los módulos fuera de Docker

## Construir los proyectos

Proyecto principal:

```bash
mvn clean install
```

API REST:

```bash
cd api-rest
mvn clean install
cd ..
```

## Iniciar el sistema

Desde la raíz del proyecto:

```bash
docker compose up -d --build
```

Consultar el estado:

```bash
docker compose ps
```

Ver los logs:

```bash
docker compose logs -f api-rest
docker compose logs -f procesador-solicitudes
docker compose logs -f facturador
docker compose logs -f app-listener
```

Detener los servicios:

```bash
docker compose down
```

## Endpoints principales

### Establecimientos

```text
GET    /api/establecimientos
GET    /api/establecimientos/{id}
POST   /api/establecimientos
PUT    /api/establecimientos/{id}
DELETE /api/establecimientos/{id}
```

### Personal

```text
GET    /api/personal
GET    /api/personal/{id}
GET    /api/establecimientos/{id}/personal
POST   /api/establecimientos/{id}/personal
POST   /api/personal
PUT    /api/personal/{id}
DELETE /api/personal/{id}
```

### Reservas

```text
GET    /api/reservas
GET    /api/reservas/{id}
POST   /api/reservas
DELETE /api/reservas/{id}
GET    /api/personal/{id}/reservas
GET    /api/personal/{id}/disponibilidad
```

## Ejemplo de solicitud de reserva

```bash
curl -i -X POST http://localhost:8080/api/reservas \
  -H "Content-Type: application/json" \
  -d '{
    "emailSolicitante": "cliente@correo.com",
    "telefonoSolicitante": "099123456",
    "establecimientoId": 2,
    "personalId": 1,
    "fechaHoraTurno": "2026-12-20T14:00:00"
  }'
```

La API responde con código `202 Accepted` y registra la reserva inicialmente con estado `SOLICITADO`.

## Documentación

- Especificación OpenAPI: `parte3-api.yaml`
- Modelo entidad-relación: `MER-Parte4.pdf`
