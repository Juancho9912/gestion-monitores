# Monitores Controller — Controlador Java (Spring Boot)

Componente **Controlador** de una arquitectura distribuida multicapa para la gestión de monitores académicos. Recibe peticiones JSON desde el frontend, aplica la validación de negocio (RF6) y reenvía las operaciones por HTTP al **intermediario PHP**, que a su vez orquesta la capa de persistencia (Python) y la base de datos.

---

## 1. Arquitectura general

```
Frontend (HTML/JS)  ──HTTP──▶  Controlador (Java)  ──HTTP──▶  Intermediario (PHP)  ──HTTP──▶  Persistencia (Python)  ──▶  Base de Datos
   :5500                        :8080                             :8000                          :5000
```

| Componente | Tecnología | Puerto | Rol |
|---|---|---|---|
| Frontend | HTML / JS | `5500` | Formularios y vistas del usuario |
| **Controlador** | Java 17 · Spring Boot 3 | `8080` | Fachada REST, validación RF6, proxy al intermediario |
| Intermediario | PHP | `8000` | Enruta peticiones a la capa de persistencia |
| Persistencia | Python | `5000` | Consulta/escritura en la base de datos |

### Reglas de comunicación del Controlador

- Solo se comunica por HTTP con el servicio PHP (`php.service.url = http://localhost:8000`).
- **Nunca** se conecta a Python (puerto 5000) ni a una base de datos.
- Propaga exactamente el mismo **código de estado HTTP** (200, 201, 400, 404, 409) y el **cuerpo JSON** que devuelva PHP.
- Si PHP no responde, responde `502 Bad Gateway` con `{"error": "No se pudo conectar con el servicio PHP"}`.

---

## 2. API (idéntica en Java, PHP y Python)

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/materias` | Materias para el desplegable del formulario |
| `GET` | `/api/monitores` | RF2 — listar todos |
| `GET` | `/api/monitores/{codigo}` | RF3 — buscar uno |
| `POST` | `/api/monitores` | RF1 — registrar |
| `PUT` | `/api/monitores/{codigo}` | RF4 — actualizar |
| `DELETE` | `/api/monitores/{codigo}` | RF5 — eliminar |

### Estructura JSON exacta

**Petición (POST / PUT):**

```json
{"codigo": "101", "nombre_completo": "Ana Ruiz", "correo": "ana@uni.edu.co", "materia_id": 1}
```

**Respuesta (GET):**

```json
{"codigo": "101", "nombre_completo": "Ana Ruiz", "correo": "ana@uni.edu.co", "materia_id": 1, "materia_nombre": "Arquitectura de Software"}
```

**Errores (RF6):** `{"error": "Mensaje claro"}` con HTTP `400` (campos vacíos) o `409` (código duplicado).

### Validación RF6 (realizada en Java antes de llamar a PHP)

| Regla | Respuesta |
|---|---|
| `codigo`, `nombre_completo`, `correo` no nulos ni vacíos | `400 Bad Request` + `{"error": "..."}` |
| `correo` debe contener `@` | `400 Bad Request` + `{"error": "..."}` |
| `materia_id` debe ser mayor a 0 | `400 Bad Request` + `{"error": "..."}` |
| `codigo` duplicado (detectado por Python) | `409 Conflict` propagado en cadena |

> En `PUT`, el `{codigo}` de la ruta se asigna al DTO y se validan `nombre_completo`, `correo` y `materia_id`.

---

## 3. Estructura del proyecto

```
monitores-controller/
├── pom.xml                          # Maven (build principal)
├── docs/
│   └── entregable-2-diagrama-secuencia.md   # Diagrama de secuencia RF1
└── src/
    ├── main/java/com/ucaldas/monitores_controller/
    │   ├── MonitoresControllerApplication.java   # Punto de entrada
    │   ├── config/CorsConfig.java                # CORS global /api/**
    │   ├── controller/MonitorController.java     # Endpoints REST + proxy a PHP
    │   └── model/MonitorDTO.java                 # DTO JSON (snake_case)
    ├── main/resources/application.properties     # puerto y URL de PHP
    └── test/java/.../
        └── controller/MonitorControllerTest.java # Suite con PHP simulado
