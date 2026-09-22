/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Record.java to edit this template
 */
package uy.edu.ingsoft.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import uy.edu.ingsoft.api.modelo.ReservaTurno;

public record ReservaRespuesta(
        Long id,
        LocalDate fechaReserva,
        String emailSolicitante,
        String telefonoSolicitante,
        Long personalId,
        LocalDateTime fechaHoraTurno
) {

    public static ReservaRespuesta desde(ReservaTurno reserva) {
        Long personalId = null;

        if (reserva.getPersonal() != null) {
            personalId = reserva.getPersonal().getId();
        }

        return new ReservaRespuesta(
                reserva.getId(),
                reserva.getFechaReserva(),
                reserva.getEmailSolicitante(),
                reserva.getTelefonoSolicitante(),
                personalId,
                reserva.getFechaHoraTurno()
        );
    }
}
