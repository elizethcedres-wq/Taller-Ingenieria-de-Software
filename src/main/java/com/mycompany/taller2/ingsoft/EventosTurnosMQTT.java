package com.mycompany.taller2.ingsoft;
import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
public final class EventosTurnosMQTT implements AutoCloseable {
    private final MqttClient cliente;
    private final String topic;
    private final Gson gson = new Gson();
    private final Set<Long> pendientes = ConcurrentHashMap.newKeySet();
    private static final class Evento {
        private Long reservaId;

        private Evento(Long reservaId) {
            this.reservaId = reservaId;
        }

        private Long reservaId() {
            return reservaId;
        }
    }
    public EventosTurnosMQTT(String id, String topic) throws MqttException {
        this.topic = topic;
        String broker = System.getenv("BROKER_URL");
        if (broker == null || broker.isBlank()) {
            broker = "tcp://127.0.0.1:1883";
        }
        cliente = new MqttClient(broker, id, new MemoryPersistence());
        cliente.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconexion, String uri) {
                try {
                    cliente.subscribe(topic, 1);
                    System.out.println("MQTT suscrito a " + topic);
                } catch (MqttException e) {
                    System.err.println("No se pudo suscribir: " + e.getMessage());
                }
            }
            @Override
            public void connectionLost(Throwable causa) {
                System.err.println("MQTT desconectado: " + causa);
            }
            @Override
            public void messageArrived(String origen, MqttMessage mensaje) {
                try {
                    String json = new String(
                            mensaje.getPayload(), StandardCharsets.UTF_8);
                    Evento evento = gson.fromJson(json, Evento.class);
                    if (evento == null || evento.reservaId() == null
                            || evento.reservaId() <= 0) {
                        System.err.println("Evento invalido en " + origen);
                        return;
                    }
                    pendientes.add(evento.reservaId());
                    System.out.println("Evento recibido " + origen
                            + " reservaId=" + evento.reservaId());
                } catch (RuntimeException e) {
                    System.err.println("JSON invalido: " + e.getMessage());
                }
            }
            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {}
        });
        MqttConnectOptions opciones = new MqttConnectOptions();
        opciones.setCleanSession(false);
        opciones.setAutomaticReconnect(true);
        opciones.setConnectionTimeout(5);
        opciones.setKeepAliveInterval(30);
        cliente.connect(opciones);
        // Garantiza la suscripcion inicial aunque el callback tarde.
        cliente.subscribe(topic, 1);
    }
    public Set<Long> recibirPendientes() {
        Set<Long> copia = new java.util.HashSet<>();
        for (Long id : pendientes) {
            if (pendientes.remove(id)) copia.add(id);
        }
        return copia;
    }
    public void publicar(String destino, Long reservaId) throws MqttException {
        String json = gson.toJson(new Evento(reservaId));
        MqttMessage mensaje = new MqttMessage(
                json.getBytes(StandardCharsets.UTF_8));
        mensaje.setQos(1);
        mensaje.setRetained(false);
        cliente.publish(destino, mensaje);
        System.out.println("Evento publicado " + destino
                + " reservaId=" + reservaId);
    }
    @Override
    public void close() throws MqttException {
        if (cliente.isConnected()) cliente.disconnect();
        cliente.close();
    }
}
