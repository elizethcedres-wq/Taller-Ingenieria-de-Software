package uy.edu.ingsoft.api.servicio;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.ingsoft.api.dto.PersonalEntrada;
import uy.edu.ingsoft.api.dto.PersonalRespuesta;
import uy.edu.ingsoft.api.excepcion.OperacionNoPermitidaException;
import uy.edu.ingsoft.api.excepcion.RecursoNoEncontradoException;
import uy.edu.ingsoft.api.modelo.Establecimiento;
import uy.edu.ingsoft.api.modelo.Personal;
import uy.edu.ingsoft.api.modelo.ReservaTurno;
import uy.edu.ingsoft.api.repositorio.EstablecimientoRepositorio;
import uy.edu.ingsoft.api.repositorio.PersonalRepositorio;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonalServicioTest {

    @Mock
    private PersonalRepositorio personalRepositorio;

    @Mock
    private EstablecimientoRepositorio establecimientoRepositorio;

    private PersonalServicio servicio;

    @BeforeEach
    void preparar() {
        servicio = new PersonalServicio(
                personalRepositorio,
                establecimientoRepositorio
        );
    }

    private Establecimiento establecimiento(Long id) {
        Establecimiento establecimiento = new Establecimiento();
        establecimiento.setId(id);
        return establecimiento;
    }

    private Personal personal(Long id, Establecimiento establecimiento) {
        Personal personal = new Personal(
                "Ana Pereira",
                "Medicina general",
                1500,
                30,
                true
        );
        personal.setId(id);
        personal.setEstablecimiento(establecimiento);
        return personal;
    }

    private PersonalEntrada entrada(Long establecimientoId) {
        return new PersonalEntrada(
                "Nombre actualizado",
                "Cardiologia",
                2000,
                30,
                false,
                establecimientoId
        );
    }

    @Test
    void listaPersonalConSusDatos() {
        Personal personal = personal(7L, establecimiento(2L));
        when(personalRepositorio.findAll())
                .thenReturn(List.of(personal));

        List<PersonalRespuesta> resultado = servicio.listar();

        assertEquals(1, resultado.size());
        assertEquals(PersonalRespuesta.desde(personal), resultado.get(0));
    }

    @Test
    void devuelveListaVaciaCuandoNoHayPersonal() {
        when(personalRepositorio.findAll()).thenReturn(List.of());

        assertTrue(servicio.listar().isEmpty());
    }

    @Test
    void obtienePersonalExistente() {
        Personal personal = personal(7L, establecimiento(2L));
        when(personalRepositorio.findById(7L))
                .thenReturn(Optional.of(personal));

        PersonalRespuesta resultado = servicio.obtener(7L);

        assertEquals(PersonalRespuesta.desde(personal), resultado);
    }

    @Test
    void rechazaConsultaDePersonalInexistente() {
        when(personalRepositorio.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                RecursoNoEncontradoException.class,
                () -> servicio.obtener(99L)
        );
    }

    @Test
    void listaPersonalDeEstablecimientoExistente() {
        Personal personal = personal(7L, establecimiento(2L));

        when(establecimientoRepositorio.existsById(2L))
                .thenReturn(true);
        when(personalRepositorio.findByEstablecimientoId(2L))
                .thenReturn(List.of(personal));

        List<PersonalRespuesta> resultado =
                servicio.listarPorEstablecimiento(2L);

        assertEquals(1, resultado.size());
        assertEquals(2L, resultado.get(0).establecimientoId());
        verify(personalRepositorio).findByEstablecimientoId(2L);
    }

    @Test
    void rechazaListadoDeEstablecimientoInexistente() {
        when(establecimientoRepositorio.existsById(99L))
                .thenReturn(false);

        assertThrows(
                RecursoNoEncontradoException.class,
                () -> servicio.listarPorEstablecimiento(99L)
        );

        verifyNoInteractions(personalRepositorio);
    }

    @Test
    void creaPersonalAsociadoAlEstablecimiento() {
        Establecimiento establecimiento = establecimiento(2L);
        PersonalEntrada entrada = entrada(2L);

        when(establecimientoRepositorio.findById(2L))
                .thenReturn(Optional.of(establecimiento));
        when(personalRepositorio.save(any(Personal.class)))
                .thenAnswer(invocacion -> {
                    Personal guardado = invocacion.getArgument(0);
                    guardado.setId(7L);
                    return guardado;
                });

        PersonalRespuesta resultado = servicio.crear(entrada);

        ArgumentCaptor<Personal> captor =
                ArgumentCaptor.forClass(Personal.class);
        verify(personalRepositorio).save(captor.capture());

        Personal guardado = captor.getValue();

        assertAll(
                () -> assertEquals(7L, resultado.id()),
                () -> assertEquals(entrada.nombre(), guardado.getNombre()),
                () -> assertEquals(
                        entrada.especialidad(), guardado.getEspecialidad()
                ),
                () -> assertEquals(
                        entrada.costoConsulta(), guardado.getCostoConsulta()
                ),
                () -> assertEquals(
                        entrada.duracionEstandar(),
                        guardado.getDuracionEstandar()
                ),
                () -> assertEquals(entrada.estado(), guardado.getEstado()),
                () -> assertSame(
                        establecimiento, guardado.getEstablecimiento()
                )
        );
    }

    @Test
    void noCreaPersonalSiElEstablecimientoNoExiste() {
        when(establecimientoRepositorio.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                RecursoNoEncontradoException.class,
                () -> servicio.crear(entrada(99L))
        );

        verify(personalRepositorio, never()).save(any(Personal.class));
    }

    @Test
    void actualizaDatosYEstablecimientoConservandoElId() {
        Personal existente = personal(7L, establecimiento(2L));
        Establecimiento nuevoEstablecimiento = establecimiento(3L);
        PersonalEntrada entrada = entrada(3L);

        when(personalRepositorio.findById(7L))
                .thenReturn(Optional.of(existente));
        when(establecimientoRepositorio.findById(3L))
                .thenReturn(Optional.of(nuevoEstablecimiento));
        when(personalRepositorio.save(any(Personal.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        PersonalRespuesta resultado = servicio.actualizar(7L, entrada);

        assertAll(
                () -> assertEquals(7L, resultado.id()),
                () -> assertEquals(entrada.nombre(), resultado.nombre()),
                () -> assertEquals(
                        entrada.especialidad(), resultado.especialidad()
                ),
                () -> assertEquals(
                        entrada.costoConsulta(), resultado.costoConsulta()
                ),
                () -> assertEquals(
                        entrada.duracionEstandar(),
                        resultado.duracionEstandar()
                ),
                () -> assertEquals(entrada.estado(), resultado.estado()),
                () -> assertEquals(3L, resultado.establecimientoId())
        );

        verify(personalRepositorio).save(existente);
    }

    @Test
    void noActualizaPersonalInexistente() {
        when(personalRepositorio.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                RecursoNoEncontradoException.class,
                () -> servicio.actualizar(99L, entrada(2L))
        );

        verifyNoInteractions(establecimientoRepositorio);
        verify(personalRepositorio, never()).save(any(Personal.class));
    }

    @Test
    void noActualizaSiElNuevoEstablecimientoNoExiste() {
        Personal existente = personal(7L, establecimiento(2L));

        when(personalRepositorio.findById(7L))
                .thenReturn(Optional.of(existente));
        when(establecimientoRepositorio.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                RecursoNoEncontradoException.class,
                () -> servicio.actualizar(7L, entrada(99L))
        );

        assertEquals("Ana Pereira", existente.getNombre());
        assertEquals(2L, existente.getEstablecimiento().getId());
        verify(personalRepositorio, never()).save(any(Personal.class));
    }

    @Test
    void eliminaPersonalSinReservas() {
        Personal existente = personal(7L, establecimiento(2L));
        when(personalRepositorio.findById(7L))
                .thenReturn(Optional.of(existente));

        servicio.eliminar(7L);

        verify(personalRepositorio).delete(existente);
    }

    @Test
    void impideEliminarPersonalConReservas() {
        Personal existente = personal(7L, establecimiento(2L));
        existente.getReservas().add(new ReservaTurno());

        when(personalRepositorio.findById(7L))
                .thenReturn(Optional.of(existente));

        assertThrows(
                OperacionNoPermitidaException.class,
                () -> servicio.eliminar(7L)
        );

        verify(personalRepositorio, never()).delete(any(Personal.class));
    }

    @Test
    void noEliminaPersonalInexistente() {
        when(personalRepositorio.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                RecursoNoEncontradoException.class,
                () -> servicio.eliminar(99L)
        );

        verify(personalRepositorio, never()).delete(any(Personal.class));
    }
}
