package com.mycompany.taller2.ingsoft;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import logica.Establecimiento;
import logica.EstadoTurno;
import logica.Personal;
import logica.ReservaTurno;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcesadorSolicitudesTest {

    @Mock
    private EntityManager em;

    @Mock
    private TypedQuery<ReservaTurno> consulta;

    @Mock
    private Establecimiento establecimiento;

    private Personal personal;
    private ReservaTurno solicitud;
    private LocalDateTime turno;

    @BeforeEach
    void preparar() {
        turno = LocalDateTime.now()
                .plusDays(10)
                .toLocalDate()
                .atTime(10, 0);

        personal = new Personal(
                "Ana Pereira",
                "Medicina general",
                1500,
                30,
                true
        );
        personal.setId(1L);
        personal.setEstablecimiento(establecimiento);

        solicitud = new ReservaTurno();
        solicitud.setEstablecimientoSolicitadoId(2L);
        solicitud.setPersonalSolicitadoId(1L);
        solicitud.setFechaHoraTurno(turno);
    }

    private void simularRelacionValida() {
        when(em.find(Establecimiento.class, 2L))
                .thenReturn(establecimiento);
        when(em.find(Personal.class, 1L))
                .thenReturn(personal);
        when(establecimiento.getId()).thenReturn(2L);
    }

    private void simularHorario() {
        when(establecimiento.getHorarioApertura())
                .thenReturn(LocalTime.of(8, 0));
        when(establecimiento.getHorarioCierre())
                .thenReturn(LocalTime.of(18, 0));
    }

    private void simularReservas(List<ReservaTurno> reservas) {
        when(em.createQuery(anyString(), eq(ReservaTurno.class)))
                .thenReturn(consulta);
        when(consulta.setParameter(anyString(), any()))
                .thenReturn(consulta);
        when(consulta.getResultList()).thenReturn(reservas);
    }

    @Test
    void aceptaSolicitudConRelacionYFechaValidas() {
        simularRelacionValida();

        assertNull(
                ProcesadorSolicitudes.validarSolicitud(em, solicitud)
        );
    }

    @Test
    void rechazaSolicitudSinEstablecimiento() {
        solicitud.setEstablecimientoSolicitadoId(null);

        assertEquals(
                "no se indicó establecimiento",
                ProcesadorSolicitudes.validarSolicitud(em, solicitud)
        );

        verifyNoInteractions(em);
    }

    @Test
    void rechazaSolicitudSinPersonal() {
        solicitud.setPersonalSolicitadoId(null);

        assertEquals(
                "no se indicó personal",
                ProcesadorSolicitudes.validarSolicitud(em, solicitud)
        );

        verifyNoInteractions(em);
    }

    @Test
    void rechazaEstablecimientoInexistente() {
        when(em.find(Establecimiento.class, 2L)).thenReturn(null);

        assertEquals(
                "el establecimiento no existe",
                ProcesadorSolicitudes.validarSolicitud(em, solicitud)
        );

        verify(em, never()).find(Personal.class, 1L);
    }

    @Test
    void rechazaPersonalInexistente() {
        when(em.find(Establecimiento.class, 2L))
                .thenReturn(establecimiento);
        when(em.find(Personal.class, 1L)).thenReturn(null);

        assertEquals(
                "el personal no existe",
                ProcesadorSolicitudes.validarSolicitud(em, solicitud)
        );
    }

    @Test
    void rechazaPersonalDeOtroEstablecimiento() {
        when(em.find(Establecimiento.class, 2L))
                .thenReturn(establecimiento);
        when(em.find(Personal.class, 1L)).thenReturn(personal);
        when(establecimiento.getId()).thenReturn(3L);

        assertEquals(
                "el personal no trabaja en el establecimiento indicado",
                ProcesadorSolicitudes.validarSolicitud(em, solicitud)
        );
    }

    @Test
    void rechazaFechaPasada() {
        simularRelacionValida();
        solicitud.setFechaHoraTurno(
                LocalDateTime.now().minusDays(1)
        );

        assertEquals(
                "la fecha y hora del turno ya transcurrieron",
                ProcesadorSolicitudes.validarSolicitud(em, solicitud)
        );
    }

    @Test
    void rechazaFechaAusente() {
        simularRelacionValida();
        solicitud.setFechaHoraTurno(null);

        assertNotNull(
                ProcesadorSolicitudes.validarSolicitud(em, solicitud)
        );
    }

    @Test
    void horarioSinReservasEstaDisponible() {
        simularHorario();
        simularReservas(List.of());

        assertTrue(
                ProcesadorSolicitudes.estaDisponible(
                        em, solicitud, personal
                )
        );
    }

    @Test
    void reservaEnElMismoHorarioImpideAgendar() {
        simularHorario();

        ReservaTurno existente = new ReservaTurno();
        existente.setFechaHoraTurno(turno);
        existente.setEstado(EstadoTurno.AGENDADO);

        simularReservas(List.of(existente));

        assertFalse(
                ProcesadorSolicitudes.estaDisponible(
                        em, solicitud, personal
                )
        );
    }

    @Test
    void turnoContiguoNoSeSolapa() {
        simularHorario();

        ReservaTurno existente = new ReservaTurno();
        existente.setFechaHoraTurno(turno.minusMinutes(30));
        existente.setEstado(EstadoTurno.AGENDADO);

        simularReservas(List.of(existente));

        assertTrue(
                ProcesadorSolicitudes.estaDisponible(
                        em, solicitud, personal
                )
        );
    }

    @Test
    void rechazaTurnoQueTerminaDespuesDelCierre() {
        simularHorario();
        solicitud.setFechaHoraTurno(
                turno.toLocalDate().atTime(17, 45)
        );

        assertFalse(
                ProcesadorSolicitudes.estaDisponible(
                        em, solicitud, personal
                )
        );

        verifyNoInteractions(em);
    }

    @Test
    void personalInactivoNoEstaDisponible() {
        personal.setEstado(false);

        assertFalse(
                ProcesadorSolicitudes.estaDisponible(
                        em, solicitud, personal
                )
        );

        verifyNoInteractions(em);
    }

    @Test
    void marcaComoAtendidoUnTurnoFinalizado() {
        ReservaTurno finalizada = new ReservaTurno();
        finalizada.setEstado(EstadoTurno.AGENDADO);
        finalizada.setPersonal(personal);
        finalizada.setFechaHoraTurno(
                LocalDateTime.now().minusDays(1)
        );

        simularReservas(List.of(finalizada));

        ProcesadorSolicitudes.marcarTurnosAtendidos(em);

        assertEquals(
                EstadoTurno.ATENDIDO,
                finalizada.getEstado()
        );
    }

    @Test
    void mantieneAgendadoUnTurnoFuturo() {
        solicitud.setEstado(EstadoTurno.AGENDADO);
        solicitud.setPersonal(personal);

        simularReservas(List.of(solicitud));

        List<Long> atendidas =
                ProcesadorSolicitudes.marcarTurnosAtendidos(em);

        assertEquals(EstadoTurno.AGENDADO, solicitud.getEstado());
        assertTrue(atendidas.isEmpty());
    }

    @Test
    void mantieneAgendadoUnTurnoQueAunNoTermino() {
        solicitud.setEstado(EstadoTurno.AGENDADO);
        solicitud.setPersonal(personal);
        solicitud.setFechaHoraTurno(
                LocalDateTime.now().minusMinutes(5)
        );

        simularReservas(List.of(solicitud));

        List<Long> atendidas =
                ProcesadorSolicitudes.marcarTurnosAtendidos(em);

        assertEquals(EstadoTurno.AGENDADO, solicitud.getEstado());
        assertTrue(atendidas.isEmpty());
    }
}
