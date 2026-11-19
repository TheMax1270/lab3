-- Esquema aditivo para labmcp; no elimina tablas ni datos existentes.
CREATE TABLE IF NOT EXISTS producto (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 nombre VARCHAR(120) NOT NULL,
 precio DECIMAL(12,2) NOT NULL,
 stock INT NOT NULL,
 version BIGINT NOT NULL,
 CONSTRAINT uk_producto_nombre UNIQUE (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS pedido (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 fecha DATETIME(6) NOT NULL,
 direccion_entrega VARCHAR(255) NOT NULL,
 estado VARCHAR(20) NOT NULL,
 total DECIMAL(14,2) NOT NULL,
 version BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS detalle_pedido (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 pedido_id BIGINT NOT NULL,
 producto_id BIGINT NOT NULL,
 cantidad INT NOT NULL,
 precio_unitario DECIMAL(12,2) NOT NULL,
 CONSTRAINT fk_detalle_pedido FOREIGN KEY (pedido_id) REFERENCES pedido(id),
 CONSTRAINT fk_detalle_producto FOREIGN KEY (producto_id) REFERENCES producto(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
