package uy.edu.ingsoft.api.servicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.ingsoft.api.dto.CancelarReservaMensaje;
import uy.edu.ingsoft.api.dto.ReservaEntrada;
import uy.edu.ingsoft.api.dto.ReservaRespuesta;
import uy.edu.ingsoft.api.dto.SolicitudReservaMensaje;
import uy.edu.ingsoft.api.excepcion.RecursoNoEncontradoException;
import uy.edu.ingsoft.api.modelo.Establecimiento;
import uy.edu.ingsoft.api.modelo.EstadoTurno;
import uy.edu.ingsoft.api.modelo.Personal;
import uy.edu.ingsoft.api.modelo.ReservaTurno;
import uy.edu.ingsoft.api.repositorio.PersonalRepositorio;
import uy.edu.ingsoft.api.repositorio.ReservaTurnoRepositorio;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservaServicioTest {

    @Mock
    private ReservaTurnoRepositorio reservaRepositorio;

    @Mock
    private PersonalRepositorio personalRepositorio;

    @Mock
    private MqttPublicadorServicio mqttPublicador;

    private ReservaServicio servicio;

    private static final LocalDateTime TURNO =
            LocalDateTime.of(2030, 6, 10, 10, 0);

    @BeforeEach
    void preparar() {
        servicio = new ReservaServicio(
                reservaRepositorio,
                personalRepositorio,
                mqttPublicador
        );
    }

    private Personal personalDisponible() {
        Establecimiento establecimiento = new Establecimiento();
        establecimiento.setId(2L);
        establecimiento.setHorarioApertura(LocalTime.of(8, 0));
        establecimiento.setHorarioCierre(LocalTime.of(18, 0));

        Personal personal = new Personal(
                "Ana Pereira",
                "Medicina general",
                1500,
                30,
                true
        );
        personal.setId(1L);
        personal.setEstablecimiento(establecimiento);
        return personal;
    }

    private ReservaTurno reserva(
            Long id,
            EstadoTurno estado,
            LocalDateTime fechaHora
    ) {
        ReservaTurno reserva = new ReservaTurno();
        reserva.setId(id);
        reserva.setFechaReserva(LocalDate.of(2030, 6, 1));
        reserva.setEmailSolicitante("cliente@correo.com");
        reserva.setTelefonoSolicitante("099123456");
        reserva.setEstablecimientoSolicitadoId(2L);
        reserva.setPersonalSolicitadoId(1L);
        reserva.setFechaHoraTurno(fechaHora);
        reserva.setEstado(estado);
        return reserva;
    }

    private void simularPersonalDisponible() {
        when(personalRepositorio.findById(1L))
                .thenReturn(Optional.of(personalDisponible()));
    }

    private void simularReservas(
            LocalDateTime fechaHora,
            List<ReservaTurno> reservas
    ) {
        when(reservaRepositorio
                .findByPersonalIdAndFechaHoraTurnoBetween(
                        1L,
                        fechaHora.minusMinutes(30),
                        fechaHora.plusMinutes(30)
                ))
                .thenReturn(reservas);
    }

    @Test
    void listaReservas() {
        ReservaTurno existente =
                reserva(7L, EstadoTurno.AGENDADO, TURNO);

        when(reservaRepositorio.findAll())
                .thenReturn(List.of(existente));

        assertEquals(
                List.of(ReservaRespuesta.desde(existente)),
                servicio.listar()
        );
    }

    @Test
    void devuelveListaVaciaSinReservas() {
        when(reservaRepositorio.findAll()).thenReturn(List.of());

        assertTrue(servicio.listar().isEmpty());
    }

    @Test
    void obtieneReservaExistente() {
        ReservaTurno existente =
                reserva(7L, EstadoTurno.AGENDADO, TURNO);

        when(reservaRepositorio.findById(7L))
                .thenReturn(Optional.of(existente));

        assertEquals(
                ReservaRespuesta.desde(existente),
                servicio.obtener(7L)
        );
    }

    @Test
    void rechazaConsultaDeReservaInexistente() {
        when(reservaRepositorio.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                RecursoNoEncontradoException.class,
                () -> servicio.obtener(99L)
        );
    }

    @Test
    void listaPorPersonalSinFiltroDeFecha() {
        ReservaTurno existente =
                reserva(7L, EstadoTurno.AGENDADO, TURNO);

        when(personalRepositorio.existsById(1L)).thenReturn(true);
        when(reservaRepositorio.findByPersonalId(1L))
                .thenReturn(List.of(existente));

        assertEquals(
                List.of(ReservaRespuesta.desde(existente)),
                servicio.listarPorPersonal(1L, null)
        );

        verify(reservaRepositorio, never())
                .findByPersonalIdAndFechaHoraTurnoBetween(
                        anyLong(),
                        any(LocalDateTime.class),
                        any(LocalDateTime.class)
                );
    }

    @Test
    void filtraPorTodoElDiaSolicitado() {
        LocalDate fecha = TURNO.toLocalDate();
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin =
                fecha.plusDays(1).atStartOfDay().minusNanos(1);

        ReservaTurno existente =
                reserva(7L, EstadoTurno.AGENDADO, TURNO);

        when(personalRepositorio.existsById(1L)).thenReturn(true);
        when(reservaRepositorio
                .findByPersonalIdAndFechaHoraTurnoBetween(
                        1L, inicio, fin
                ))
                .thenReturn(List.of(existente));

        assertEquals(
                List.of(ReservaRespuesta.desde(existente)),
                servicio.listarPorPersonal(1L, fecha)
        );

        verify(reservaRepositorio)
                .findByPersonalIdAndFechaHoraTurnoBetween(
                        1L, inicio, fin
                );
    }

    @Test
    void noListaReservasDePersonalInexistente() {
        when(personalRepositorio.existsById(99L)).thenReturn(false);

        assertThrows(
                RecursoNoEncontradoException.class,
                () -> servicio.listarPorPersonal(99L, null)
        );

        verifyNoInteractions(reservaRepositorio);
    }

    @Test
    void registraSolicitudYPublicaSuIdentificador() {
        ReservaEntrada entrada = new ReservaEntrada(
                "cliente@correo.com",
                "099123456",
                2L,
                1L,
                TURNO
        );

        LocalDate antes = LocalDate.now();

        when(reservaRepositorio.saveAndFlush(any(ReservaTurno.class)))
                .thenAnswer(invocacion -> {
                    ReservaTurno guardada = invocacion.getArgument(0);
                    guardada.setId(7L);
                    return guardada;
                });

        ReservaRespuesta resultado = servicio.solicitar(entrada);

        var captor = org.mockito.ArgumentCaptor
                .forClass(ReservaTurno.class);

        var orden = inOrder(reservaRepositorio, mqttPublicador);

        orden.verify(reservaRepositorio)
                .saveAndFlush(captor.capture());

        orden.verify(mqttPublicador).publicar(
                eq("turnos/solicitudes"),
                eq(new SolicitudReservaMensaje(7L))
        );

        ReservaTurno guardada = captor.getValue();
        LocalDate despues = LocalDate.now();

        assertAll(
                () -> assertEquals(7L, resultado.id()),
                () -> assertEquals(
                        EstadoTurno.SOLICITADO, resultado.estado()
                ),
                () -> assertEquals(
                        entrada.emailSolicitante(),
                        guardada.getEmailSolicitante()
                ),
                () -> assertEquals(
                        entrada.telefonoSolicitante(),
                        guardada.getTelefonoSolicitante()
                ),
                () -> assertEquals(
                        entrada.establecimientoId(),
                        guardada.getEstablecimientoSolicitadoId()
                ),
                () -> assertEquals(
                        entrada.personalId(),
                        guardada.getPersonalSolicitadoId()
                ),
                () -> assertEquals(
                        TURNO, guardada.getFechaHoraTurno()
                ),
                () -> assertNull(guardada.getPersonal()),
                () -> assertTrue(
                        guardada.getFechaReserva().equals(antes)
                        || guardada.getFechaReserva().equals(despues)
                )
        );

        verifyNoInteractions(personalRepositorio);
    }

    @Test
    void noPublicaSiFallaElGuardado() {
        ReservaEntrada entrada = new ReservaEntrada(
                "cliente@correo.com",
                "099123456",
                2L,
                1L,
                TURNO
        );

        when(reservaRepositorio.saveAndFlush(any(ReservaTurno.class)))
                .thenThrow(new IllegalStateException("Fallo simulado"));

        assertThrows(
                IllegalStateException.class,
                () -> servicio.solicitar(entrada)
        );

        verifyNoInteractions(mqttPublicador);
    }

    @Test
    void propagaFalloDelPublicador() {
        ReservaEntrada entrada = new ReservaEntrada(
                "cliente@correo.com",
                "099123456",
                2L,
                1L,
                TURNO
        );

        when(reservaRepositorio.saveAndFlush(any(ReservaTurno.class)))
                .thenAnswer(invocacion -> {
                    ReservaTurno guardada = invocacion.getArgument(0);
                    guardada.setId(7L);
                    return guardada;
                });

        IllegalStateException fallo =
                new IllegalStateException("Fallo MQTT simulado");

        doThrow(fallo).when(mqttPublicador)
                .publicar(
                        eq("turnos/solicitudes"),
                        any(SolicitudReservaMensaje.class)
                );

        assertSame(
                fallo,
                assertThrows(
                        IllegalStateException.class,
                        () -> servicio.solicitar(entrada)
                )
        );
    }

    @Test
    void publicaSolicitudDeCancelacion() {
        when(reservaRepositorio.existsById(7L)).thenReturn(true);

        assertNotNull(servicio.cancelar(7L));

        verify(mqttPublicador).publicar(
                eq("turnos/cancelar"),
                eq(new CancelarReservaMensaje(7L))
        );

        verify(reservaRepositorio, never())
                .delete(any(ReservaTurno.class));
    }

    @Test
    void noPublicaCancelacionDeReservaInexistente() {
        when(reservaRepositorio.existsById(99L)).thenReturn(false);

        assertThrows(
                RecursoNoEncontradoException.class,
                () -> servicio.cancelar(99L)
        );

        verifyNoInteractions(mqttPublicador);
    }

    @Test
    void rechazaDisponibilidadDePersonalInexistente() {
        when(personalRepositorio.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                RecursoNoEncontradoException.class,
                () -> servicio.estaDisponible(99L, TURNO)
        );

        verifyNoInteractions(reservaRepositorio);
    }

    @Test
    void personalInactivoNoEstaDisponible() {
        Personal personal = personalDisponible();
        personal.setEstado(false);

        when(personalRepositorio.findById(1L))
                .thenReturn(Optional.of(personal));

        assertFalse(servicio.estaDisponible(1L, TURNO));
        verifyNoInteractions(reservaRepositorio);
    }

    @Test
    void personalSinEstablecimientoNoEstaDisponible() {
        Personal personal = personalDisponible();
        personal.setEstablecimiento(null);

        when(personalRepositorio.findById(1L))
                .thenReturn(Optional.of(personal));

        assertFalse(servicio.estaDisponible(1L, TURNO));
        verifyNoInteractions(reservaRepositorio);
    }

    @Test
    void personalConDuracionIncompatibleNoEstaDisponible() {
        Personal personal = personalDisponible();
        personal.setDuracionEstandar(60);

        when(personalRepositorio.findById(1L))
                .thenReturn(Optional.of(personal));

        assertFalse(servicio.estaDisponible(1L, TURNO));
        verifyNoInteractions(reservaRepositorio);
    }

    @ParameterizedTest
    @ValueSource(strings = {"07:59", "17:31"})
    void rechazaTurnosFueraDelHorario(String hora) {
        simularPersonalDisponible();

        LocalDateTime fechaHora =
                TURNO.toLocalDate().atTime(LocalTime.parse(hora));

        assertFalse(servicio.estaDisponible(1L, fechaHora));
        verifyNoInteractions(reservaRepositorio);
    }

    @ParameterizedTest
    @ValueSource(strings = {"08:00", "17:30"})
    void aceptaLosLimitesDelHorario(String hora) {
        simularPersonalDisponible();

        LocalDateTime fechaHora =
                TURNO.toLocalDate().atTime(LocalTime.parse(hora));

        simularReservas(fechaHora, List.of());

        assertTrue(servicio.estaDisponible(1L, fechaHora));
    }

    @Test
    void horarioSinReservasEstaDisponible() {
        simularPersonalDisponible();
        simularReservas(TURNO, List.of());

        assertTrue(servicio.estaDisponible(1L, TURNO));
    }

    @ParameterizedTest
    @EnumSource(
            value = EstadoTurno.class,
            names = {"AGENDADO", "ATENDIDO", "FACTURADO"}
    )
    void estadosQueOcupanElHorarioImpidenReservar(EstadoTurno estado) {
        simularPersonalDisponible();

        simularReservas(
                TURNO,
                List.of(reserva(7L, estado, TURNO))
        );

        assertFalse(servicio.estaDisponible(1L, TURNO));
    }

    @ParameterizedTest
    @EnumSource(
            value = EstadoTurno.class,
            names = {
                "SOLICITADO",
                "CANCELADO",
                "RECHAZADO_SOLICITUD_NO_VALIDA",
                "RECHAZADO_TURNO_OCUPADO"
            }
    )
    void estadosQueNoOcupanElHorarioPermitenReservar(
            EstadoTurno estado
    ) {
        simularPersonalDisponible();

        simularReservas(
                TURNO,
                List.of(reserva(7L, estado, TURNO))
        );

        assertTrue(servicio.estaDisponible(1L, TURNO));
    }

    @ParameterizedTest
    @ValueSource(ints = {-29, 29})
    void rechazaSolapamientoParcial(int diferenciaMinutos) {
        simularPersonalDisponible();

        simularReservas(
                TURNO,
                List.of(reserva(
                        7L,
                        EstadoTurno.AGENDADO,
                        TURNO.plusMinutes(diferenciaMinutos)
                ))
        );

        assertFalse(servicio.estaDisponible(1L, TURNO));
    }

    @ParameterizedTest
    @ValueSource(ints = {-30, 30})
    void permiteTurnosContiguosSinSolapamiento(int diferenciaMinutos) {
        simularPersonalDisponible();

        simularReservas(
                TURNO,
                List.of(reserva(
                        7L,
                        EstadoTurno.AGENDADO,
                        TURNO.plusMinutes(diferenciaMinutos)
                ))
        );

        assertTrue(servicio.estaDisponible(1L, TURNO));
    }
}
