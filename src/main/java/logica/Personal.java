/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package logica;
import javax.persistence.*;
import java.util.Set;
import java.util.HashSet;

/**
 *
 * @author Nicolás
 */

@Entity
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
    
    @OneToMany(mappedBy = "personal", cascade = CascadeType.ALL)
    private Set<ReservaTurno> reservaTurno = new HashSet<>();
    
    public Personal(){
        
    }
    
    public Personal(
            String n,
            String e,
            int cC,
            int dE,
            Boolean es
    ){
        this.nombre = n;
        this.especialidad = e;
        this.costoConsulta = cC;
        this.duracionEstandar = dE;
        this.estado = es;
    }
    
    public Long getId(){return id;}
    public void setId(Long id){this.id = id;}
    
    public String getNombre(){return nombre;}
    public void setNombre(String n){this.nombre = n;}
    
    public String getEspecialidad(){return especialidad;}
    public void setEspecialidad(String e){this.especialidad = e;}
    
    public int getCostoConsulta(){return costoConsulta;}
    public void setCostoConsulta(int cC){this.costoConsulta = cC;}
    
    public int getDuracionEstandar(){return duracionEstandar;}
    public void setDuracionEstandar(int dE){this.duracionEstandar = dE;}
    public Boolean getEstado(){return estado;}
    public void setEstado(Boolean es){this.estado = es;}
    
    public Establecimiento getEstablecimiento() {
    return establecimiento;
}

public void setEstablecimiento(Establecimiento establecimiento) {
    this.establecimiento = establecimiento;
}
    
    
}
