/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package uy.edu.ingsoft.api.repositorio;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import uy.edu.ingsoft.api.modelo.Personal;

public interface PersonalRepositorio
        extends JpaRepository<Personal, Long> {

    List<Personal> findByEstablecimientoId(Long establecimientoId);
}