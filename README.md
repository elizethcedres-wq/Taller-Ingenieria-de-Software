
# Taller 2 - Sistema de Reserva de Turnos

## Descripción

Prototipo de AgendaMosquitto, un sistema de reserva de turnos compuesto por módulos independientes que se comunican mediante MQTT.

El sistema permite:

- Registrar reservas.
- Cancelar reservas.
- Consultar disponibilidad de un profesional.
- Consultar la agenda de un profesional para una fecha.
- Persistir las reservas en MySQL.
- Generar reservas de prueba automáticamente.

La comunicación entre los módulos se realiza mediante MQTT. No se utiliza una API REST.

---

## Tecnologías utilizadas

- Java 21
- Maven
- MySQL 8.0
- Eclipse Mosquitto
- MQTT
- Eclipse Paho MQTT
- JPA / EclipseLink
- Docker
- Docker Compose

---

## Arquitectura

El proyecto utiliza los siguientes servicios Docker:

### MySQL

Base de datos utilizada para persistir:

- Establecimientos.
- Personal.
- Reservas.

### Mosquitto

Broker MQTT utilizado para la comunicación entre los módulos.

### App Listener

Consumidor MQTT y persistidor de reservas.


# Taller 2 - Sistema de Reserva de Turnos

## Descripción

Prototipo de un sistema de reserva de turnos compuesto por módulos independientes que se comunican mediante MQTT.

El sistema permite:

- Registrar reservas.
- Cancelar reservas.
- Consultar disponibilidad de un profesional.
- Consultar la agenda de un profesional para una fecha.
- Persistir las reservas en MySQL.
- Generar reservas de prueba automáticamente.

La comunicación entre los módulos se realiza mediante MQTT. No se utiliza una API REST en esta iteración.

---

## Tecnologías utilizadas

- Java 21
- Maven
- MySQL 8.0
- Eclipse Mosquitto
- MQTT
- Eclipse Paho MQTT
- JPA / EclipseLink
- Docker
- Docker Compose

---

## Arquitectura

El proyecto utiliza los siguientes servicios Docker:

### MySQL

Base de datos utilizada para persistir:

- Establecimientos.
- Personal.
- Reservas.

### Mosquitto

Broker MQTT utilizado para la comunicación entre los módulos.

### App Listener

Consumidor MQTT y persistidor de reservas.

Se encarga de:

- Recibir solicitudes de reserva.
- Validar las reglas de negocio.
- Persistir las reservas.
- Procesar cancelaciones.
- Consultar disponibilidad.
- Consultar agendas.

### App Generador

Simula un cliente que genera solicitudes de reserva automáticamente cada 10 segundos y las publica mediante MQTT.

---

## Requisitos

Se necesita tener instalado:

- Docker
- Docker Compose

No es necesario instalar MySQL ni Mosquitto en el sistema anfitrión, ya que ambos funcionan mediante Docker.

---

## Iniciar el sistema

Desde la raíz del proyecto:

```bash
docker compose up -d --build
