/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uy.edu.ingsoft.api.servicio;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.ingsoft.api.dto.PersonalEntrada;
import uy.edu.ingsoft.api.dto.PersonalRespuesta;
import uy.edu.ingsoft.api.excepcion.OperacionNoPermitidaException;
import uy.edu.ingsoft.api.excepcion.RecursoNoEncontradoException;
import uy.edu.ingsoft.api.modelo.Establecimiento;
import uy.edu.ingsoft.api.modelo.Personal;
import uy.edu.ingsoft.api.repositorio.EstablecimientoRepositorio;
import uy.edu.ingsoft.api.repositorio.PersonalRepositorio;

@Service
public class PersonalServicio {

    private final PersonalRepositorio personalRepositorio;
    private final EstablecimientoRepositorio establecimientoRepositorio;

    public PersonalServicio(
            PersonalRepositorio personalRepositorio,
            EstablecimientoRepositorio establecimientoRepositorio
    ) {
        this.personalRepositorio = personalRepositorio;
        this.establecimientoRepositorio =
                establecimientoRepositorio;
    }

    @Transactional(readOnly = true)
    public List<PersonalRespuesta> listar() {
        return personalRepositorio.findAll()
                .stream()
                .map(PersonalRespuesta::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public PersonalRespuesta obtener(Long id) {
        return PersonalRespuesta.desde(
                buscarPersonal(id)
        );
    }

    @Transactional(readOnly = true)
    public List<PersonalRespuesta> listarPorEstablecimiento(
            Long establecimientoId
    ) {
        if (!establecimientoRepositorio
                .existsById(establecimientoId)) {

            throw new RecursoNoEncontradoException(
                    "No existe un establecimiento con ID "
                    + establecimientoId
            );
        }

        return personalRepositorio
                .findByEstablecimientoId(establecimientoId)
                .stream()
                .map(PersonalRespuesta::desde)
                .toList();
    }

    @Transactional
    public PersonalRespuesta crear(PersonalEntrada entrada) {
        Establecimiento establecimiento =
                buscarEstablecimiento(
                        entrada.establecimientoId()
                );

        Personal personal = new Personal();

        copiarDatos(entrada, personal, establecimiento);

        return PersonalRespuesta.desde(
                personalRepositorio.save(personal)
        );
    }

    @Transactional
    public PersonalRespuesta actualizar(
            Long id,
            PersonalEntrada entrada
    ) {
        Personal personal = buscarPersonal(id);

        Establecimiento establecimiento =
                buscarEstablecimiento(
                        entrada.establecimientoId()
                );

        copiarDatos(entrada, personal, establecimiento);

        return PersonalRespuesta.desde(
                personalRepositorio.save(personal)
        );
    }

    @Transactional
    public void eliminar(Long id) {
        Personal personal = buscarPersonal(id);

        if (!personal.getReservas().isEmpty()) {
            throw new OperacionNoPermitidaException(
                    "No se puede eliminar el personal "
                    + id + " porque tiene reservas asociadas"
            );
        }

        personalRepositorio.delete(personal);
    }

    private Personal buscarPersonal(Long id) {
        return personalRepositorio.findById(id)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "No existe personal con ID " + id
                        )
                );
    }

    private Establecimiento buscarEstablecimiento(Long id) {
        return establecimientoRepositorio.findById(id)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "No existe un establecimiento con ID "
                                + id
                        )
                );
    }

    private void copiarDatos(
            PersonalEntrada entrada,
            Personal personal,
            Establecimiento establecimiento
    ) {
        personal.setNombre(entrada.nombre());
        personal.setEspecialidad(entrada.especialidad());
        personal.setCostoConsulta(entrada.costoConsulta());
        personal.setDuracionEstandar(
                entrada.duracionEstandar()
        );
        personal.setEstado(entrada.estado());
        personal.setEstablecimiento(establecimiento);
    }
}
