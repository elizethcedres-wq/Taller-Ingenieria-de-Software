/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.taller2.ingsoft;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 *
 * @author Nicolás
 */
public class Taller2IngSoft {

    public static void main(String[] args) {
        EntityManagerFactory emf =
        Persistence.createEntityManagerFactory("turnosPU");

        EntityManager em = emf.createEntityManager();

        System.out.println("Conexión OK");

        em.close();
        emf.close();
    }
}
