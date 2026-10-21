package uy.edu.ingsoft.api.servicio;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.ingsoft.api.dto.EstablecimientoEntrada;
import uy.edu.ingsoft.api.dto.EstablecimientoRespuesta;
import uy.edu.ingsoft.api.excepcion.OperacionNoPermitidaException;
import uy.edu.ingsoft.api.excepcion.RecursoNoEncontradoException;
import uy.edu.ingsoft.api.modelo.Establecimiento;
import uy.edu.ingsoft.api.modelo.Personal;
import uy.edu.ingsoft.api.repositorio.EstablecimientoRepositorio;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EstablecimientoServicioTest {

    @Mock
    private EstablecimientoRepositorio repositorio;

    private EstablecimientoServicio servicio;

    @BeforeEach
    void preparar() {
        servicio = new EstablecimientoServicio(repositorio);
    }

    private Establecimiento establecimiento() {
        Establecimiento establecimiento = new Establecimiento(
                "Clinica original",
                "Direccion original",
                "099111111",
                "original@correo.com",
                LocalTime.of(8, 0),
                LocalTime.of(18, 0)
        );
        establecimiento.setId(2L);
        return establecimiento;
    }

    private EstablecimientoEntrada entrada(
            LocalTime apertura,
            LocalTime cierre
    ) {
        return new EstablecimientoEntrada(
                "Clinica actualizada",
                "Direccion actualizada",
                "099222222",
                "actualizada@correo.com",
                apertura,
                cierre
        );
    }

    private EstablecimientoEntrada entradaValida() {
        return entrada(
                LocalTime.of(9, 0),
                LocalTime.of(19, 0)
        );
    }

    @Test
    void listaEstablecimientos() {
        Establecimiento existente = establecimiento();
        when(repositorio.findAll()).thenReturn(List.of(existente));

        List<EstablecimientoRespuesta> resultado = servicio.listar();

        assertEquals(
                List.of(EstablecimientoRespuesta.desde(existente)),
                resultado
        );
    }

    @Test
    void devuelveListaVaciaCuandoNoHayEstablecimientos() {
        when(repositorio.findAll()).thenReturn(List.of());

        assertTrue(servicio.listar().isEmpty());
    }

    @Test
    void obtieneEstablecimientoExistente() {
        Establecimiento existente = establecimiento();
        when(repositorio.findById(2L))
                .thenReturn(Optional.of(existente));

        assertEquals(
                EstablecimientoRespuesta.desde(existente),
                servicio.obtener(2L)
        );
    }

    @Test
    void rechazaConsultaDeEstablecimientoInexistente() {
        when(repositorio.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                RecursoNoEncontradoException.class,
                () -> servicio.obtener(99L)
        );
    }

    @Test
    void creaEstablecimientoConTodosSusDatos() {
        EstablecimientoEntrada entrada = entradaValida();

        when(repositorio.save(any(Establecimiento.class)))
                .thenAnswer(invocacion -> {
                    Establecimiento guardado = invocacion.getArgument(0);
                    guardado.setId(2L);
                    return guardado;
                });

        EstablecimientoRespuesta resultado = servicio.crear(entrada);

        ArgumentCaptor<Establecimiento> captor =
                ArgumentCaptor.forClass(Establecimiento.class);
        verify(repositorio).save(captor.capture());

        Establecimiento guardado = captor.getValue();

        assertAll(
                () -> assertEquals(2L, resultado.id()),
                () -> assertEquals(
                        entrada.nombreComercial(),
                        guardado.getNombreComercial()
                ),
                () -> assertEquals(
                        entrada.direccion(), guardado.getDireccion()
                ),
                () -> assertEquals(
                        entrada.telefono(), guardado.getTelefono()
                ),
                () -> assertEquals(
                        entrada.correo(), guardado.getCorreo()
                ),
                () -> assertEquals(
                        entrada.horarioApertura(),
                        guardado.getHorarioApertura()
                ),
                () -> assertEquals(
                        entrada.horarioCierre(),
                        guardado.getHorarioCierre()
                )
        );
    }

    @ParameterizedTest
    @CsvSource({
        "18:00, 18:00",
        "19:00, 18:00"
    })
    void noCreaConHorariosIgualesOInvertidos(
            String apertura,
            String cierre
    ) {
        EstablecimientoEntrada entrada = entrada(
                LocalTime.parse(apertura),
                LocalTime.parse(cierre)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.crear(entrada)
        );

        verifyNoInteractions(repositorio);
    }

    @Test
    void actualizaTodosLosDatosConservandoElId() {
        Establecimiento existente = establecimiento();
        EstablecimientoEntrada entrada = entradaValida();

        when(repositorio.findById(2L))
                .thenReturn(Optional.of(existente));
        when(repositorio.save(any(Establecimiento.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        EstablecimientoRespuesta resultado =
                servicio.actualizar(2L, entrada);

        assertEquals(
                new EstablecimientoRespuesta(
                        2L,
                        entrada.nombreComercial(),
                        entrada.direccion(),
                        entrada.telefono(),
                        entrada.correo(),
                        entrada.horarioApertura(),
                        entrada.horarioCierre()
                ),
                resultado
        );

        verify(repositorio).save(existente);
    }

    @Test
    void noActualizaEstablecimientoInexistente() {
        when(repositorio.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                RecursoNoEncontradoException.class,
                () -> servicio.actualizar(99L, entradaValida())
        );

        verify(repositorio, never())
                .save(any(Establecimiento.class));
    }

    @ParameterizedTest
    @CsvSource({
        "18:00, 18:00",
        "19:00, 18:00"
    })
    void noActualizaConHorariosIgualesOInvertidos(
            String apertura,
            String cierre
    ) {
        EstablecimientoEntrada entrada = entrada(
                LocalTime.parse(apertura),
                LocalTime.parse(cierre)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.actualizar(2L, entrada)
        );

        verifyNoInteractions(repositorio);
    }

    @Test
    void eliminaEstablecimientoSinPersonal() {
        Establecimiento existente = establecimiento();
        when(repositorio.findById(2L))
                .thenReturn(Optional.of(existente));

        servicio.eliminar(2L);

        verify(repositorio).delete(existente);
    }

    @Test
    void impideEliminarEstablecimientoConPersonal() {
        Establecimiento existente = establecimiento();
        existente.agregarTrabajador(new Personal());

        when(repositorio.findById(2L))
                .thenReturn(Optional.of(existente));

        assertThrows(
                OperacionNoPermitidaException.class,
                () -> servicio.eliminar(2L)
        );

        verify(repositorio, never())
                .delete(any(Establecimiento.class));
    }

    @Test
    void noEliminaEstablecimientoInexistente() {
        when(repositorio.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                RecursoNoEncontradoException.class,
                () -> servicio.eliminar(99L)
        );

        verify(repositorio, never())
                .delete(any(Establecimiento.class));
    }
}
