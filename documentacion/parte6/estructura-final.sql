
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
DROP TABLE IF EXISTS `CLIENTE`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `CLIENTE` (
  `ID` bigint NOT NULL AUTO_INCREMENT,
  `EMAIL` varchar(255) NOT NULL,
  `TELEFONO` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `EMAIL` (`EMAIL`),
  UNIQUE KEY `UNQ_CLIENTE_0` (`EMAIL`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `ESTABLECIMIENTO`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ESTABLECIMIENTO` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `correo` varchar(255) DEFAULT NULL,
  `direccion` varchar(255) DEFAULT NULL,
  `horarioApertura` time(6) DEFAULT NULL,
  `horarioCierre` time(6) DEFAULT NULL,
  `nombreComercial` varchar(255) DEFAULT NULL,
  `telefono` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `FACTURA`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `FACTURA` (
  `ID` bigint NOT NULL AUTO_INCREMENT,
  `ANIO` int DEFAULT NULL,
  `FECHAEMISION` date DEFAULT NULL,
  `MES` int DEFAULT NULL,
  `TOTAL` decimal(12,2) NOT NULL,
  `cliente_id` bigint NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UNQ_FACTURA_0` (`cliente_id`,`ANIO`,`MES`),
  CONSTRAINT `FK_FACTURA_cliente_id` FOREIGN KEY (`cliente_id`) REFERENCES `CLIENTE` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `ITEMFACTURA`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ITEMFACTURA` (
  `ID` bigint NOT NULL AUTO_INCREMENT,
  `DESCRIPCION` varchar(255) NOT NULL,
  `IMPORTE` decimal(12,2) NOT NULL,
  `factura_id` bigint NOT NULL,
  `reserva_id` bigint NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `reserva_id` (`reserva_id`),
  UNIQUE KEY `UNQ_ITEMFACTURA_0` (`reserva_id`),
  KEY `FK_ITEMFACTURA_factura_id` (`factura_id`),
  CONSTRAINT `FK_ITEMFACTURA_factura_id` FOREIGN KEY (`factura_id`) REFERENCES `FACTURA` (`ID`),
  CONSTRAINT `FK_ITEMFACTURA_reserva_id` FOREIGN KEY (`reserva_id`) REFERENCES `RESERVATURNO` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `PERSONAL`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `PERSONAL` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `costoConsulta` int NOT NULL,
  `duracionEstandar` int NOT NULL,
  `especialidad` varchar(255) DEFAULT NULL,
  `estado` bit(1) DEFAULT NULL,
  `nombre` varchar(255) DEFAULT NULL,
  `establecimiento_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_PERSONAL_establecimiento_id` (`establecimiento_id`),
  CONSTRAINT `FK_PERSONAL_establecimiento_id` FOREIGN KEY (`establecimiento_id`) REFERENCES `ESTABLECIMIENTO` (`id`),
  CONSTRAINT `FKy9hsumt1rk8d21iglqw10hmd` FOREIGN KEY (`establecimiento_id`) REFERENCES `ESTABLECIMIENTO` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `RESERVATURNO`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `RESERVATURNO` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `emailSolicitante` varchar(255) DEFAULT NULL,
  `establecimiento_solicitado_id` bigint DEFAULT NULL,
  `estado` enum('AGENDADO','ATENDIDO','CANCELADO','FACTURADO','RECHAZADO_SOLICITUD_NO_VALIDA','RECHAZADO_TURNO_OCUPADO','SOLICITADO') DEFAULT NULL,
  `fechaHoraTurno` datetime(6) DEFAULT NULL,
  `fechaReserva` date DEFAULT NULL,
  `personal_solicitado_id` bigint DEFAULT NULL,
  `telefonoSolicitante` varchar(255) DEFAULT NULL,
  `personal_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_RESERVATURNO_personal_id` (`personal_id`),
  CONSTRAINT `FK_RESERVATURNO_personal_id` FOREIGN KEY (`personal_id`) REFERENCES `PERSONAL` (`id`),
  CONSTRAINT `FKdcdqkxewx44bdw4eeuh3s9nd7` FOREIGN KEY (`personal_id`) REFERENCES `PERSONAL` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