```

### Configuración (`src/main/resources/application.properties`)

```properties
spring.application.name=monitores-controller
server.port=8080
php.service.url=http://localhost:8000
```

### CORS (`config/CorsConfig.java`)

Permite cualquier origen (`*`) y los métodos `GET, POST, PUT, DELETE, OPTIONS` hacia `/api/**`.

### DTO (`model/MonitorDTO.java`)

Campos en snake_case con `@JsonProperty` para serialización con Jackson:
`codigo` (String), `nombre_completo` (String), `correo` (String), `materia_id` (Integer), `materia_nombre` (String).

### Controlador (`controller/MonitorController.java`)

- `@RestController` + `@RequestMapping("/api")`
- `java.net.http.HttpClient` para reenviar peticiones a PHP
- `ObjectMapper` para serializar solo los campos de la petición (`codigo`, `nombre_completo`, `correo`, `materia_id` — sin `materia_nombre`)
- Validación RF6 en memoria → `400` sin tocar PHP
- Propagación transparente de status/body → captura de `IOException`/`InterruptedException` → `502`

---

## 4. Requisitos

- **JDK 17+** (probado con Temurin 17.0.21)
- **Maven 3.9+**
- Los demás componentes (Frontend :5500, PHP :8000, Python :5000) son opcionales para compilar; necesarios para la integración completa.

---

## 5. Ejecución

```bash
# Compilar
mvn compile

# Ejecutar pruebas
mvn test

# Levantar el controlador (http://localhost:8080)
mvn spring-boot:run
```

> **Windows / VS Code:** si `mvn` no está en el PATH, fija las rutas en la terminal:
> ```powershell
> $env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot"
> $env:MAVEN_HOME="C:\tools\maven\apache-maven-3.9.16"
> $env:PATH="$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;"+$env:PATH
> ```
> **Git Bash:** `export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.21.9-hotspot"` y `export PATH="$JAVA_HOME/bin:/c/tools/maven/apache-maven-3.9.16/bin:$PATH"`.

También puede ejecutarse desde VS Code con la extensión **Spring Boot Extension Pack** (▶ en `MonitoresControllerApplication.java`).

---

## 6. Pruebas automatizadas

`src/test/java/.../MonitorControllerTest.java` — `@SpringBootTest` + `MockMvc` + servidor HTTP embebido (`com.sun.net.httpserver.HttpServer`) en puerto aleatorio, inyectado con `@DynamicPropertySource` a `php.service.url`.

| # | Escenario | Verificación |
|---|---|---|
| 1 | Registro inválido (nombre vacío / correo sin `@`) | `400` + JSON de error, **0** peticiones a PHP |
| 2 | Registro válido | PHP devuelve `201` → Java devuelve `201` con el mismo JSON |
| 3 | Código duplicado | `409` propagado con `{"error": "Código duplicado"}` (no se convierte en 500) |
| 4 | Consultas | `GET /api/monitores` (lista), `GET /api/monitores/101` (200) y `999` (404) |
| 5 | PUT / DELETE | Status y body propagados correctamente |
| 6 | PHP apagado | `502 Bad Gateway` con `{"error": "No se pudo conectar con el servicio PHP"}` |

Resultado: **`Tests run: 7, Failures: 0, Errors: 0`** (`mvn test` → `BUILD SUCCESS`).

---

## 7. Documentación / entregables

- **Entregable 2** — `docs/entregable-2-diagrama-secuencia.md`: diagrama de secuencia (Mermaid) del registro de un monitor (RF1) con los 6 participantes (Usuario, Interfaz, Controlador Java, Servicio PHP, Servicio Python, Base de Datos), bloques `alt` para RF6 (400 sin llegar a PHP; 409/201 propagados en cadena) y tabla de responsabilidades por capa.

---

## 8. Notas

- Los archivos `build.gradle` / `gradlew` corresponden al esqueleto inicial de Spring Initializr; **el build activo es Maven** (`pom.xml`).
- Dependencias: únicamente `spring-boot-starter-web` y `spring-boot-starter-test` (scope test).
- Cambiar una sola letra en las claves JSON rompe la integración con los demás componentes: respetar snake_case exacto.
