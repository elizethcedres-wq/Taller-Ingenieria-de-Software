/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Record.java to edit this template
 */
package uy.edu.ingsoft.api.dto;

import uy.edu.ingsoft.api.modelo.Personal;

public record PersonalRespuesta(
        Long id,
        String nombre,
        String especialidad,
        int costoConsulta,
        int duracionEstandar,
        Boolean estado,
        Long establecimientoId
) {

    public static PersonalRespuesta desde(Personal personal) {
        Long establecimientoId = null;

        if (personal.getEstablecimiento() != null) {
            establecimientoId =
                    personal.getEstablecimiento().getId();
        }

        return new PersonalRespuesta(
                personal.getId(),
                personal.getNombre(),
                personal.getEspecialidad(),
                personal.getCostoConsulta(),
                personal.getDuracionEstandar(),
                personal.getEstado(),
                establecimientoId
        );
    }
}
