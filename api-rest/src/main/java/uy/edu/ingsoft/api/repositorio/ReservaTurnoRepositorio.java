/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package uy.edu.ingsoft.api.repositorio;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import uy.edu.ingsoft.api.modelo.ReservaTurno;

public interface ReservaTurnoRepositorio
        extends JpaRepository<ReservaTurno, Long> {

    List<ReservaTurno> findByPersonalId(Long personalId);

    List<ReservaTurno>
            findByPersonalIdAndFechaHoraTurnoBetween(
                    Long personalId,
                    LocalDateTime inicio,
                    LocalDateTime fin
            );
}