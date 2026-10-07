/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package logica;
import java.time.LocalDate;
import java.time.LocalDateTime;
import javax.persistence.*;

/**
 *
 * @author Nicolás
 */

@Entity
@Table(name = "RESERVATURNO")


public class ReservaTurno {
    
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private EstadoTurno estado = EstadoTurno.SOLICITADO;
    
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDate fechaReserva;
    private String emailSolicitante;
    private String telefonoSolicitante;
    private LocalDateTime fechaHoraTurno;
    
    @Column(name = "establecimiento_solicitado_id")
    private Long establecimientoSolicitadoId;

    @Column(name = "personal_solicitado_id")
    private Long personalSolicitadoId;
    
    @ManyToOne
    @JoinColumn(name = "personal_id")
    private Personal personal;
    
    
   public ReservaTurno() {
    this.estado = EstadoTurno.SOLICITADO;
}

    public ReservaTurno(
            LocalDate fechaReserva,
            String emailSolicitante,
            String telefonoSolicitante,
            LocalDateTime fechaHoraTurno
    ) {
        this.fechaReserva = fechaReserva;
        this.emailSolicitante = emailSolicitante;
        this.telefonoSolicitante = telefonoSolicitante;
        this.fechaHoraTurno = fechaHoraTurno;
        this.estado = EstadoTurno.SOLICITADO;
    }
    
    public LocalDate getFechaReserva(){return fechaReserva;}
    public void setFechaReserva(LocalDate fR){this.fechaReserva = fR;}
    
    public String getEmailSolicitante(){return emailSolicitante;}
    public void setEmailSolicitante(String eS){this.emailSolicitante = eS;}
    
    public String getTelefonoSolicitante(){return telefonoSolicitante;}
    public void setTelefonoSolicitante(String tS){this.telefonoSolicitante = tS;}
    
    public Personal getpersonal(){return personal;}
    public void setPersonal(Personal p){this.personal = p;}
    
    public LocalDateTime getFechaHoraTurno(){return fechaHoraTurno;}
    public void setFechaHoraTurno(LocalDateTime fHT){this.fechaHoraTurno = fHT;}
    
    public Long getId() {return id;}
    
    public Long getEstablecimientoSolicitadoId() {
        return establecimientoSolicitadoId;
    }

    public void setEstablecimientoSolicitadoId(
            Long establecimientoSolicitadoId
    ) {
        this.establecimientoSolicitadoId =
                establecimientoSolicitadoId;
    }

    public Long getPersonalSolicitadoId() {
        return personalSolicitadoId;
    }

    public void setPersonalSolicitadoId(
            Long personalSolicitadoId
    ) {
        this.personalSolicitadoId = personalSolicitadoId;
    }
    
    
    
    public EstadoTurno getEstado() {return estado; }
    public void setEstado(EstadoTurno estado) {this.estado = estado;}
     
    
    
}
