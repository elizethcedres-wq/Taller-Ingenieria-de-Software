/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package logica;
import java.time.LocalTime;
import java.time.LocalDate;
import java.time.LocalDateTime;
import javax.persistence.*;

/**
 *
 * @author Nicolás
 */

@Entity
@Table(
    uniqueConstraints = {
        @UniqueConstraint(
            columnNames = {"personal_id", "fechaHoraTurno"}
        )
    }
)
public class ReservaTurno {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDate fechaReserva;
    private String emailSolicitante;
    private String telefonoSolicitante;
    private LocalDateTime fechaHoraTurno;
    
    @ManyToOne
    @JoinColumn(name = "personal_id")
    private Personal personal;
    
    public ReservaTurno(){
        
    }
    
    public ReservaTurno(
            LocalDate fR,
            String eS,
            String tS,
            LocalDateTime fHT
    ){
        this.fechaReserva = fR;
        this.emailSolicitante = eS;
        this.telefonoSolicitante = tS;
        this.fechaHoraTurno = fHT;
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
    
    
    
    
    
    
    
    
}
