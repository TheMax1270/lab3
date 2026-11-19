-- Precios de ejemplo COP. Solo inserta ausentes, sin sobrescribir precio o stock.
INSERT INTO producto (nombre, precio, stock, version)
SELECT 'Arroz 1 kg', 4500.00, 50, 0 WHERE NOT EXISTS (SELECT 1 FROM producto WHERE nombre = 'Arroz 1 kg');
INSERT INTO producto (nombre, precio, stock, version)
SELECT 'Leche 1 L', 4200.00, 40, 0 WHERE NOT EXISTS (SELECT 1 FROM producto WHERE nombre = 'Leche 1 L');
INSERT INTO producto (nombre, precio, stock, version)
SELECT 'Huevos docena', 12000.00, 30, 0 WHERE NOT EXISTS (SELECT 1 FROM producto WHERE nombre = 'Huevos docena');
INSERT INTO producto (nombre, precio, stock, version)
SELECT 'Pan tajado 500 g', 6500.00, 25, 0 WHERE NOT EXISTS (SELECT 1 FROM producto WHERE nombre = 'Pan tajado 500 g');
INSERT INTO producto (nombre, precio, stock, version)
SELECT 'Aceite vegetal 1 L', 10500.00, 35, 0 WHERE NOT EXISTS (SELECT 1 FROM producto WHERE nombre = 'Aceite vegetal 1 L');
INSERT INTO producto (nombre, precio, stock, version)
SELECT 'Azucar 1 kg', 4800.00, 45, 0 WHERE NOT EXISTS (SELECT 1 FROM producto WHERE nombre = 'Azucar 1 kg');
INSERT INTO producto (nombre, precio, stock, version)
SELECT 'Sal 1 kg', 2200.00, 60, 0 WHERE NOT EXISTS (SELECT 1 FROM producto WHERE nombre = 'Sal 1 kg');
INSERT INTO producto (nombre, precio, stock, version)
SELECT 'Pasta 500 g', 3800.00, 40, 0 WHERE NOT EXISTS (SELECT 1 FROM producto WHERE nombre = 'Pasta 500 g');
