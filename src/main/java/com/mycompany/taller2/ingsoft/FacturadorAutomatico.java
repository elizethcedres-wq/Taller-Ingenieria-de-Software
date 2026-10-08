/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.taller2.ingsoft;

/**
 *
 * @author elizeth
 */

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import logica.Cliente;
import logica.EstadoTurno;
import logica.Factura;
import logica.ItemFactura;
import logica.Personal;
import logica.ReservaTurno;


public class FacturadorAutomatico {

    private static final long INTERVALO_PREDETERMINADO = 300;

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
                "Servicio de facturación iniciado."
        );

        System.out.println(
                "Intervalo: "
                + intervaloSegundos
                + " segundos."
        );

        while (!Thread.currentThread().isInterrupted()) {
            facturarTurnos(emf);

            try {
                Thread.sleep(intervaloSegundos * 1000);
            } catch (InterruptedException excepcion) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static long obtenerIntervalo() {
        String valor = System.getenv(
                "BILLING_INTERVAL_SECONDS"
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

    private static void facturarTurnos(
            EntityManagerFactory emf
    ) {
        EntityManager em = emf.createEntityManager();

        try {
            em.getTransaction().begin();

            List<ReservaTurno> atendidos =
                    em.createQuery(
                            "SELECT r "
                            + "FROM ReservaTurno r "
                            + "WHERE r.estado = :estado",
                            ReservaTurno.class
                    )
                    .setParameter(
                            "estado",
                            EstadoTurno.ATENDIDO
                    )
                    .getResultList();

            int facturados = 0;

            for (ReservaTurno reserva : atendidos) {
                if (facturarReserva(em, reserva)) {
                    facturados++;
                }
            }

            em.getTransaction().commit();

            System.out.println(
                    "Ciclo de facturación finalizado. "
                    + "Encontrados: "
                    + atendidos.size()
                    + ", facturados: "
                    + facturados
            );

        } catch (Exception excepcion) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }

            System.err.println(
                    "Error durante la facturación: "
                    + excepcion.getMessage()
            );

            excepcion.printStackTrace();

        } finally {
            em.close();
        }
    }

    private static boolean facturarReserva(
            EntityManager em,
            ReservaTurno reserva
    ) {
        if (reserva.getEmailSolicitante() == null
                || reserva.getEmailSolicitante().isBlank()) {

            System.out.println(
                    "Reserva "
                    + reserva.getId()
                    + " sin correo; no se factura."
            );

            return false;
        }

        Personal personal = reserva.getpersonal();

        if (personal == null) {
            System.out.println(
                    "Reserva "
                    + reserva.getId()
                    + " sin personal; no se factura."
            );

            return false;
        }

        Cliente cliente = buscarOCrearCliente(
                em,
                reserva.getEmailSolicitante(),
                reserva.getTelefonoSolicitante()
        );

        YearMonth periodo = YearMonth.from(
                reserva.getFechaHoraTurno()
        );

        Factura factura = buscarOCrearFactura(
                em,
                cliente,
                periodo
        );

        Long cantidadItems =
                em.createQuery(
                        "SELECT COUNT(i) "
                        + "FROM ItemFactura i "
                        + "WHERE i.reserva.id = :reservaId",
                        Long.class
                )
                .setParameter(
                        "reservaId",
                        reserva.getId()
                )
                .getSingleResult();

        if (cantidadItems > 0) {
            reserva.setEstado(EstadoTurno.FACTURADO);
            return false;
        }

        BigDecimal importe = BigDecimal.valueOf(
                personal.getCostoConsulta()
        );

        String descripcion =
                "Consulta con "
                + personal.getNombre()
                + " - turno "
                + reserva.getId();

        ItemFactura item = new ItemFactura(
                descripcion,
                importe,
                reserva
        );

        factura.agregarItem(item);

        em.persist(item);

        reserva.setEstado(EstadoTurno.FACTURADO);

        System.out.println(
                "Reserva "
                + reserva.getId()
                + " facturada por "
                + importe
                + ". Factura ID: "
                + factura.getId()
        );

        return true;
    }

    private static Cliente buscarOCrearCliente(
            EntityManager em,
            String email,
            String telefono
    ) {
        List<Cliente> encontrados =
                em.createQuery(
                        "SELECT c "
                        + "FROM Cliente c "
                        + "WHERE c.email = :email",
                        Cliente.class
                )
                .setParameter("email", email)
                .setMaxResults(1)
                .getResultList();

        if (!encontrados.isEmpty()) {
            Cliente cliente = encontrados.get(0);

            if (telefono != null
                    && !telefono.isBlank()) {
                cliente.setTelefono(telefono);
            }

            return cliente;
        }

        Cliente cliente = new Cliente(
                email,
                telefono
        );

        em.persist(cliente);
        em.flush();

        return cliente;
    }

    private static Factura buscarOCrearFactura(
            EntityManager em,
            Cliente cliente,
            YearMonth periodo
    ) {
        List<Factura> encontradas =
                em.createQuery(
                        "SELECT f "
                        + "FROM Factura f "
                        + "WHERE f.cliente = :cliente "
                        + "AND f.anio = :anio "
                        + "AND f.mes = :mes",
                        Factura.class
                )
                .setParameter("cliente", cliente)
                .setParameter("anio", periodo.getYear())
                .setParameter(
                        "mes",
                        periodo.getMonthValue()
                )
                .setMaxResults(1)
                .getResultList();

        if (!encontradas.isEmpty()) {
            return encontradas.get(0);
        }

        Factura factura = new Factura(
                cliente,
                periodo.getYear(),
                periodo.getMonthValue(),
                LocalDate.now()
        );

        em.persist(factura);
        em.flush();

        return factura;
    }
}