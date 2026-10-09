package com.mycompany.taller2.ingsoft;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import javax.persistence.*;
import logica.*;
public class FacturadorAutomatico {
    public static void main(String[] args) {
        EntityManagerFactory emf =
                Persistence.createEntityManagerFactory("turnosPU");
        final EventosTurnosMQTT eventos;
        try {
            eventos = new EventosTurnosMQTT("iis-facturador-parte4", "turnos/atendidos");
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
            facturarTurnos(emf, avisadas);
            try {
                Thread.sleep(intervalo * 1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    private static long obtenerIntervalo() {
        String valor = System.getenv("BILLING_INTERVAL_SECONDS");
        try {
            long n = Long.parseLong(valor);
            return n > 0 ? n : 300;
        } catch (NumberFormatException e) {
            return 300;
        }
    }
    private static void facturarTurnos(EntityManagerFactory emf,
            java.util.Set<Long> avisadas) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            java.util.Map<Long, ReservaTurno> lote =
                    new java.util.LinkedHashMap<>();
            for (Long id : avisadas) {
                ReservaTurno r = em.find(ReservaTurno.class, id);
                if (r != null && r.getEstado() == EstadoTurno.ATENDIDO) {
                    lote.put(id, r);
                }
            }
            List<ReservaTurno> recuperadas = em.createQuery(
                    "SELECT r FROM ReservaTurno r "
                    + "WHERE r.estado = :estado ORDER BY r.id",
                    ReservaTurno.class)
                    .setParameter("estado", EstadoTurno.ATENDIDO)
                    .getResultList();
            for (ReservaTurno r : recuperadas) lote.putIfAbsent(r.getId(), r);
            List<ReservaTurno> atendidos = new java.util.ArrayList<>(lote.values());
            atendidos.sort(java.util.Comparator.comparing(ReservaTurno::getId));
            int facturados = 0;
            for (ReservaTurno reserva : atendidos) {
                em.refresh(reserva, javax.persistence.LockModeType.PESSIMISTIC_WRITE);
                if (reserva.getEstado() != EstadoTurno.ATENDIDO) continue;
                if (facturarReserva(em, reserva)) facturados++;
            }
            em.getTransaction().commit();
            System.out.println("Ciclo de facturacion: " + facturados);
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            e.printStackTrace();
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
