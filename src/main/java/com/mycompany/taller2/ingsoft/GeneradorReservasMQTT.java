package com.mycompany.taller2.ingsoft;

import java.time.LocalDateTime;
import java.util.Random;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class GeneradorReservasMQTT {

    private static final String TOPIC = "turnos/reserva";

    public static void main(String[] args) {

        try {
            // Obtiene la URL de la variable de entorno de Docker, si no existe usa localhost para pruebas locales
                String broker = System.getenv("BROKER_URL") != null 
                ? System.getenv("BROKER_URL") 
                : "tcp://127.0.0.1:1883";

                MqttClient cliente = new MqttClient(broker, MqttClient.generateClientId());

            cliente.connect();

            System.out.println("Conectado a Mosquitto");
            System.out.println("Presione Ctrl+C para detener el generador");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    System.out.println("\nApagando generador...");

                    if (cliente.isConnected()) {
                        cliente.disconnect();
                    }

                    cliente.close();

                    System.out.println("Conexión MQTT cerrada correctamente");
                    System.out.println("Generador detenido");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }));

            Random random = new Random();
            int contador = 1;

            while (true) {

                int personalId = random.nextInt(3) + 1;

                String email = "cliente" + contador + "@mail.com";
                String telefono = "09" + (100000 + random.nextInt(900000));

                int minutosDesdeApertura = random.nextInt(20) * 30;

                LocalDateTime fechaHora = LocalDateTime.now()
                .plusDays(random.nextInt(30))
                .withHour(8)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .plusMinutes(minutosDesdeApertura);

                String json = String.format("""
                    {
                      "emailSolicitante": "%s",
                      "telefonoSolicitante": "%s",
                      "personalId": %d,
                      "fechaHoraTurno": "%s"
                    }
                    """,
                    email,
                    telefono,
                    personalId,
                    fechaHora
                );

                MqttMessage mensaje = new MqttMessage(json.getBytes());
                mensaje.setQos(1);

                cliente.publish(TOPIC, mensaje);

                System.out.println("--------------------------------");
                System.out.println("Reserva #" + contador);
                System.out.println(json);

                contador++;

                Thread.sleep(10000);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}