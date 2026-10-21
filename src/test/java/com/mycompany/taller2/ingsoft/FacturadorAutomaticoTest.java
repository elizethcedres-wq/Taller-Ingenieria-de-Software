package com.mycompany.taller2.ingsoft;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import logica.Cliente;
import logica.EstadoTurno;
import logica.Factura;
import logica.ItemFactura;
import logica.Personal;
import logica.ReservaTurno;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FacturadorAutomaticoTest {

    @Mock
    private EntityManager em;

    @Mock
    private TypedQuery<Cliente> consultaCliente;

    @Mock
    private TypedQuery<Factura> consultaFactura;

    @Mock
    private TypedQuery<Long> consultaItems;

    private ReservaTurno reserva;
    private Cliente cliente;
    private Factura factura;

    @BeforeEach
    void preparar() {
        Personal personal = new Personal(
                "Ana Pereira",
                "Medicina general",
                1500,
                30,
                true
        );

        reserva = new ReservaTurno();
        reserva.setEmailSolicitante("cliente@correo.com");
        reserva.setTelefonoSolicitante("099123456");
        reserva.setFechaHoraTurno(
                LocalDateTime.of(2026, 9, 15, 10, 0)
        );
        reserva.setPersonal(personal);
        reserva.setEstado(EstadoTurno.ATENDIDO);

        cliente = new Cliente(
                "cliente@correo.com",
                "099000000"
        );

        factura = new Factura(
                cliente,
                2026,
                9,
                LocalDate.of(2026, 9, 30)
        );
    }

    private void simularClientes(List<Cliente> clientes) {
        when(em.createQuery(anyString(), eq(Cliente.class)))
                .thenReturn(consultaCliente);
        when(consultaCliente.setParameter(eq("email"), any()))
                .thenReturn(consultaCliente);
        when(consultaCliente.setMaxResults(1))
                .thenReturn(consultaCliente);
        when(consultaCliente.getResultList()).thenReturn(clientes);
    }

    private void simularFacturas(List<Factura> facturas) {
        when(em.createQuery(anyString(), eq(Factura.class)))
                .thenReturn(consultaFactura);
        when(consultaFactura.setParameter(anyString(), any()))
                .thenReturn(consultaFactura);
        when(consultaFactura.setMaxResults(1))
                .thenReturn(consultaFactura);
        when(consultaFactura.getResultList()).thenReturn(facturas);
    }

    private void simularCantidadItems(long cantidad) {
        when(em.createQuery(anyString(), eq(Long.class)))
                .thenReturn(consultaItems);
        when(consultaItems.setParameter(eq("reservaId"), any()))
                .thenReturn(consultaItems);
        when(consultaItems.getSingleResult()).thenReturn(cantidad);
    }

    private void simularFacturaExistente(long cantidadItems) {
        simularClientes(List.of(cliente));
        simularFacturas(List.of(factura));
        simularCantidadItems(cantidadItems);
    }

    @Test
    void noFacturaReservaSinCorreo() {
        reserva.setEmailSolicitante(null);

        assertFalse(FacturadorAutomatico.facturarReserva(em, reserva));
        assertEquals(EstadoTurno.ATENDIDO, reserva.getEstado());
        verifyNoInteractions(em);
    }

    @Test
    void noFacturaReservaConCorreoEnBlanco() {
        reserva.setEmailSolicitante(" ");

        assertFalse(FacturadorAutomatico.facturarReserva(em, reserva));
        verifyNoInteractions(em);
    }

    @Test
    void noFacturaReservaSinPersonal() {
        reserva.setPersonal(null);

        assertFalse(FacturadorAutomatico.facturarReserva(em, reserva));
        assertEquals(EstadoTurno.ATENDIDO, reserva.getEstado());
        verifyNoInteractions(em);
    }

    @Test
    void agregaItemConImporteYReservaCorrectos() {
        simularFacturaExistente(0);

        assertTrue(FacturadorAutomatico.facturarReserva(em, reserva));

        ArgumentCaptor<ItemFactura> captor =
                ArgumentCaptor.forClass(ItemFactura.class);

        verify(em).persist(captor.capture());

        ItemFactura item = captor.getValue();

        assertAll(
                () -> assertSame(reserva, item.getReserva()),
                () -> assertSame(factura, item.getFactura()),
                () -> assertEquals(
                        0,
                        new BigDecimal("1500.00")
                                .compareTo(item.getImporte())
                ),
                () -> assertTrue(
                        item.getDescripcion().contains("Ana Pereira")
                ),
                () -> assertTrue(factura.getItems().contains(item)),
                () -> assertEquals(
                        EstadoTurno.FACTURADO, reserva.getEstado()
                )
        );
    }

    @Test
    void acumulaElImporteSobreElTotalExistente() {
        factura.setTotal(new BigDecimal("2000.00"));
        simularFacturaExistente(0);

        FacturadorAutomatico.facturarReserva(em, reserva);

        assertEquals(
                0,
                new BigDecimal("3500.00").compareTo(factura.getTotal())
        );
    }

    @Test
    void noDuplicaItemSiLaReservaYaTieneUno() {
        factura.setTotal(new BigDecimal("1500.00"));
        simularFacturaExistente(1);

        assertFalse(FacturadorAutomatico.facturarReserva(em, reserva));

        verify(em, never()).persist(any());
        assertTrue(factura.getItems().isEmpty());
        assertEquals(
                0,
                new BigDecimal("1500.00").compareTo(factura.getTotal())
        );
        assertEquals(EstadoTurno.FACTURADO, reserva.getEstado());
    }

    @Test
    void reutilizaClienteYFacturaExistentes() {
        simularFacturaExistente(0);

        FacturadorAutomatico.facturarReserva(em, reserva);

        verify(em, never()).persist(isA(Cliente.class));
        verify(em, never()).persist(isA(Factura.class));
        verify(em).persist(isA(ItemFactura.class));
        verify(consultaCliente).setParameter(
                "email", "cliente@correo.com"
        );
    }

    @Test
    void actualizaTelefonoDelClienteExistente() {
        simularFacturaExistente(0);

        FacturadorAutomatico.facturarReserva(em, reserva);

        assertEquals("099123456", cliente.getTelefono());
    }

    @Test
    void conservaTelefonoSiLaReservaNoIndicaUno() {
        reserva.setTelefonoSolicitante(null);
        simularFacturaExistente(0);

        FacturadorAutomatico.facturarReserva(em, reserva);

        assertEquals("099000000", cliente.getTelefono());
    }

    @Test
    void creaClienteYFacturaCuandoNoExisten() {
        simularClientes(List.of());
        simularFacturas(List.of());
        simularCantidadItems(0);

        LocalDate antes = LocalDate.now();

        assertTrue(FacturadorAutomatico.facturarReserva(em, reserva));

        ArgumentCaptor<Cliente> clienteCaptor =
                ArgumentCaptor.forClass(Cliente.class);
        ArgumentCaptor<Factura> facturaCaptor =
                ArgumentCaptor.forClass(Factura.class);
        ArgumentCaptor<ItemFactura> itemCaptor =
                ArgumentCaptor.forClass(ItemFactura.class);

        verify(em).persist(clienteCaptor.capture());
        verify(em).persist(facturaCaptor.capture());
        verify(em).persist(itemCaptor.capture());

        Cliente creado = clienteCaptor.getValue();
        Factura creada = facturaCaptor.getValue();
        ItemFactura item = itemCaptor.getValue();

        LocalDate despues = LocalDate.now();

        assertAll(
                () -> assertEquals(
                        "cliente@correo.com", creado.getEmail()
                ),
                () -> assertEquals(
                        "099123456", creado.getTelefono()
                ),
                () -> assertSame(creado, creada.getCliente()),
                () -> assertEquals(2026, creada.getAnio()),
                () -> assertEquals(9, creada.getMes()),
                () -> assertTrue(
                        creada.getFechaEmision().equals(antes)
                        || creada.getFechaEmision().equals(despues)
                ),
                () -> assertSame(creada, item.getFactura()),
                () -> assertEquals(
                        0,
                        new BigDecimal("1500.00")
                                .compareTo(creada.getTotal())
                )
        );
    }

    @Test
    void buscaFacturaDelPeriodoDelTurno() {
        simularFacturaExistente(0);

        FacturadorAutomatico.facturarReserva(em, reserva);

        verify(consultaFactura).setParameter("cliente", cliente);
        verify(consultaFactura).setParameter("anio", 2026);
        verify(consultaFactura).setParameter("mes", 9);
    }
}
