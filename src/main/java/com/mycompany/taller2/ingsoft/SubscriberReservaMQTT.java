package com.mycompany.taller2.ingsoft;

import com.google.gson.Gson;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import logica.AgendaDTO;
import logica.CancelarTurnoDTO;
import logica.DisponibilidadDTO;
import logica.Personal;
import logica.ReservaDTO;
import logica.ReservaTurno;
import org.eclipse.paho.client.mqttv3.*;

public class SubscriberReservaMQTT {

    private static final String BROKER =
            System.getenv("BROKER_URL") != null
                    ? System.getenv("BROKER_URL")
                    : "tcp://127.0.0.1:1883";

    private static final String TOPIC = "turnos/reserva";
    private static final String TOPIC_CANCELAR = "turnos/cancelar";
    private static final String TOPIC_DISPONIBILIDAD ="turnos/disponibilidad";
    private static final String TOPIC_DISPONIBILIDAD_RESPUESTA = "turnos/disponibilidad/respuesta";
    private static final String TOPIC_AGENDA = "turnos/agenda";
    private static final String TOPIC_AGENDA_RESPUESTA = "turnos/agenda/respuesta";

    public static void main(String[] args) {

        try {

            final EntityManagerFactory emf =
                    Persistence.createEntityManagerFactory("turnosPU");

            final EntityManager em =
                    emf.createEntityManager();

            CargoDatosPrueba.cargarSiEsNecesario(em);

            Gson gson = new Gson();

            System.out.println(
                    "Intentando conectar a Mosquitto en: " + BROKER
            );

            final MqttClient cliente =
                    new MqttClient(
                            BROKER,
                            MqttClient.generateClientId()
                    );

            cliente.setCallback(new MqttCallback() {

                @Override
                public void connectionLost(Throwable cause) {

                    System.out.println(
                            "Conexión MQTT perdida: "
                            + cause.getMessage()
                    );
                }

                @Override
                public void messageArrived(
                        String topic,
                        MqttMessage message
                ) throws Exception {

                    String json =
                            new String(message.getPayload());

                    System.out.println("--------------------------------");
                    System.out.println("Mensaje recibido en: " + topic);
                    System.out.println(json);

                    // ==========================================
                    // CANCELAR TURNO
                    // ==========================================

                    if (topic.equals(TOPIC_CANCELAR)) {

                        cancelarTurno(
                                json,
                                gson,
                                em
                        );

                        return;
                    }

                    // ==========================================
                    // CONSULTAR DISPONIBILIDAD
                    // ==========================================

                    if (topic.equals(TOPIC_DISPONIBILIDAD)) {

                        consultarDisponibilidad(
                                json,
                                gson,
                                em,
                                cliente
                        );

                        return;
                    }

                    // ==========================================
                    // CONSULTAR AGENDA
                    // ==========================================

                    if (topic.equals(TOPIC_AGENDA)) {

                        consultarAgenda(
                                json,
                                gson,
                                em,
                                cliente
                        );

                        return;
                    }

                    // ==========================================
                    // CREAR RESERVA
                    // ==========================================

                    if (topic.equals(TOPIC)) {

                        crearReserva(
                                json,
                                gson,
                                em
                        );

                        return;
                    }
                }

                @Override
                public void deliveryComplete(
                        IMqttDeliveryToken token
                ) {
                }
            });

            // ==========================================
            // CONECTAR A MOSQUITTO
            // ==========================================

            cliente.connect();

            System.out.println(
                    "Conectado a Mosquitto correctamente"
            );

            // ==========================================
            // SUSCRIPCIONES
            // ==========================================

            cliente.subscribe(TOPIC);
            cliente.subscribe(TOPIC_CANCELAR);
            cliente.subscribe(TOPIC_DISPONIBILIDAD);
            cliente.subscribe(TOPIC_AGENDA);

            System.out.println(
                    "Escuchando topic: " + TOPIC
            );

            System.out.println(
                    "Escuchando topic: " + TOPIC_CANCELAR
            );

            System.out.println(
                    "Escuchando topic: "
                    + TOPIC_DISPONIBILIDAD
            );

            System.out.println(
                    "Escuchando topic: "
                    + TOPIC_AGENDA
            );

            System.out.println(
                    "Listener activo. Esperando mensajes..."
            );

            CountDownLatch mantenerVivo =
                    new CountDownLatch(1);

            // ==========================================
            // APAGADO CORRECTO
            // ==========================================

            Runtime.getRuntime().addShutdownHook(
                    new Thread(() -> {

                        System.out.println(
                                "\nApagando listener..."
                        );

                        try {

                            if (cliente.isConnected()) {
                                cliente.disconnect();
                            }

                            cliente.close();

                            if (em.isOpen()) {
                                em.close();
                            }

                            if (emf.isOpen()) {
                                emf.close();
                            }

                            System.out.println(
                                    "Listener detenido correctamente"
                            );

                        } catch (Exception e) {

                            e.printStackTrace();
                        }
                    })
            );

            mantenerVivo.await();

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    // =====================================================
    // CREAR RESERVA
    // =====================================================

    private static void crearReserva(
            String json,
            Gson gson,
            EntityManager em
    ) {

        ReservaDTO dto =
                gson.fromJson(
                        json,
                        ReservaDTO.class
                );

        if (dto.getPersonalId() == null) {

            System.out.println(
                    "Reserva rechazada: "
                    + "no se indicó el personal"
            );

            return;
        }

        if (dto.getFechaHoraTurno() == null) {

            System.out.println(
                    "Reserva rechazada: "
                    + "no se indicó fecha y hora"
            );

            return;
        }

        LocalDateTime fechaHoraTurno;

        try {

            fechaHoraTurno =
                    LocalDateTime.parse(
                            dto.getFechaHoraTurno()
                    );

        } catch (Exception e) {

            System.out.println(
                    "Reserva rechazada: "
                    + "formato de fecha/hora inválido"
            );

            return;
        }

        Personal personal =
                em.find(
                        Personal.class,
                        dto.getPersonalId()
                );

        if (personal == null) {

            System.out.println(
                    "Reserva rechazada: Personal "
                    + dto.getPersonalId()
                    + " no existe"
            );

            return;
        }

        if (personal.getEstado() == null
                || !personal.getEstado()) {

            System.out.println(
                    "Reserva rechazada: el Personal "
                    + personal.getId()
                    + " está inactivo"
            );

            return;
        }

        if (personal.getEstablecimiento() == null) {

            System.out.println(
                    "Reserva rechazada: el Personal "
                    + personal.getId()
                    + " no tiene establecimiento asignado"
            );

            return;
        }

        if (personal.getDuracionEstandar() != 30) {

            System.out.println(
                    "Reserva rechazada: la duración del turno "
                    + "debe ser de 30 minutos"
            );

            return;
        }

        LocalTime horaInicio =
                fechaHoraTurno.toLocalTime();

        LocalTime horaFin =
                horaInicio.plusMinutes(30);

        LocalTime apertura =
                personal
                        .getEstablecimiento()
                        .getHorarioApertura();

        LocalTime cierre =
                personal
                        .getEstablecimiento()
                        .getHorarioCierre();

        if (horaInicio.isBefore(apertura)
                || horaFin.isAfter(cierre)) {

            System.out.println(
                    "Reserva rechazada: el turno está "
                    + "fuera del horario del establecimiento"
            );

            System.out.println(
                    "Horario permitido: "
                    + apertura
                    + " - "
                    + cierre
            );

            return;
        }

        // Verificar solapamiento de turnos.
        LocalDateTime limiteInferior =
                fechaHoraTurno.minusMinutes(29);

        LocalDateTime limiteSuperior =
                fechaHoraTurno.plusMinutes(29);

        Long cantidadReservas =
                em.createQuery(
                        "SELECT COUNT(r) "
                        + "FROM ReservaTurno r "
                        + "WHERE r.personal = :personal "
                        + "AND r.fechaHoraTurno >= :limiteInferior "
                        + "AND r.fechaHoraTurno <= :limiteSuperior",
                        Long.class
                )
                .setParameter("personal", personal)
                .setParameter(
                        "limiteInferior",
                        limiteInferior
                )
                .setParameter(
                        "limiteSuperior",
                        limiteSuperior
                )
                .getSingleResult();

        if (cantidadReservas > 0) {

            System.out.println(
                    "Reserva rechazada: el Personal "
                    + personal.getId()
                    + " ya tiene un turno que se solapa "
                    + "con " + fechaHoraTurno
            );

            return;
        }

        ReservaTurno reserva =
                new ReservaTurno();

        reserva.setEmailSolicitante(
                dto.getEmailSolicitante()
        );

        reserva.setTelefonoSolicitante(
                dto.getTelefonoSolicitante()
        );

        reserva.setPersonal(personal);

        reserva.setFechaHoraTurno(
                fechaHoraTurno
        );

        reserva.setFechaReserva(
                LocalDate.now()
        );

        try {

            em.getTransaction().begin();

            em.persist(reserva);

            em.getTransaction().commit();

            System.out.println(
                    "Reserva guardada correctamente."
            );

            System.out.println(
                    "ID = " + reserva.getId()
            );

        } catch (Exception e) {

            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }

            System.out.println(
                    "Reserva rechazada: "
                    + "no se pudo guardar"
            );

            System.out.println(
                    "Motivo: " + e.getMessage()
            );
        }

        System.out.println("--------------------------------");
    }

    // =====================================================
    // CANCELAR TURNO
    // =====================================================

    private static void cancelarTurno(
            String json,
            Gson gson,
            EntityManager em
    ) {

        CancelarTurnoDTO dto =
                gson.fromJson(
                        json,
                        CancelarTurnoDTO.class
                );

        if (dto.getReservaId() == null) {

            System.out.println(
                    "Cancelación rechazada: "
                    + "no se indicó el ID de la reserva"
            );

            return;
        }

        ReservaTurno reserva =
                em.find(
                        ReservaTurno.class,
                        dto.getReservaId()
                );

        if (reserva == null) {

            System.out.println(
                    "Cancelación rechazada: "
                    + "no existe la reserva con ID "
                    + dto.getReservaId()
            );

            return;
        }

        try {

            em.getTransaction().begin();

            em.remove(reserva);

            em.getTransaction().commit();

            System.out.println(
                    "Turno cancelado correctamente."
            );

            System.out.println(
                    "Reserva eliminada. ID = "
                    + dto.getReservaId()
            );

        } catch (Exception e) {

            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }

            System.out.println(
                    "Cancelación rechazada: "
                    + "no se pudo eliminar la reserva"
            );

            System.out.println(
                    "Motivo: " + e.getMessage()
            );
        }

        System.out.println("--------------------------------");
    }

