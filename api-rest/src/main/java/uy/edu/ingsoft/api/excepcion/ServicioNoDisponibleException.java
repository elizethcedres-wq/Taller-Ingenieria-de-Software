/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uy.edu.ingsoft.api.excepcion;

public class ServicioNoDisponibleException
        extends RuntimeException {

    public ServicioNoDisponibleException(String mensaje,Throwable causa) {
        super(mensaje, causa);
    }
}
