# Sistema Distribuido de Gestión de Monitores Académicos

**Universidad de Caldas** — Arquitectura de Software

Aplicación web distribuida, multicapa y heterogénea para la administración de monitores académicos mediante servicios REST y JSON.

## Arquitectura y Puertos Locales

* **Frontend (HTML/CSS/JS):** `http://localhost:5500` (`/frontend`)
* **Controlador (Java):** `http://localhost:8080` (`/java-controller`)
* **Intermediario / Proxy (PHP):** `http://localhost:8000` (`/php-broker`)
* **Persistencia (Python - Flask):** `http://localhost:5000` (`/python-service`)
* **Base de Datos (SQLite):** `/db/schema.sql`

## Contrato de Endpoints (Todas las capas)

* `GET /api/materias` — Lista las 8 asignaturas precargadas.
* `GET /api/monitores` — Lista todos los monitores registrados.
* `GET /api/monitores/{codigo}` — Consulta un monitor por código.
* `POST /api/monitores` — Registra un monitor (`201`, `400`, `409`).
* `PUT /api/monitores/{codigo}` — Actualiza un monitor (`200`, `400`, `404`).
* `DELETE /api/monitores/{codigo}` — Elimina un monitor (`200`, `404`).

## Cómo ejecutar la capa de Persistencia (Python + BD)

```bash
cd python-service
python -m venv venv
source venv/Scripts/activate   # En PowerShell: .\venv\Scripts\Activate.ps1
pip install -r requirements.txt
python app.py


## Ejecución del intermediario PHP

El servicio PHP funciona como intermediario entre el controlador Java
y el servicio Python. Recibe peticiones HTTP, las reenvía mediante
cURL y devuelve las respuestas conservando los códigos HTTP.

### Requisitos

- PHP 8 o superior.
- Extensión cURL habilitada.
- Servicio Python ejecutándose en el puerto 5000.

### Iniciar el servicio

Desde la carpeta principal del proyecto:

```powershell
cd php-broker
php -S localhost:8000 router.php
```

Si PHP no está agregado al PATH de Windows y se utiliza XAMPP:

```powershell
& "C:\xampp\php\php.exe" -S localhost:8000 router.php
```

El servicio estará disponible en:

http://localhost:8000

Es importante ejecutar el servidor desde la carpeta `php-broker`
para que PHP encuentre correctamente el archivo `router.php`.

### Verificar la comunicación

Con Python y PHP ejecutándose, abrir una nueva terminal:

```powershell
curl.exe -i http://localhost:8000/api/materias
curl.exe -i http://localhost:8000/api/monitores
```

Ambas consultas deben devolver un código HTTP 200 y una
respuesta en formato JSON.

## Pruebas de calidad con Postman

El proyecto cuenta con una colección de Postman ubicada en:

`postman/Gestion_Monitores_QA.postman_collection.json`

Esta colección permite probar los endpoints y validar
automáticamente sus respuestas.

### Importar la colección

1. Abrir Postman.
2. Seleccionar Import.
3. Buscar el archivo JSON dentro de la carpeta `postman`.
4. Importar la colección.
5. Verificar las variables de configuración.

### Variables

- `base_url`: http://localhost:8000
- `codigo_prueba`: 202699

La variable `base_url` permite ejecutar las pruebas contra
diferentes servicios:

- Python: http://localhost:5000
- PHP: http://localhost:8000
- Java: http://localhost:8080 (cuando esté integrado)

### Organización de las pruebas

**01 - Consultas sin modificar datos**

Contiene consultas de materias y monitores, búsqueda por código
y validaciones de respuestas 404 y 405.

**02 - Validaciones de errores**

Comprueba el manejo de campos obligatorios y códigos duplicados,
mediante respuestas HTTP 400 y 409.

**03 - CRUD secuencial**

Ejecuta las operaciones de registro, consulta, actualización y
eliminación de un monitor temporal.

Esta carpeta debe ejecutarse en el orden definido. El código
temporal debe estar disponible antes de comenzar.

### Resultados de las pruebas

Se realizaron pruebas locales sobre la comunicación PHP-Python
utilizando Postman Collection Runner.

| Grupo | Aprobadas | Fallidas |
|---|---:|---:|
| Consultas | 15 | 0 |
| Validaciones de errores | 6 | 0 |
| CRUD secuencial | 18 | 0 |
| **Total** | **39** | **0** |

Las 39 validaciones automáticas se aprobaron durante las pruebas
locales del 20 de noviembre de 2026.

Estos resultados corresponden a la integración PHP-Python.
No representan todavía pruebas completas del frontend con Java.

## Estado de integración

### Componentes probados

- Base de datos SQLite y servicio Python.
- Servicio intermediario PHP.
- Comunicación PHP-Python mediante cURL.
- Operaciones CRUD y manejo de errores HTTP.
- Colección de Postman con pruebas automáticas.

### Integraciones pendientes de verificar

- Controlador Java con el intermediario PHP.
- Interfaz web con el controlador Java.
- Pruebas de funcionamiento de extremo a extremo.

## Recomendaciones

- Ejecutar Python antes de iniciar las pruebas de PHP.
- Verificar que los puertos 5000 y 8000 estén disponibles.
- No ejecutar PHP desde una carpeta diferente a `php-broker`.
- No modificar los códigos de monitores originales durante las pruebas.
- Conservar el orden de las operaciones CRUD en Postman.
- No subir entornos virtuales ni archivos temporales a GitHub.

```
