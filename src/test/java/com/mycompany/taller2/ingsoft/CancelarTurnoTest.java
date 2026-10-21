package com.mycompany.taller2.ingsoft;

import com.google.gson.Gson;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import logica.EstadoTurno;
import logica.ReservaTurno;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CancelarTurnoTest {

    @Mock
    private EntityManager em;

    @Mock
    private EntityTransaction transaccion;

    private final Gson gson = new Gson();

    @ParameterizedTest
    @ValueSource(strings = {
        "esto-no-es-json",
        "null",
        "{}",
        "{\"reservaId\":null}",
        "{\"reservaId\":0}",
        "{\"reservaId\":-1}",
        "{\"reservaId\":\"abc\"}"
    })
    void descartaMensajeInvalidoSinAccederALaBase(String json) {
        assertDoesNotThrow(
                () -> SubscriberReservaMQTT.cancelarTurno(
                        json, gson, em
                )
        );

        verifyNoInteractions(em);
    }

    @Test
    void procesaCancelacionValidaDespuesDeMensajeInvalido() {
        assertDoesNotThrow(
                () -> SubscriberReservaMQTT.cancelarTurno(
                        "esto-no-es-json", gson, em
                )
        );

        ReservaTurno reserva = new ReservaTurno();
        reserva.setEstado(EstadoTurno.AGENDADO);

        when(em.find(ReservaTurno.class, 3L)).thenReturn(reserva);
        when(em.getTransaction()).thenReturn(transaccion);

        SubscriberReservaMQTT.cancelarTurno(
                "{\"reservaId\":3}", gson, em
        );

        assertEquals(EstadoTurno.CANCELADO, reserva.getEstado());
        verify(transaccion).begin();
        verify(transaccion).commit();
        verify(transaccion, never()).rollback();
        verify(em).merge(reserva);
        verify(em, never()).remove(any());
    }
}
