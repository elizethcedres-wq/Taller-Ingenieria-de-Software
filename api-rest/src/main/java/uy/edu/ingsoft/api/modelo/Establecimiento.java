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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "ESTABLECIMIENTO")
public class Establecimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombreComercial;
    private String direccion;
    private String telefono;
    private String correo;
    private LocalTime horarioApertura;
    private LocalTime horarioCierre;

    @JsonIgnore
    @OneToMany(mappedBy = "establecimiento")
    private Set<Personal> trabajadores = new HashSet<>();

    public Establecimiento() {
    }

    public Establecimiento(
            String nombreComercial,
            String direccion,
            String telefono,
            String correo,
            LocalTime horarioApertura,
            LocalTime horarioCierre
    ) {
        this.nombreComercial = nombreComercial;
        this.direccion = direccion;
        this.telefono = telefono;
        this.correo = correo;
        this.horarioApertura = horarioApertura;
        this.horarioCierre = horarioCierre;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombreComercial() {
        return nombreComercial;
    }

    public void setNombreComercial(String nombreComercial) {
        this.nombreComercial = nombreComercial;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public LocalTime getHorarioApertura() {
        return horarioApertura;
    }

    public void setHorarioApertura(LocalTime horarioApertura) {
        this.horarioApertura = horarioApertura;
    }

    public LocalTime getHorarioCierre() {
        return horarioCierre;
    }

    public void setHorarioCierre(LocalTime horarioCierre) {
        this.horarioCierre = horarioCierre;
    }

    public Set<Personal> getTrabajadores() {
        return trabajadores;
    }

    public void setTrabajadores(Set<Personal> trabajadores) {
        this.trabajadores = trabajadores;
    }

    public void agregarTrabajador(Personal personal) {
        trabajadores.add(personal);
        personal.setEstablecimiento(this);
    }
}
