/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uy.edu.ingsoft.api.modelo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "PERSONAL")
public class Personal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String especialidad;
    private int costoConsulta;
    private int duracionEstandar;
    private Boolean estado;

    @ManyToOne
    @JoinColumn(name = "establecimiento_id")
    private Establecimiento establecimiento;

    @JsonIgnore
    @OneToMany(mappedBy = "personal")
    private Set<ReservaTurno> reservas = new HashSet<>();

    public Personal() {
    }

    public Personal(
            String nombre,
            String especialidad,
            int costoConsulta,
            int duracionEstandar,
            Boolean estado
    ) {
        this.nombre = nombre;
        this.especialidad = especialidad;
        this.costoConsulta = costoConsulta;
        this.duracionEstandar = duracionEstandar;
        this.estado = estado;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(String especialidad) {
        this.especialidad = especialidad;
    }

    public int getCostoConsulta() {
        return costoConsulta;
    }

    public void setCostoConsulta(int costoConsulta) {
        this.costoConsulta = costoConsulta;
    }

    public int getDuracionEstandar() {
        return duracionEstandar;
    }

    public void setDuracionEstandar(int duracionEstandar) {
        this.duracionEstandar = duracionEstandar;
    }

    public Boolean getEstado() {
        return estado;
    }

    public void setEstado(Boolean estado) {
        this.estado = estado;
    }

    public Establecimiento getEstablecimiento() {
        return establecimiento;
    }

    public void setEstablecimiento(Establecimiento establecimiento) {
        this.establecimiento = establecimiento;
    }

    public Set<ReservaTurno> getReservas() {
        return reservas;
    }

    public void setReservas(Set<ReservaTurno> reservas) {
        this.reservas = reservas;
    }
}
