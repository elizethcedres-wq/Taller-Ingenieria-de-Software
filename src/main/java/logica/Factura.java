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
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.*;

/**
 *
 * @author elizeth
 */
@Entity
@Table(
        name = "FACTURA",
        uniqueConstraints = {
            @UniqueConstraint(
                    columnNames = {
                        "cliente_id",
                        "anio",
                        "mes"
                    }
            )
        }
)
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int anio;
    private int mes;
    private LocalDate fechaEmision;

    @Column(
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal total = BigDecimal.ZERO;

    @ManyToOne(optional = false)
    @JoinColumn(
            name = "cliente_id",
            nullable = false
    )
    private Cliente cliente;

    @OneToMany(
            mappedBy = "factura",
            cascade = CascadeType.ALL
    )
    private Set<ItemFactura> items = new HashSet<>();

    public Factura() {
    }

    public Factura(
            Cliente cliente,
            int anio,
            int mes,
            LocalDate fechaEmision
    ) {
        this.cliente = cliente;
        this.anio = anio;
        this.mes = mes;
        this.fechaEmision = fechaEmision;
        this.total = BigDecimal.ZERO;
    }

    public Long getId() {
        return id;
    }

    public int getAnio() {
        return anio;
    }

    public void setAnio(int anio) {
        this.anio = anio;
    }

    public int getMes() {
        return mes;
    }

    public void setMes(int mes) {
        this.mes = mes;
    }

    public LocalDate getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(LocalDate fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public Set<ItemFactura> getItems() {
        return items;
    }

    public void setItems(Set<ItemFactura> items) {
        this.items = items;
    }

    public void agregarItem(ItemFactura item) {
        items.add(item);
        item.setFactura(this);
        total = total.add(item.getImporte());
    }
}
