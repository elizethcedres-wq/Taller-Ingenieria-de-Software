/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Record.java to edit this template
 */
package uy.edu.ingsoft.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;

public record ReservaEntrada(

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        String emailSolicitante,

        @NotBlank(message = "El teléfono es obligatorio")
        String telefonoSolicitante,

        @NotNull(message = "El ID del personal es obligatorio")
        @Positive(message = "El ID del personal debe ser positivo")
        Long personalId,

        @NotNull(message = "La fecha y hora son obligatorias")
        @Future(message = "La fecha del turno debe ser futura")
        LocalDateTime fechaHoraTurno
) {
}