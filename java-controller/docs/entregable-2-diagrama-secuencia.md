# Entregable 2 — Diagrama de secuencia: Registro de un monitor (RF1)

> Componente: **Controlador (Java)** — Spring Boot 3, Java 17
> Ruta modelada: `POST /api/monitores`
> Referencia de código: `src/main/java/com/ucaldas/monitores_controller/controller/MonitorController.java`

## Diagrama de secuencia

```mermaid
sequenceDiagram
    participant U as Usuario
    participant I as Interfaz (HTML/JS)
    participant J as Controlador (Java)
    participant P as Servicio (PHP)
    participant Y as Servicio (Python)
    participant B as Base de Datos

    U->>I: Llena el formulario y pulsa "Registrar"
    I->>J: POST /api/monitores<br/>{codigo, nombre_completo, correo, materia_id}
    activate J

    alt RF6: campos vacios, materia_id invalido o correo sin @
        J-->>I: 400 Bad Request<br/>{"error": "mensaje descriptivo"}
        Note over J: La validacion ocurre ANTES de invocar PHP.<br/>No se genera trafico a las capas inferiores.
        I-->>U: Muestra el error en el formulario
    else RF6: datos validos
        J->>P: POST http://localhost:8000/api/monitores<br/>JSON reenviado (misma estructura)
        activate P
        P->>Y: POST /api/monitores<br/>JSON del monitor
        activate Y

        alt RF6: el codigo ya existe (Python)
            Y->>B: SELECT ... WHERE codigo = ?
            activate B
            B-->>Y: Registro existente
            deactivate B
            Y-->>P: 409 Conflict<br/>{"error": "Codigo duplicado"}
            P-->>J: 409 Conflict<br/>{"error": "Codigo duplicado"}
            J-->>I: 409 Conflict<br/>{"error": "Codigo duplicado"}
            Note over J: El Controlador solo propaga el status<br/>y el cuerpo JSON y no lo convierte en 500.
            I-->>U: Muestra "Codigo duplicado"
        else RF6: el codigo no existe (Python)
            Y->>B: INSERT INTO monitores (...)<br/>{codigo, nombre_completo, correo, materia_id}
            activate B
            B-->>Y: Registro insertado
            deactivate B
            Y-->>P: 201 Created<br/>{codigo, nombre_completo, correo, materia_id, materia_nombre}
            P-->>J: 201 Created<br/>{mismo JSON}
            J-->>I: 201 Created<br/>{mismo JSON}
            Note over J: Se propaga el mismo status (201)<br/>y el cuerpo JSON sin transformarlo.
            I-->>U: Actualiza la vista: muestra el monitor registrado
        end
        deactivate Y
        deactivate P
    end
    deactivate J
```

## Responsabilidad de cada capa en el flujo

| Capa | Responsabilidad durante el registro (RF1 + RF6) |
|---|---|
| **Usuario** | Digita los datos del monitor en el formulario y dispara el registro; recibe el resultado (éxito o mensaje de error). |
| **Interfaz (HTML/JS)** | Construye el JSON de la petición (`{"codigo","nombre_completo","correo","materia_id"}`), envía `POST /api/monitores` al Controlador (puerto 8080) y refleja en pantalla el `status` y el `{"error": ...}` o el monitor creado. |
| **Controlador (Java)** | Expone `POST /api/monitores`, aplica la validación RF6 en memoria (campos vacíos, `correo` con `@`, `materia_id > 0`) y, si falla, responde `400` **sin llamar a PHP**. Si es válido, reenvía la petición por HTTP (`HttpClient`) al intermediario PHP y propaga tal cual el `status` y el cuerpo JSON (incluidos `409` y `201`). No toca Python ni la base de datos. |
| **Servicio (PHP)** | Intermediario: recibe la petición del Controlador, la enruta a la capa de persistencia (Python) y devuelve el mismo `status` y JSON, sin alterar la respuesta. |
| **Servicio (Python)** | Capa de persistencia: consulta la existencia del `codigo`; si existe responde `409 Conflict` con `{"error": "Código duplicado"}`, si no existe inserta el registro y responde `201 Created` con el monitor completo (incluido `materia_nombre`). |
| **Base de Datos** | Ejecuta la consulta `SELECT` por `codigo` y el `INSERT` del nuevo monitor; devuelve el resultado a Python. |
