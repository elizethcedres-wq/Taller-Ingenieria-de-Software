package com.mycompany.taller2.ingsoft;

import java.time.LocalTime;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import logica.Establecimiento;
import logica.Personal;

public class CargoDatosPrueba {

    public static void main(String[] args) {

        EntityManagerFactory emf =
                Persistence.createEntityManagerFactory("turnosPU");

        EntityManager em = emf.createEntityManager();

        try {
            cargarSiEsNecesario(em);
            System.out.println("Datos cargados correctamente.");

        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            if (em.isOpen()) {
                em.close();
            }

            if (emf.isOpen()) {
                emf.close();
            }
        }
    }

    public static void cargarSiEsNecesario(EntityManager em) {

        Long cantidadPersonal =
                em.createQuery(
                        "SELECT COUNT(p) FROM Personal p",
                        Long.class
                ).getSingleResult();

        if (cantidadPersonal > 0) {
            System.out.println(
                    "La base ya contiene personal. No se cargan datos de prueba."
            );
            return;
        }

        EntityManager currentEm = em;

        currentEm.getTransaction().begin();

        // ==========================================
        // ESTABLECIMIENTO 1 + PERSONAL 1
        // ==========================================

        Establecimiento est1 = new Establecimiento(
                "Clinica Adyacente",
                "18 de Julio 1235",
                "24001238",
                "info@clinica.es",
                LocalTime.of(8, 0),
                LocalTime.of(18, 0)
        );

        currentEm.persist(est1);

        Personal p1 = new Personal(
                "Juan Perez",
                "Cardiologia",
                1500,
                30,
                true
        );

        p1.setEstablecimiento(est1);
        currentEm.persist(p1);

        // ==========================================
        // ESTABLECIMIENTO 2 + PERSONAL 2
        // ==========================================

        Establecimiento est2 = new Establecimiento(
                "Central",
                "18 Julio 1235",
                "24001234",
                "info@clinica.com",
                LocalTime.of(8, 0),
                LocalTime.of(18, 0)
        );

        currentEm.persist(est2);

        Personal p2 = new Personal(
                "Juan",
                "Cardiologia",
                1500,
                30,
                true
        );

        p2.setEstablecimiento(est2);
        currentEm.persist(p2);

        // ==========================================
        // ESTABLECIMIENTO 3 + PERSONAL 3
        // ==========================================

        Establecimiento est3 = new Establecimiento(
                "Clinica asdf",
                "18 de Agosto 1235",
                "24111234",
                "info@agosto.com",
                LocalTime.of(8, 0),
                LocalTime.of(18, 0)
        );

        currentEm.persist(est3);

        Personal p3 = new Personal(
                "juANJO Perez",
                "Cardiologia",
                1530,
                30,
                true
        );

        p3.setEstablecimiento(est3);
        currentEm.persist(p3);

        currentEm.getTransaction().commit();

        System.out.println(
                "Datos iniciales de prueba cargados."
        );
    }
}
