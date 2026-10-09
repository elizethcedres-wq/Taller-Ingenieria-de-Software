# Prueba de reservas de la Parte 4

1. Mantener Docker Desktop y los servicios del proyecto ejecutándose. El generador debe estar detenido durante la prueba.
2. En Postman, usar **Import** y seleccionar `Sistema-Reserva-Turnos-Parte4.postman_collection.json`. Si ya estaba importada, reemplazar la colección anterior.
3. Comprobar que la variable de colección `baseUrl` sea `http://localhost:8080/api`.
4. Seleccionar únicamente la carpeta **reservas** y usar **Run folder**. Mantener el orden guardado: solicitar, consultar, cancelar y listar.
5. Ejecutar una iteración. Si configuraste un límite para ejecutar scripts, permitir al menos 120 segundos.
6. Comprobar que las siete pruebas pasan y que la cancelación confirma `CANCELADO`.

Cada ejecución crea un establecimiento, un profesional y una reserva de prueba nuevos. La reserva usa una fecha dentro de tres días y su ID se guarda automáticamente en `reservaId`. Los datos quedan en la base para poder consultarlos.

La cancelación se consulta cada dos segundos y tiene un límite de 90 segundos. Si falla, revisar que `app-listener`, la API y Mosquitto estén funcionando.

También se pueden ejecutar las peticiones manualmente, en el mismo orden. La consulta por ID sirve antes y después de cancelar.

Las carpetas **personal** y **establecimientos** conservan peticiones manuales con IDs de ejemplo, incluidas operaciones de eliminación. No ejecutar toda la colección como una prueba automática; esta prueba preparada corresponde a la carpeta **reservas**.
