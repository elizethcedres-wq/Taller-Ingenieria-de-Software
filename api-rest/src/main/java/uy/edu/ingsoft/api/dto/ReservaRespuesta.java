/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Record.java to edit this template
 */
package uy.edu.ingsoft.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import uy.edu.ingsoft.api.modelo.ReservaTurno;
import uy.edu.ingsoft.api.modelo.EstadoTurno;

public record ReservaRespuesta(
        Long id,
        LocalDate fechaReserva,
        String emailSolicitante,
        String telefonoSolicitante,
        Long establecimientoId,
        Long personalId,
        LocalDateTime fechaHoraTurno,
        EstadoTurno estado
) {

    public static ReservaRespuesta desde(ReservaTurno reserva) {
        Long establecimientoId =
                reserva.getEstablecimientoSolicitadoId();

        Long personalId =
                reserva.getPersonalSolicitadoId();

        if (reserva.getPersonal() != null) {
            personalId = reserva.getPersonal().getId();

            if (establecimientoId == null
                    && reserva.getPersonal()
                            .getEstablecimiento() != null) {

                establecimientoId = reserva.getPersonal()
                        .getEstablecimiento()
                        .getId();
            }
        }

        return new ReservaRespuesta(
                reserva.getId(),
                reserva.getFechaReserva(),
                reserva.getEmailSolicitante(),
                reserva.getTelefonoSolicitante(),
                establecimientoId,
                personalId,
                reserva.getFechaHoraTurno(),
                reserva.getEstado()
        );
    }
}