    // =====================================================
    // CONSULTAR DISPONIBILIDAD
    // =====================================================

    private static void consultarDisponibilidad(
            String json,
            Gson gson,
            EntityManager em,
            MqttClient cliente
    ) {

        DisponibilidadDTO dto =
                gson.fromJson(
                        json,
                        DisponibilidadDTO.class
                );

        Map<String, Object> respuesta =
                new HashMap<>();

        respuesta.put(
                "personalId",
                dto.getPersonalId()
        );

        respuesta.put(
                "fechaHoraTurno",
                dto.getFechaHoraTurno()
        );

        if (dto.getPersonalId() == null
                || dto.getFechaHoraTurno() == null) {

            respuesta.put("disponible", false);
            respuesta.put(
                    "motivo",
                    "Faltan datos obligatorios"
            );

            publicarRespuesta(
                    cliente,
                    TOPIC_DISPONIBILIDAD_RESPUESTA,
                    gson,
                    respuesta
            );

            return;
        }

        LocalDateTime fechaHora;

        try {

            fechaHora =
                    LocalDateTime.parse(
                            dto.getFechaHoraTurno()
                    );

        } catch (Exception e) {

            respuesta.put("disponible", false);
            respuesta.put(
                    "motivo",
                    "Formato de fecha/hora inválido"
            );

            publicarRespuesta(
                    cliente,
                    TOPIC_DISPONIBILIDAD_RESPUESTA,
                    gson,
                    respuesta
            );

            return;
        }

        Personal personal =
                em.find(
                        Personal.class,
                        dto.getPersonalId()
                );

        if (personal == null) {

            respuesta.put("disponible", false);
            respuesta.put(
                    "motivo",
                    "El personal no existe"
            );

            publicarRespuesta(
                    cliente,
                    TOPIC_DISPONIBILIDAD_RESPUESTA,
                    gson,
                    respuesta
            );

            return;
        }

        if (personal.getEstado() == null
                || !personal.getEstado()) {

            respuesta.put("disponible", false);
            respuesta.put(
                    "motivo",
                    "El personal está inactivo"
            );

            publicarRespuesta(
                    cliente,
                    TOPIC_DISPONIBILIDAD_RESPUESTA,
                    gson,
                    respuesta
            );

            return;
        }

        if (personal.getEstablecimiento() == null) {

            respuesta.put("disponible", false);
            respuesta.put(
                    "motivo",
                    "El personal no tiene establecimiento"
            );

            publicarRespuesta(
                    cliente,
                    TOPIC_DISPONIBILIDAD_RESPUESTA,
                    gson,
                    respuesta
            );

            return;
        }

        if (personal.getDuracionEstandar() != 30) {

            respuesta.put("disponible", false);
            respuesta.put(
                    "motivo",
                    "La duración del turno no es de 30 minutos"
            );

            publicarRespuesta(
                    cliente,
                    TOPIC_DISPONIBILIDAD_RESPUESTA,
                    gson,
                    respuesta
            );

            return;
        }

        LocalTime horaInicio =
                fechaHora.toLocalTime();

        LocalTime horaFin =
                horaInicio.plusMinutes(30);

        LocalTime apertura =
                personal
                        .getEstablecimiento()
                        .getHorarioApertura();

        LocalTime cierre =
                personal
                        .getEstablecimiento()
                        .getHorarioCierre();

        if (horaInicio.isBefore(apertura)
                || horaFin.isAfter(cierre)) {

            respuesta.put("disponible", false);
            respuesta.put(
                    "motivo",
                    "El horario está fuera del establecimiento"
            );

            publicarRespuesta(
                    cliente,
                    TOPIC_DISPONIBILIDAD_RESPUESTA,
                    gson,
                    respuesta
            );

            return;
        }

        LocalDateTime limiteInferior =
                fechaHora.minusMinutes(29);

        LocalDateTime limiteSuperior =
                fechaHora.plusMinutes(29);

        Long cantidadReservas =
                em.createQuery(
                        "SELECT COUNT(r) "
                        + "FROM ReservaTurno r "
                        + "WHERE r.personal = :personal "
                        + "AND r.fechaHoraTurno >= :limiteInferior "
                        + "AND r.fechaHoraTurno <= :limiteSuperior",
                        Long.class
                )
                .setParameter(
                        "personal",
                        personal
                )
                .setParameter(
                        "limiteInferior",
                        limiteInferior
                )
                .setParameter(
                        "limiteSuperior",
                        limiteSuperior
                )
                .getSingleResult();

        if (cantidadReservas > 0) {

            respuesta.put("disponible", false);
            respuesta.put(
                    "motivo",
                    "El personal ya tiene un turno en ese horario"
            );

        } else {

            respuesta.put("disponible", true);
            respuesta.put(
                    "motivo",
                    "Horario disponible"
            );
        }

        publicarRespuesta(
                cliente,
                TOPIC_DISPONIBILIDAD_RESPUESTA,
                gson,
                respuesta
        );
    }

