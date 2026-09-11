package logica;

public class ReservaDTO {

    private String emailSolicitante;
    private String telefonoSolicitante;
    private Long personalId;
    private String fechaHoraTurno;

    public String getEmailSolicitante() {
        return emailSolicitante;
    }

    public void setEmailSolicitante(String emailSolicitante) {
        this.emailSolicitante = emailSolicitante;
    }

    public String getTelefonoSolicitante() {
        return telefonoSolicitante;
    }

    public void setTelefonoSolicitante(String telefonoSolicitante) {
        this.telefonoSolicitante = telefonoSolicitante;
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