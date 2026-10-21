package uy.edu.ingsoft.api.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntradasValidacionTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void prepararValidador() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void cerrarValidador() {
        factory.close();
    }

    private static Set<String> camposInvalidos(Object entrada) {
        return validator.validate(entrada)
                .stream()
                .map(error -> error.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    @Test
    void aceptaEstablecimientoValido() {
        var entrada = new EstablecimientoEntrada(
                "Clinica de prueba",
                "Av. Principal 123",
                "099123456",
                "clinica@correo.com",
                LocalTime.of(8, 0),
                LocalTime.of(18, 0)
        );

        assertTrue(validator.validate(entrada).isEmpty());
    }

    @Test
    void rechazaEstablecimientoConCamposObligatoriosAusentes() {
        var entrada = new EstablecimientoEntrada(
                " ", "", null, "", null, null
        );

        assertEquals(
                Set.of(
                        "nombreComercial",
                        "direccion",
                        "telefono",
                        "correo",
                        "horarioApertura",
                        "horarioCierre"
                ),
                camposInvalidos(entrada)
        );
    }

    @Test
    void rechazaCorreoInvalidoDeEstablecimiento() {
        var entrada = new EstablecimientoEntrada(
                "Clinica de prueba",
                "Av. Principal 123",
                "099123456",
                "correo-invalido",
                LocalTime.of(8, 0),
                LocalTime.of(18, 0)
        );

        assertEquals(Set.of("correo"), camposInvalidos(entrada));
    }

    @Test
    void aceptaPersonalValido() {
        var entrada = new PersonalEntrada(
                "Ana Pereira",
                "Medicina general",
                1500,
                30,
                true,
                1L
        );

        assertTrue(validator.validate(entrada).isEmpty());
    }

    @Test
    void aceptaCostoCeroYPersonalInactivo() {
        var entrada = new PersonalEntrada(
                "Ana Pereira",
                "Medicina general",
                0,
                30,
                false,
                1L
        );

        assertTrue(validator.validate(entrada).isEmpty());
    }

    @Test
    void rechazaCostoNegativo() {
        var entrada = new PersonalEntrada(
                "Ana Pereira",
                "Medicina general",
                -1,
                30,
                true,
                1L
        );

        assertEquals(
                Set.of("costoConsulta"),
                camposInvalidos(entrada)
        );
    }

    @Test
    void rechazaDuracionMenorATreinta() {
        var entrada = new PersonalEntrada(
                "Ana Pereira",
                "Medicina general",
                1500,
                29,
                true,
                1L
        );

        assertEquals(
                Set.of("duracionEstandar"),
                camposInvalidos(entrada)
        );
    }

    @Test
    void rechazaDuracionMayorATreinta() {
        var entrada = new PersonalEntrada(
                "Ana Pereira",
                "Medicina general",
                1500,
                31,
                true,
                1L
        );

        assertEquals(
                Set.of("duracionEstandar"),
                camposInvalidos(entrada)
        );
    }

    @Test
    void rechazaPersonalConCamposObligatoriosAusentes() {
        var entrada = new PersonalEntrada(
                "", " ", 1500, 30, null, null
        );

        assertEquals(
                Set.of(
                        "nombre",
                        "especialidad",
                        "estado",
                        "establecimientoId"
                ),
                camposInvalidos(entrada)
        );
    }

    @Test
    void rechazaEstablecimientoNoPositivoEnPersonal() {
        for (long id : new long[]{0L, -1L}) {
            var entrada = new PersonalEntrada(
                    "Ana Pereira",
                    "Medicina general",
                    1500,
                    30,
                    true,
                    id
            );

            assertEquals(
                    Set.of("establecimientoId"),
                    camposInvalidos(entrada),
                    "El ID " + id + " debe rechazarse"
            );
        }
    }

    @Test
    void aceptaSolicitudDeReservaValida() {
        var entrada = new ReservaEntrada(
                "cliente@correo.com",
                "099123456",
                1L,
                1L,
                LocalDateTime.of(2030, 6, 10, 10, 0)
        );

        assertTrue(validator.validate(entrada).isEmpty());
    }

    @Test
    void rechazaReservaConCamposObligatoriosAusentes() {
        var entrada = new ReservaEntrada(
                "", " ", null, null, null
        );

        assertEquals(
                Set.of(
                        "emailSolicitante",
                        "telefonoSolicitante",
                        "establecimientoId",
                        "personalId",
                        "fechaHoraTurno"
                ),
                camposInvalidos(entrada)
        );
    }

    @Test
    void rechazaCorreoInvalidoEnReserva() {
        var entrada = new ReservaEntrada(
                "correo-invalido",
                "099123456",
                1L,
                1L,
                LocalDateTime.of(2030, 6, 10, 10, 0)
        );

        assertEquals(
                Set.of("emailSolicitante"),
                camposInvalidos(entrada)
        );
    }

    @Test
    void rechazaIdsNoPositivosEnReserva() {
        for (long id : new long[]{0L, -1L}) {
            var entrada = new ReservaEntrada(
                    "cliente@correo.com",
                    "099123456",
                    id,
                    id,
                    LocalDateTime.of(2030, 6, 10, 10, 0)
            );

            assertEquals(
                    Set.of("establecimientoId", "personalId"),
                    camposInvalidos(entrada),
                    "El ID " + id + " debe rechazarse"
            );
        }
    }
}
