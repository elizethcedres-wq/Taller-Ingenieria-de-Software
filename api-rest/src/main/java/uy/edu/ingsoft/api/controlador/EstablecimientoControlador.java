/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uy.edu.ingsoft.api.controlador;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import uy.edu.ingsoft.api.dto.EstablecimientoEntrada;
import uy.edu.ingsoft.api.dto.EstablecimientoRespuesta;
import uy.edu.ingsoft.api.servicio.EstablecimientoServicio;

@RestController
@RequestMapping("/establecimientos")
public class EstablecimientoControlador {

    private final EstablecimientoServicio servicio;

    public EstablecimientoControlador(
            EstablecimientoServicio servicio
    ) {
        this.servicio = servicio;
    }

    @GetMapping
    public ResponseEntity<List<EstablecimientoRespuesta>> listar() {
        return ResponseEntity.ok(servicio.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EstablecimientoRespuesta> obtener(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(servicio.obtener(id));
    }

    @PostMapping
    public ResponseEntity<EstablecimientoRespuesta> crear(
            @Valid
            @RequestBody EstablecimientoEntrada entrada
    ) {
        EstablecimientoRespuesta creado =
                servicio.crear(entrada);

        URI ubicacion = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creado.id())
                .toUri();

        return ResponseEntity
                .created(ubicacion)
                .body(creado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EstablecimientoRespuesta> actualizar(
            @PathVariable Long id,
            @Valid
            @RequestBody EstablecimientoEntrada entrada
    ) {
        return ResponseEntity.ok(
                servicio.actualizar(id, entrada)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id
    ) {
        servicio.eliminar(id);

        return ResponseEntity.noContent().build();
    }
}