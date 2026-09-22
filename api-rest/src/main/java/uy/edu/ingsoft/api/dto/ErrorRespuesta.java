/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Record.java to edit this template
 */
package uy.edu.ingsoft.api.dto;

import java.time.LocalDateTime;

public record ErrorRespuesta(
        LocalDateTime fechaHora,
        int estado,
        String error,
        String mensaje,
        String ruta
) {
}
