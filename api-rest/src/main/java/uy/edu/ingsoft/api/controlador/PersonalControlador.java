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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import uy.edu.ingsoft.api.dto.PersonalEntrada;
import uy.edu.ingsoft.api.dto.PersonalRespuesta;
import uy.edu.ingsoft.api.servicio.PersonalServicio;

@RestController
public class PersonalControlador {

    private final PersonalServicio servicio;

    public PersonalControlador(PersonalServicio servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/personal")
    public ResponseEntity<List<PersonalRespuesta>> listar() {
        return ResponseEntity.ok(servicio.listar());
    }

    @GetMapping("/personal/{id}")
    public ResponseEntity<PersonalRespuesta> obtener(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(servicio.obtener(id));
    }

    @GetMapping("/establecimientos/{id}/personal")
    public ResponseEntity<List<PersonalRespuesta>>
            listarPorEstablecimiento(@PathVariable Long id) {

        return ResponseEntity.ok(
                servicio.listarPorEstablecimiento(id)
        );
    }

    @PostMapping("/personal")
    public ResponseEntity<PersonalRespuesta> crear(
            @Valid
            @RequestBody PersonalEntrada entrada
    ) {
        PersonalRespuesta creado = servicio.crear(entrada);

        URI ubicacion = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/personal/{id}")
                .buildAndExpand(creado.id())
                .toUri();

        return ResponseEntity
                .created(ubicacion)
                .body(creado);
    }

    @PutMapping("/personal/{id}")
    public ResponseEntity<PersonalRespuesta> actualizar(
            @PathVariable Long id,
            @Valid
            @RequestBody PersonalEntrada entrada
    ) {
        return ResponseEntity.ok(
                servicio.actualizar(id, entrada)
        );
    }

    @DeleteMapping("/personal/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id
    ) {
        servicio.eliminar(id);

        return ResponseEntity.noContent().build();
    }
}
