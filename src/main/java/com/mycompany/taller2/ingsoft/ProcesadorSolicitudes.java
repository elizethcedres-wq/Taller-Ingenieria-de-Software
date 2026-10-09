package com.mycompany.taller2.ingsoft;
import java.time.LocalDateTime;
import java.util.List;
import javax.persistence.*;
import logica.*;
public class ProcesadorSolicitudes {
    public static void main(String[] args) {
        EntityManagerFactory emf =
                Persistence.createEntityManagerFactory("turnosPU");
        final EventosTurnosMQTT eventos;
        try {
            eventos = new EventosTurnosMQTT("iis-procesador-parte4", "turnos/solicitudes");
        } catch (org.eclipse.paho.client.mqttv3.MqttException e) {
            e.printStackTrace();
            emf.close();
            System.exit(1);
            return;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { eventos.close(); } catch (Exception e) { e.printStackTrace(); }
            if (emf.isOpen()) emf.close();
        }));
        long intervalo = obtenerIntervalo();
        System.out.println("Intervalo: " + intervalo + " segundos.");
        while (!Thread.currentThread().isInterrupted()) {
            java.util.Set<Long> avisadas = eventos.recibirPendientes();
            System.out.println("Eventos del ciclo: " + avisadas);
            procesarSolicitudes(emf, avisadas, eventos);
            try {
                Thread.sleep(intervalo * 1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    private static long obtenerIntervalo() {
        String valor = System.getenv("VALIDATION_INTERVAL_SECONDS");
        try {
            long n = Long.parseLong(valor);
            return n > 0 ? n : 60;
        } catch (NumberFormatException e) {
            return 60;
        }
    }
    private static void procesarSolicitudes(EntityManagerFactory emf,
            java.util.Set<Long> avisadas, EventosTurnosMQTT eventos) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            java.util.Map<Long, ReservaTurno> lote =
                    new java.util.LinkedHashMap<>();
            for (Long id : avisadas) {
                ReservaTurno r = em.find(ReservaTurno.class, id);
                if (r != null && r.getEstado() == EstadoTurno.SOLICITADO) {
                    lote.put(id, r);
                }
            }
            List<ReservaTurno> recuperadas = em.createQuery(
                    "SELECT r FROM ReservaTurno r "
                    + "WHERE r.estado = :estado ORDER BY r.id",
                    ReservaTurno.class)
                    .setParameter("estado", EstadoTurno.SOLICITADO)
                    .getResultList();
            for (ReservaTurno r : recuperadas) lote.putIfAbsent(r.getId(), r);
            List<ReservaTurno> solicitudes = new java.util.ArrayList<>(lote.values());
            solicitudes.sort(java.util.Comparator.comparing(ReservaTurno::getId));
            for (ReservaTurno solicitud : solicitudes) {
                em.refresh(solicitud, javax.persistence.LockModeType.PESSIMISTIC_WRITE);
                if (solicitud.getEstado() != EstadoTurno.SOLICITADO) continue;
                String error = validarSolicitud(em, solicitud);
                if (error != null) {
                    solicitud.setEstado(EstadoTurno.RECHAZADO_SOLICITUD_NO_VALIDA);
                    System.out.println("Rechazada " + solicitud.getId() + ": " + error);
                    continue;
                }
                Personal personal = em.find(Personal.class,
                        solicitud.getPersonalSolicitadoId());
                if (estaDisponible(em, solicitud, personal)) {
                    solicitud.setPersonal(personal);
                    solicitud.setEstado(EstadoTurno.AGENDADO);
                    em.flush();
                } else {
                    solicitud.setEstado(EstadoTurno.RECHAZADO_TURNO_OCUPADO);
                }
                System.out.println("Reserva " + solicitud.getId()
                        + " -> " + solicitud.getEstado());
            }
            List<Long> atendidas = marcarTurnosAtendidos(em);
            em.getTransaction().commit();
            System.out.println("Atendidas: " + atendidas.size());
            for (Long id : atendidas) {
                try {
                    eventos.publicar("turnos/atendidos", id);
                } catch (org.eclipse.paho.client.mqttv3.MqttException e) {
                    System.err.println("Evento pendiente " + id + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
    }
private static List<Long> marcarTurnosAtendidos(EntityManager em) {
    List<ReservaTurno> agendadas = em.createQuery(
            "SELECT r FROM ReservaTurno r WHERE r.estado = :estado",
            ReservaTurno.class)
            .setParameter("estado", EstadoTurno.AGENDADO)
            .getResultList();
    List<Long> atendidas = new java.util.ArrayList<>();
    LocalDateTime ahora = LocalDateTime.now();
    for (ReservaTurno reserva : agendadas) {
        em.refresh(reserva, javax.persistence.LockModeType.PESSIMISTIC_WRITE);
        if (reserva.getEstado() != EstadoTurno.AGENDADO) continue;
        int duracion = 30;
        if (reserva.getpersonal() != null
                && reserva.getpersonal().getDuracionEstandar() > 0) {
            duracion = reserva.getpersonal().getDuracionEstandar();
        }
        LocalDateTime fin = reserva.getFechaHoraTurno().plusMinutes(duracion);
        if (!fin.isAfter(ahora)) {
            reserva.setEstado(EstadoTurno.ATENDIDO);
            atendidas.add(reserva.getId());
        }
    }
    return atendidas;
}
    private static boolean estaDisponible(
            EntityManager em,
            ReservaTurno solicitud,
            Personal personal
    ) {
        if (!Boolean.TRUE.equals(personal.getEstado())) {
            return false;
        }
        Establecimiento establecimiento =
                personal.getEstablecimiento();
        if (establecimiento == null) {
            return false;
        }
        int duracion = personal.getDuracionEstandar();
        if (duracion <= 0) {
            duracion = 30;
        }
        LocalDateTime inicioSolicitado =
                solicitud.getFechaHoraTurno();
        LocalDateTime finSolicitado =
                inicioSolicitado.plusMinutes(duracion);
        if (!inicioSolicitado.toLocalDate().equals(
                finSolicitado.toLocalDate()
        )) {
            return false;
        }
        if (inicioSolicitado.toLocalTime().isBefore(
                establecimiento.getHorarioApertura()
        )) {
            return false;
        }
        if (finSolicitado.toLocalTime().isAfter(
                establecimiento.getHorarioCierre()
        )) {
            return false;
        }
        List<ReservaTurno> agendadas =
                em.createQuery(
                        "SELECT r "
                        + "FROM ReservaTurno r "
                        + "WHERE r.personal.id = :personalId "
                        + "AND r.estado IN :estados",
                        ReservaTurno.class
                )
                .setParameter(
                        "personalId",
                        personal.getId()
                )
                .setParameter("estados", List.of(EstadoTurno.AGENDADO,
                        EstadoTurno.ATENDIDO, EstadoTurno.FACTURADO))
                .getResultList();
        for (ReservaTurno existente : agendadas) {
            LocalDateTime inicioExistente =
                    existente.getFechaHoraTurno();
            LocalDateTime finExistente =
                    inicioExistente.plusMinutes(duracion);
            boolean seSuperponen =
                    inicioSolicitado.isBefore(finExistente)
                    && finSolicitado.isAfter(inicioExistente);
            if (seSuperponen) {
                return false;
            }
        }
        return true;
    }
    private static String validarSolicitud(
            EntityManager em,
            ReservaTurno solicitud
    ) {
        Long establecimientoId =
                solicitud.getEstablecimientoSolicitadoId();
        Long personalId =
                solicitud.getPersonalSolicitadoId();
        if (establecimientoId == null) {
            return "no se indicó establecimiento";
        }
        if (personalId == null) {
            return "no se indicó personal";
        }
        Establecimiento establecimiento =
                em.find(
                        Establecimiento.class,
                        establecimientoId
                );
        if (establecimiento == null) {
            return "el establecimiento no existe";
        }
        Personal personal =
                em.find(Personal.class, personalId);
        if (personal == null) {
            return "el personal no existe";
        }
        if (personal.getEstablecimiento() == null
                || !establecimientoId.equals(
                        personal.getEstablecimiento().getId()
                )) {
            return "el personal no trabaja en "
                    + "el establecimiento indicado";
        }
        if (solicitud.getFechaHoraTurno() == null
                || !solicitud.getFechaHoraTurno()
                        .isAfter(LocalDateTime.now())) {
            return "la fecha y hora del turno "
                    + "ya transcurrieron";
        }
        return null;
    }
}
