package com.mycompany.taller2.ingsoft;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class CancelarTurnoMQTT {

    private static final String TOPIC = "turnos/cancelar";

    public static void main(String[] args) {

        if (args.length == 0) {
            System.out.println("Uso:");
            System.out.println("  java ... CancelarTurnoMQTT <reservaId>");
            System.out.println();
            System.out.println("Ejemplo:");
            System.out.println("  java ... CancelarTurnoMQTT 602");
            return;
        }

        try {
            Long reservaId = Long.parseLong(args[0]);

            String broker =
                    System.getenv("BROKER_URL") != null
                            ? System.getenv("BROKER_URL")
                            : "tcp://127.0.0.1:1883";

            MqttClient cliente =
                    new MqttClient(
                            broker,
                            MqttClient.generateClientId()
                    );

            cliente.connect();

            System.out.println("Conectado a Mosquitto");

            String json = String.format("""
                {
                  "reservaId": %d
                }
                """, reservaId);

            MqttMessage mensaje =
                    new MqttMessage(json.getBytes());

            mensaje.setQos(1);

            cliente.publish(TOPIC, mensaje);

            System.out.println("--------------------------------");
            System.out.println("Solicitud de cancelación enviada");
            System.out.println("Reserva ID: " + reservaId);
            System.out.println("Topic: " + TOPIC);
            System.out.println(json);
            System.out.println("--------------------------------");

            cliente.disconnect();
            cliente.close();

            System.out.println("Conexión MQTT cerrada correctamente");

        } catch (NumberFormatException e) {
            System.out.println("Error: el ID de la reserva debe ser numérico.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
