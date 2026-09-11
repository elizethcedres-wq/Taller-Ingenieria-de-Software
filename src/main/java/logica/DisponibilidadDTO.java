/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package logica;

/**
 *
 * @author elizeth
 */
public class DisponibilidadDTO {
    private Long personalId;
    private String fechaHoraTurno;

    public DisponibilidadDTO() {
    }

    public Long getPersonalId() {
        return personalId;
    }

    public void setPersonalId(Long personalId) {
        this.personalId = personalId;
    }

    public String getFechaHoraTurno() {
        return fechaHoraTurno;
    }

    public void setFechaHoraTurno(String fechaHoraTurno) {
        this.fechaHoraTurno = fechaHoraTurno;
    }
}
