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
                    validas++;

                    System.out.println(
                            "Solicitud "
                            + solicitud.getId()
                            + " validada correctamente."
                    );
                }
            }

            em.getTransaction().commit();

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