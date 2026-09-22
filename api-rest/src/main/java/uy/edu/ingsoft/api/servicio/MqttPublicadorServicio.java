/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uy.edu.ingsoft.api.servicio;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uy.edu.ingsoft.api.excepcion.ServicioNoDisponibleException;

@Service
public class MqttPublicadorServicio {

    private final String brokerUrl;
    private final ObjectMapper objectMapper;

    public MqttPublicadorServicio(
            @Value("${mqtt.broker-url}") String brokerUrl,
            ObjectMapper objectMapper
    ) {
        this.brokerUrl = brokerUrl;
        this.objectMapper = objectMapper;
    }

    public void publicar(String topic, Object contenido) {
        MqttClient cliente = null;

        try {
            String json = objectMapper.writeValueAsString(
                    contenido
            );

            cliente = new MqttClient(
                    brokerUrl,
                    MqttClient.generateClientId()
            );

            MqttConnectOptions opciones =
                    new MqttConnectOptions();

            opciones.setCleanSession(true);
            opciones.setConnectionTimeout(5);

            cliente.connect(opciones);

            MqttMessage mensaje =
                    new MqttMessage(json.getBytes());

            mensaje.setQos(1);
            mensaje.setRetained(false);

            cliente.publish(topic, mensaje);

        } catch (JsonProcessingException excepcion) {
            throw new IllegalArgumentException(
                    "No se pudo convertir la solicitud a JSON",
                    excepcion
            );

        } catch (MqttException excepcion) {
            throw new ServicioNoDisponibleException(
                    "No se pudo conectar o publicar en Mosquitto",
                    excepcion
            );

        } finally {
            cerrarCliente(cliente);
        }
    }

    private void cerrarCliente(MqttClient cliente) {
        if (cliente == null) {
            return;
        }

        try {
            if (cliente.isConnected()) {
                cliente.disconnect();
            }

            cliente.close();

        } catch (MqttException excepcion) {
            System.err.println(
                    "No se pudo cerrar el cliente MQTT: "
                    + excepcion.getMessage()
            );
        }
    }
}