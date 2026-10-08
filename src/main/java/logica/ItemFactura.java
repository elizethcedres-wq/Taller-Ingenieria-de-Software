/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package logica;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import java.math.BigDecimal;
import javax.persistence.*;


/**
 *
 * @author elizeth
 */

@Entity
@Table(
        name = "ITEMFACTURA",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = "reserva_id")
        }
)
public class ItemFactura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String descripcion;

    @Column(
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal importe;

    @ManyToOne(optional = false)
    @JoinColumn(
            name = "factura_id",
            nullable = false
    )
    private Factura factura;

    @OneToOne(optional = false)
    @JoinColumn(
            name = "reserva_id",
            nullable = false,
            unique = true
    )
    private ReservaTurno reserva;

    public ItemFactura() {
    }

    public ItemFactura(
            String descripcion,
            BigDecimal importe,
            ReservaTurno reserva
    ) {
        this.descripcion = descripcion;
        this.importe = importe;
        this.reserva = reserva;
    }

    public Long getId() {
        return id;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getImporte() {
        return importe;
    }

    public void setImporte(BigDecimal importe) {
        this.importe = importe;
    }

    public Factura getFactura() {
        return factura;
    }

    public void setFactura(Factura factura) {
        this.factura = factura;
    }

    public ReservaTurno getReserva() {
        return reserva;
    }

    public void setReserva(ReservaTurno reserva) {
        this.reserva = reserva;
    }
}