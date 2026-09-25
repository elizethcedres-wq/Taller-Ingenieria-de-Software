/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uy.edu.ingsoft.api.controlador;


import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uy.edu.ingsoft.api.dto.MensajeRespuesta;
import uy.edu.ingsoft.api.dto.ReservaEntrada;
import uy.edu.ingsoft.api.dto.ReservaRespuesta;
import uy.edu.ingsoft.api.servicio.ReservaServicio;

@RestController
public class ReservaControlador {

    private final ReservaServicio servicio;

    public ReservaControlador(ReservaServicio servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/reservas")
    public ResponseEntity<List<ReservaRespuesta>> listar() {
        return ResponseEntity.ok(servicio.listar());
    }

    @GetMapping("/reservas/{id}")
    public ResponseEntity<ReservaRespuesta> obtener(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(servicio.obtener(id));
    }

    @GetMapping("/personal/{id}/reservas")
    public ResponseEntity<List<ReservaRespuesta>>
            listarPorPersonal(
                    @PathVariable Long id,
                    @RequestParam(required = false)
                    @DateTimeFormat(
                            iso = DateTimeFormat.ISO.DATE
                    )
                    LocalDate fecha
            ) {

        return ResponseEntity.ok(
                servicio.listarPorPersonal(id, fecha)
        );
    }

    @PostMapping("/reservas")
    public ResponseEntity<MensajeRespuesta> solicitar(
            @Valid
            @RequestBody ReservaEntrada entrada
    ) {
        return ResponseEntity
                .accepted()
                .body(servicio.solicitar(entrada));
    }

    @DeleteMapping("/reservas/{id}")
    public ResponseEntity<MensajeRespuesta> cancelar(
            @PathVariable Long id
    ) {
        return ResponseEntity
                .accepted()
                .body(servicio.cancelar(id));
    }
    
    @GetMapping("/personal/{id}/disponibilidad")
    public ResponseEntity<Map<String, Object>> disponibilidad(
            @PathVariable Long id,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime fechaHora
    ) {
        boolean disponible = servicio.estaDisponible(id, fechaHora);

        return ResponseEntity.ok(Map.of(
                "personalId", id,
                "fechaHoraTurno", fechaHora,
                "disponible", disponible
        ));
    }
}
