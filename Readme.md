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
```
