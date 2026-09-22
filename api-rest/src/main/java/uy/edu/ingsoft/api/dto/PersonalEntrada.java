/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Record.java to edit this template
 */
package uy.edu.ingsoft.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record PersonalEntrada(

        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        @NotBlank(message = "La especialidad es obligatoria")
        String especialidad,

        @PositiveOrZero(
                message = "El costo de consulta no puede ser negativo"
        )
        int costoConsulta,

        @Positive(
                message = "La duración estándar debe ser mayor que cero"
        )
        int duracionEstandar,

        @NotNull(message = "El estado es obligatorio")
        Boolean estado,

        @NotNull(message = "El establecimiento es obligatorio")
        @Positive(message = "El ID del establecimiento debe ser positivo")
        Long establecimientoId
) {
}
