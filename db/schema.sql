-- Creación de tabla de materias
CREATE TABLE IF NOT EXISTS materias (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre VARCHAR(100) NOT NULL UNIQUE
);

-- Creación de tabla de monitores
CREATE TABLE IF NOT EXISTS monitores (
    codigo VARCHAR(20) PRIMARY KEY,
    nombre_completo VARCHAR(150) NOT NULL,
    correo VARCHAR(100) NOT NULL,
    materia_id INTEGER NOT NULL,
    FOREIGN KEY (materia_id) REFERENCES materias(id)
);

-- Materias precargadas (8 asignaturas de Ingeniería de Sistemas)
INSERT INTO materias (id, nombre) VALUES 
(1, 'Arquitectura de Software'),
(2, 'Ingeniería de Software III'),
(3, 'Bases de Datos II'),
(4, 'Programación Concurrente y Distribuida'),
(5, 'Redes y Comunicaciones'),
(6, 'Estructuras de Datos'),
(7, 'Sistemas Operativos'),
(8, 'Inteligencia Artificial');

-- Monitores precargados de ejemplo para demostración inicial
INSERT INTO monitores (codigo, nombre_completo, correo, materia_id) VALUES 
('202601', 'Camila Andrea Gómez', 'camila.gomez@ucaldas.edu.co', 1),
('202602', 'Sebastián Restrepo Toro', 'sebastian.restrepo@ucaldas.edu.co', 3),
('202603', 'Valentina Ríos Cardona', 'valentina.rios@ucaldas.edu.co', 4),
('202604', 'Mateo López Salazar', 'mateo.lopez@ucaldas.edu.co', 6);