    // =====================================================
    // CONSULTAR AGENDA
    // =====================================================

    private static void consultarAgenda(
            String json,
            Gson gson,
            EntityManager em,
            MqttClient cliente
    ) {

        AgendaDTO dto =
                gson.fromJson(
                        json,
                        AgendaDTO.class
                );

        Map<String, Object> respuesta =
                new HashMap<>();

        respuesta.put(
                "personalId",
                dto.getPersonalId()
        );

        respuesta.put(
                "fecha",
                dto.getFecha()
        );

        if (dto.getPersonalId() == null
                || dto.getFecha() == null) {

            respuesta.put(
                    "error",
                    "Faltan datos obligatorios"
            );

            publicarRespuesta(
                    cliente,
                    TOPIC_AGENDA_RESPUESTA,
                    gson,
                    respuesta
            );

            return;
        }

        LocalDate fecha;

        try {

            fecha =
                    LocalDate.parse(
                            dto.getFecha()
                    );

        } catch (Exception e) {

            respuesta.put(
                    "error",
                    "Formato de fecha inválido"
            );

            publicarRespuesta(
                    cliente,
                    TOPIC_AGENDA_RESPUESTA,
                    gson,
                    respuesta
            );

            return;
        }

        Personal personal =
                em.find(
                        Personal.class,
                        dto.getPersonalId()
                );

        if (personal == null) {

            respuesta.put(
                    "error",
                    "El personal no existe"
            );

            publicarRespuesta(
                    cliente,
                    TOPIC_AGENDA_RESPUESTA,
                    gson,
                    respuesta
            );

            return;
        }

        LocalDateTime inicio =
                fecha.atStartOfDay();

        LocalDateTime fin =
                fecha.plusDays(1).atStartOfDay();

        List<ReservaTurno> reservas =
                em.createQuery(
                        "SELECT r "
                        + "FROM ReservaTurno r "
                        + "WHERE r.personal = :personal "
                        + "AND r.fechaHoraTurno >= :inicio "
                        + "AND r.fechaHoraTurno < :fin "
                        + "ORDER BY r.fechaHoraTurno",
                        ReservaTurno.class
                )
                .setParameter(
                        "personal",
                        personal
                )
                .setParameter(
                        "inicio",
                        inicio
                )
                .setParameter(
                        "fin",
                        fin
                )
                .getResultList();

        List<Map<String, Object>> turnos =
                new java.util.ArrayList<>();

        for (ReservaTurno reserva : reservas) {

            Map<String, Object> turno =
                    new HashMap<>();

            turno.put(
                    "id",
                    reserva.getId()
            );

            turno.put(
                    "hora",
                    reserva
                            .getFechaHoraTurno()
                            .toLocalTime()
                            .toString()
            );

            turno.put(
                    "emailSolicitante",
                    reserva.getEmailSolicitante()
            );

            turno.put(
                    "telefonoSolicitante",
                    reserva.getTelefonoSolicitante()
            );

            turnos.add(turno);
        }

        respuesta.put(
                "turnos",
                turnos
        );

        respuesta.put(
                "cantidad",
                turnos.size()
        );

        publicarRespuesta(
                cliente,
                TOPIC_AGENDA_RESPUESTA,
                gson,
                respuesta
        );
    }

    // =====================================================
    // PUBLICAR RESPUESTA MQTT
    // =====================================================

    private static void publicarRespuesta(
            MqttClient cliente,
            String topic,
            Gson gson,
            Map<String, Object> respuesta
    ) {

        try {

            String json =
                    gson.toJson(respuesta);

            MqttMessage mensaje =
                    new MqttMessage(
                            json.getBytes()
                    );

            mensaje.setQos(1);

            cliente.publish(
                    topic,
                    mensaje
            );

            System.out.println(
                    "Respuesta publicada en "
                    + topic
                    + ":"
            );

            System.out.println(json);

        } catch (Exception e) {

            System.out.println(
                    "Error publicando respuesta MQTT: "
                    + e.getMessage()
            );
        }
    }
}
