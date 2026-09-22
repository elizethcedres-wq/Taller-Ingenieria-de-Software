/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package logica;
import java.time.LocalTime;
import java.util.Set;
import java.util.HashSet;
import javax.persistence.*;

/**
 *
 * @author Nicolás
 */

@Entity
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
    
    @OneToMany(mappedBy = "establecimiento", cascade = CascadeType.ALL)
    private Set<Personal> trabajadores = new HashSet <>();
    
    public Establecimiento(){
        
    }
    
    public Establecimiento(
            String nC,
            String d,
            String t,
            String c,
            LocalTime hA,
            LocalTime hC
    ){
        this.nombreComercial = nC;
        this.direccion = d;
        this.telefono = t;
        this.correo = c;
        this.horarioApertura = hA;
        this.horarioCierre = hC;
    }
    
    public Long getId(){return id;}
    public void setId(Long id){this.id = id;}
    
    public String getNombreComercial(){return nombreComercial;}
    public void setNombreComercial(String nC){this.nombreComercial = nC;}
    
    public String getDireccion(){return direccion;}
    public void setDireccion(String d){this.direccion = d;}
    
    public String getTelefono(){return telefono;}
    public void setTelefono(String t){this.telefono = t;}
    
    public String getCorreo(){return correo;}
    public void setCorreo(String c){this.correo = c;}
    
    public LocalTime getHorarioApertura(){return horarioApertura;}
    public void setHorarioApertura(LocalTime hA){this.horarioApertura = hA;}
    
    public LocalTime getHorarioCierre(){return horarioCierre;}
    public void setHorarioCierre(LocalTime hC){this.horarioCierre = hC;}
    
    public Set<Personal> getTrabajadores() {return trabajadores;}
    public void setTrabajadores(Set<Personal> t) {this.trabajadores = t;}
    
    public void agregarTrabajador(Personal p){trabajadores.add(p);}
    
    
    
    
    
}
