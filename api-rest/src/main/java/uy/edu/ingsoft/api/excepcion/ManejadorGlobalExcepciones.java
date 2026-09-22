/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uy.edu.ingsoft.api.excepcion;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import uy.edu.ingsoft.api.dto.ErrorRespuesta;

@RestControllerAdvice
public class ManejadorGlobalExcepciones {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorRespuesta> manejarNoEncontrado(
            RecursoNoEncontradoException excepcion,
            HttpServletRequest solicitud
    ) {
        return construirRespuesta(
                HttpStatus.NOT_FOUND,
                excepcion.getMessage(),
                solicitud
        );
    }

    @ExceptionHandler(OperacionNoPermitidaException.class)
    public ResponseEntity<ErrorRespuesta> manejarConflicto(
            OperacionNoPermitidaException excepcion,
            HttpServletRequest solicitud
    ) {
        return construirRespuesta(
                HttpStatus.CONFLICT,
                excepcion.getMessage(),
                solicitud
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorRespuesta> manejarArgumentoInvalido(
            IllegalArgumentException excepcion,
            HttpServletRequest solicitud
    ) {
        return construirRespuesta(
                HttpStatus.BAD_REQUEST,
                excepcion.getMessage(),
                solicitud
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespuesta> manejarValidacion(
            MethodArgumentNotValidException excepcion,
            HttpServletRequest solicitud
    ) {
        String mensaje = excepcion.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error ->
                        error.getField()
                        + ": "
                        + error.getDefaultMessage()
                )
                .collect(Collectors.joining("; "));

        return construirRespuesta(
                HttpStatus.BAD_REQUEST,
                mensaje,
                solicitud
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRespuesta> manejarErrorInterno(
            Exception excepcion,
            HttpServletRequest solicitud
    ) {
        excepcion.printStackTrace();

        return construirRespuesta(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error interno en el servidor",
                solicitud
        );
    }

    private ResponseEntity<ErrorRespuesta> construirRespuesta(
            HttpStatus estado,
            String mensaje,
            HttpServletRequest solicitud
    ) {
        ErrorRespuesta error = new ErrorRespuesta(
                LocalDateTime.now(),
                estado.value(),
                estado.getReasonPhrase(),
                mensaje,
                solicitud.getRequestURI()
        );

        return ResponseEntity
                .status(estado)
                .body(error);
    }
    
    @ExceptionHandler(ServicioNoDisponibleException.class)
    public ResponseEntity<ErrorRespuesta> manejarServicioNoDisponible(
        ServicioNoDisponibleException excepcion,
        HttpServletRequest solicitud) 
    {
    return construirRespuesta(
            HttpStatus.SERVICE_UNAVAILABLE,
            excepcion.getMessage(),
            solicitud
        );
    }
    
    
    
    
}