/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uy.edu.ingsoft.api.servicio;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.ingsoft.api.dto.EstablecimientoEntrada;
import uy.edu.ingsoft.api.dto.EstablecimientoRespuesta;
import uy.edu.ingsoft.api.excepcion.OperacionNoPermitidaException;
import uy.edu.ingsoft.api.excepcion.RecursoNoEncontradoException;
import uy.edu.ingsoft.api.modelo.Establecimiento;
import uy.edu.ingsoft.api.repositorio.EstablecimientoRepositorio;

@Service
public class EstablecimientoServicio {

    private final EstablecimientoRepositorio repositorio;

    public EstablecimientoServicio(
            EstablecimientoRepositorio repositorio
    ) {
        this.repositorio = repositorio;
    }

    @Transactional(readOnly = true)
    public List<EstablecimientoRespuesta> listar() {
        return repositorio.findAll()
                .stream()
                .map(EstablecimientoRespuesta::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public EstablecimientoRespuesta obtener(Long id) {
        return EstablecimientoRespuesta.desde(
                buscarEntidad(id)
        );
    }

    @Transactional
    public EstablecimientoRespuesta crear(
            EstablecimientoEntrada entrada
    ) {
        validarHorarios(entrada);

        Establecimiento establecimiento = new Establecimiento();

        copiarDatos(entrada, establecimiento);

        Establecimiento guardado =
                repositorio.save(establecimiento);

        return EstablecimientoRespuesta.desde(guardado);
    }

    @Transactional
    public EstablecimientoRespuesta actualizar(
            Long id,
            EstablecimientoEntrada entrada
    ) {
        validarHorarios(entrada);

        Establecimiento establecimiento = buscarEntidad(id);

        copiarDatos(entrada, establecimiento);

        Establecimiento actualizado =
                repositorio.save(establecimiento);

        return EstablecimientoRespuesta.desde(actualizado);
    }

    @Transactional
    public void eliminar(Long id) {
        Establecimiento establecimiento = buscarEntidad(id);

        if (!establecimiento.getTrabajadores().isEmpty()) {
            throw new OperacionNoPermitidaException(
                    "No se puede eliminar el establecimiento "
                    + id + " porque tiene personal asociado"
            );
        }

        repositorio.delete(establecimiento);
    }

    private Establecimiento buscarEntidad(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "No existe un establecimiento con ID "
                                + id
                        )
                );
    }

    private void copiarDatos(
            EstablecimientoEntrada entrada,
            Establecimiento establecimiento
    ) {
        establecimiento.setNombreComercial(
                entrada.nombreComercial()
        );
        establecimiento.setDireccion(
                entrada.direccion()
        );
        establecimiento.setTelefono(
                entrada.telefono()
        );
        establecimiento.setCorreo(
                entrada.correo()
        );
        establecimiento.setHorarioApertura(
                entrada.horarioApertura()
        );
        establecimiento.setHorarioCierre(
                entrada.horarioCierre()
        );
    }

    private void validarHorarios(
            EstablecimientoEntrada entrada
    ) {
        if (!entrada.horarioApertura()
                .isBefore(entrada.horarioCierre())) {

            throw new IllegalArgumentException(
                    "El horario de apertura debe ser "
                    + "anterior al horario de cierre"
            );
        }
    }
}