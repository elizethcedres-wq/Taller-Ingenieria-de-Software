/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uy.edu.ingsoft.api.servicio;

import java.time.LocalTime;
import uy.edu.ingsoft.api.modelo.Personal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.ingsoft.api.dto.CancelarReservaMensaje;
import uy.edu.ingsoft.api.dto.MensajeRespuesta;
import uy.edu.ingsoft.api.dto.ReservaEntrada;
import uy.edu.ingsoft.api.dto.ReservaRespuesta;
import uy.edu.ingsoft.api.excepcion.RecursoNoEncontradoException;
import uy.edu.ingsoft.api.repositorio.PersonalRepositorio;
import uy.edu.ingsoft.api.repositorio.ReservaTurnoRepositorio;

@Service
public class ReservaServicio {

    private static final String TOPIC_RESERVA =
            "turnos/reserva";

    private static final String TOPIC_CANCELAR =
            "turnos/cancelar";

    private final ReservaTurnoRepositorio reservaRepositorio;
    private final PersonalRepositorio personalRepositorio;
    private final MqttPublicadorServicio mqttPublicador;

    public ReservaServicio(
            ReservaTurnoRepositorio reservaRepositorio,
            PersonalRepositorio personalRepositorio,
            MqttPublicadorServicio mqttPublicador
    ) {
        this.reservaRepositorio = reservaRepositorio;
        this.personalRepositorio = personalRepositorio;
        this.mqttPublicador = mqttPublicador;
    }

    @Transactional(readOnly = true)
    public List<ReservaRespuesta> listar() {
        return reservaRepositorio.findAll()
                .stream()
                .map(ReservaRespuesta::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReservaRespuesta obtener(Long id) {
        return ReservaRespuesta.desde(
                reservaRepositorio.findById(id)
                        .orElseThrow(() ->
                                new RecursoNoEncontradoException(
                                        "No existe una reserva con ID "
                                        + id
                                )
                        )
        );
    }

    @Transactional(readOnly = true)
    public List<ReservaRespuesta> listarPorPersonal(
            Long personalId,
            LocalDate fecha
    ) {
        if (!personalRepositorio.existsById(personalId)) {
            throw new RecursoNoEncontradoException(
                    "No existe personal con ID " + personalId
            );
        }

        if (fecha == null) {
            return reservaRepositorio
                    .findByPersonalId(personalId)
                    .stream()
                    .map(ReservaRespuesta::desde)
                    .toList();
        }

        LocalDateTime inicio = fecha.atStartOfDay();

        LocalDateTime fin = fecha
                .plusDays(1)
                .atStartOfDay()
                .minusNanos(1);

        return reservaRepositorio
                .findByPersonalIdAndFechaHoraTurnoBetween(
                        personalId,
                        inicio,
                        fin
                )
                .stream()
                .map(ReservaRespuesta::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public MensajeRespuesta solicitar(
            ReservaEntrada entrada
    ) {
        if (!personalRepositorio
                .existsById(entrada.personalId())) {

            throw new RecursoNoEncontradoException(
                    "No existe personal con ID "
                    + entrada.personalId()
            );
        }

        mqttPublicador.publicar(
                TOPIC_RESERVA,
                entrada
        );

        return new MensajeRespuesta(
                "Solicitud de reserva aceptada "
                + "para procesamiento"
        );
    }

    @Transactional(readOnly = true)
    public MensajeRespuesta cancelar(Long id) {
        if (!reservaRepositorio.existsById(id)) {
            throw new RecursoNoEncontradoException(
                    "No existe una reserva con ID " + id
            );
        }

        CancelarReservaMensaje mensaje =
                new CancelarReservaMensaje(id);

        mqttPublicador.publicar(
                TOPIC_CANCELAR,
                mensaje
        );

        return new MensajeRespuesta(
                "Solicitud de cancelación aceptada "
                + "para procesamiento"
        );
    }
    
    @Transactional(readOnly = true)
    public boolean estaDisponible(Long personalId, LocalDateTime fechaHora) {
        Personal personal = personalRepositorio.findById(personalId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe personal con ID " + personalId));

        if (!Boolean.TRUE.equals(personal.getEstado())) {
            return false;
        }

        if (personal.getEstablecimiento() == null
                || personal.getDuracionEstandar() != 30) {
            return false;
        }

        LocalTime inicio = fechaHora.toLocalTime();
        LocalTime fin = inicio.plusMinutes(30);

        if (inicio.isBefore(personal.getEstablecimiento().getHorarioApertura())
                || fin.isAfter(personal.getEstablecimiento().getHorarioCierre())) {
            return false;
        }

        LocalDateTime limiteInferior = fechaHora.minusMinutes(30);
        LocalDateTime limiteSuperior = fechaHora.plusMinutes(30);

        return reservaRepositorio
                .findByPersonalIdAndFechaHoraTurnoBetween(
                        personalId, limiteInferior, limiteSuperior)
                .stream()
                .noneMatch(reserva ->
                        reserva.getFechaHoraTurno().isAfter(limiteInferior)
                        && reserva.getFechaHoraTurno().isBefore(limiteSuperior));
    }
}
