/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.taller2.ingsoft;

import java.time.LocalDateTime;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import logica.EstadoTurno;
import logica.Establecimiento;
import logica.Personal;
import logica.ReservaTurno;

public class ProcesadorSolicitudes {

    private static final long INTERVALO_PREDETERMINADO = 60;

    public static void main(String[] args) {
        long intervaloSegundos = obtenerIntervalo();

        EntityManagerFactory emf =
                Persistence.createEntityManagerFactory(
                        "turnosPU"
                );

        Runtime.getRuntime().addShutdownHook(
                new Thread(emf::close)
        );

        System.out.println(
                "Procesador de solicitudes iniciado."
        );

        System.out.println(
                "Intervalo: "
                + intervaloSegundos
                + " segundos."
        );

        while (!Thread.currentThread().isInterrupted()) {
            procesarSolicitudes(emf);

            try {
                Thread.sleep(intervaloSegundos * 1000);
            } catch (InterruptedException excepcion) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static long obtenerIntervalo() {
        String valor = System.getenv(
                "VALIDATION_INTERVAL_SECONDS"
        );

        if (valor == null || valor.isBlank()) {
            return INTERVALO_PREDETERMINADO;
        }

        try {
            long intervalo = Long.parseLong(valor);

            if (intervalo <= 0) {
                return INTERVALO_PREDETERMINADO;
            }

            return intervalo;

        } catch (NumberFormatException excepcion) {
            return INTERVALO_PREDETERMINADO;
        }
    }

    private static void procesarSolicitudes(
            EntityManagerFactory emf
    ) {
        EntityManager em = emf.createEntityManager();

        try {
            em.getTransaction().begin();

            List<ReservaTurno> solicitudes =
                    em.createQuery(
                            "SELECT r "
                            + "FROM ReservaTurno r "
                            + "WHERE r.estado = :estado",
                            ReservaTurno.class
                    )
                    .setParameter(
                            "estado",
                            EstadoTurno.SOLICITADO
                    )
                    .getResultList();

            int rechazadas = 0;
            int validas = 0;

            for (ReservaTurno solicitud : solicitudes) {
                String error = validarSolicitud(
                        em,
                        solicitud
                );

                if (error != null) {
                    solicitud.setEstado(
                            EstadoTurno
                                    .RECHAZADO_SOLICITUD_NO_VALIDA
                    );

                    rechazadas++;

                    System.out.println(
                            "Solicitud "
                            + solicitud.getId()
                            + " rechazada: "
                            + error
                    );

                } else {
                    Personal personal = em.find(
                            Personal.class,
                            solicitud.getPersonalSolicitadoId()
                    );

                    if (estaDisponible(
                            em,
                            solicitud,
                            personal
                    )) {
                        solicitud.setPersonal(personal);
                        solicitud.setEstado(
                                EstadoTurno.AGENDADO
                        );

                        validas++;

                        System.out.println(
                                "Solicitud "
                                + solicitud.getId()
                                + " agendada correctamente."
                        );

                    } else {
                        solicitud.setEstado(
                                EstadoTurno
                                        .RECHAZADO_TURNO_OCUPADO
                        );

                        rechazadas++;

                        System.out.println(
                                "Solicitud "
                                + solicitud.getId()
                                + " rechazada: horario no disponible."
                        );
                    }
                }
            }
            int atendidas = marcarTurnosAtendidos(em);
            em.getTransaction().commit();

            System.out.println(
                    "Turnos marcados como ATENDIDO: "
                    + atendidas
            );
            
            System.out.println(
                    "Ciclo finalizado. Encontradas: "
                    + solicitudes.size()
                    + ", válidas: "
                    + validas
                    + ", rechazadas: "
                    + rechazadas
            );

        } catch (Exception excepcion) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }

            System.err.println(
                    "Error procesando solicitudes: "
                    + excepcion.getMessage()
            );

            excepcion.printStackTrace();

        } finally {
            em.close();
        }
    }

    
        private static int marcarTurnosAtendidos(
            EntityManager em
    ) {
        List<ReservaTurno> agendadas =
                em.createQuery(
                        "SELECT r "
                        + "FROM ReservaTurno r "
                        + "WHERE r.estado = :estado",
                        ReservaTurno.class
                )
                .setParameter(
                        "estado",
                        EstadoTurno.AGENDADO
                )
                .getResultList();

        int cantidad = 0;
        LocalDateTime ahora = LocalDateTime.now();

        for (ReservaTurno reserva : agendadas) {
            int duracion = 30;

            if (reserva.getpersonal() != null
                    && reserva.getpersonal()
                            .getDuracionEstandar() > 0) {

                duracion = reserva.getpersonal()
                        .getDuracionEstandar();
            }

            LocalDateTime finTurno =
                    reserva.getFechaHoraTurno()
                            .plusMinutes(duracion);

            if (!finTurno.isAfter(ahora)) {
                reserva.setEstado(
                        EstadoTurno.ATENDIDO
                );

                cantidad++;

                System.out.println(
                        "Reserva "
                        + reserva.getId()
                        + " marcada como ATENDIDO."
                );
            }
        }

        return cantidad;
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
                        + "AND r.estado = :estado",
                        ReservaTurno.class
                )
                .setParameter(
                        "personalId",
                        personal.getId()
                )
                .setParameter(
                        "estado",
                        EstadoTurno.AGENDADO
                )
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