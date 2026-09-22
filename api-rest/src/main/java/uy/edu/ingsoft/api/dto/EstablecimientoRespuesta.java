/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Record.java to edit this template
 */
package uy.edu.ingsoft.api.dto;

import java.time.LocalTime;
import uy.edu.ingsoft.api.modelo.Establecimiento;

public record EstablecimientoRespuesta(
        Long id,
        String nombreComercial,
        String direccion,
        String telefono,
        String correo,
        LocalTime horarioApertura,
        LocalTime horarioCierre
) {

    public static EstablecimientoRespuesta desde(
            Establecimiento establecimiento
    ) {
        return new EstablecimientoRespuesta(
                establecimiento.getId(),
                establecimiento.getNombreComercial(),
                establecimiento.getDireccion(),
                establecimiento.getTelefono(),
                establecimiento.getCorreo(),
                establecimiento.getHorarioApertura(),
                establecimiento.getHorarioCierre()
        );
    }
}
