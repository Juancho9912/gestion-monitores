package com.ucaldas.monitores_controller.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ucaldas.monitores_controller.model.MonitorDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controlador principal del componente "Controlador Java".
 * Recibe JSON del frontend y reenvia las peticiones por HTTP al servicio PHP
 * (intermediario) configurado en ${php.service.url}.
 * NO se conecta a Python ni a bases de datos; solo proxy + validacion RF6.
 */
@RestController
@RequestMapping("/api")
public class MonitorController {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${php.service.url}")
    private String phpServiceUrl;

    @GetMapping("/materias")
    public ResponseEntity<String> listarMaterias() {
        return forward(request(HttpMethod.GET, "/api/materias", null));
    }

    @GetMapping("/monitores")
    public ResponseEntity<String> listarMonitores() {
        return forward(request(HttpMethod.GET, "/api/monitores", null));
    }

    @GetMapping("/monitores/{codigo}")
    public ResponseEntity<String> buscarMonitor(@PathVariable String codigo) {
        return forward(request(HttpMethod.GET, "/api/monitores/" + encode(codigo), null));
    }

    @PostMapping("/monitores")
    public ResponseEntity<String> registrarMonitor(@RequestBody MonitorDTO monitor) {
        String error = validar(monitor, true);
        if (error != null) {
            return badRequest(error);
        }
        return forward(request(HttpMethod.POST, "/api/monitores", monitor));
    }

    @PutMapping("/monitores/{codigo}")
    public ResponseEntity<String> actualizarMonitor(@PathVariable String codigo, @RequestBody MonitorDTO monitor) {
        monitor.setCodigo(codigo);
        String error = validar(monitor, false);
        if (error != null) {
            return badRequest(error);
        }
        return forward(request(HttpMethod.PUT, "/api/monitores/" + encode(codigo), monitor));
    }

    @DeleteMapping("/monitores/{codigo}")
    public ResponseEntity<String> eliminarMonitor(@PathVariable String codigo) {
        return forward(request(HttpMethod.DELETE, "/api/monitores/" + encode(codigo), null));
    }

    /**
     * Valida RF6. Si hay error devuelve el mensaje descriptivo, si no, null.
     * @param validarCodigo si es true tambien valida codigo (POST); en PUT el codigo viene de la ruta.
     */
    private String validar(MonitorDTO monitor, boolean validarCodigo) {
        if (validarCodigo && esVacio(monitor.getCodigo())) {
            return "El campo codigo no puede estar vacio";
        }
        if (esVacio(monitor.getNombre_completo())) {
            return "El campo nombre_completo no puede estar vacio";
        }
        if (esVacio(monitor.getCorreo())) {
            return "El campo correo no puede estar vacio";
        }
        if (!monitor.getCorreo().contains("@")) {
            return "El correo debe contener @";
        }
        if (monitor.getMateria_id() == null || monitor.getMateria_id() <= 0) {
            return "El campo materia_id debe ser mayor a 0";
        }
        return null;
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }

    private HttpRequest request(HttpMethod method, String path, MonitorDTO monitor) throws IllegalStateException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(phpServiceUrl + path));
        if (monitor != null) {
            builder.header("Content-Type", MediaType.APPLICATION_JSON_VALUE);
            builder.method(method.name(), HttpRequest.BodyPublishers.ofString(toJson(monitor), StandardCharsets.UTF_8));
        } else {
            builder.method(method.name(), HttpRequest.BodyPublishers.noBody());
        }
        return builder.build();
    }

    /**
     * Serializa solo los campos de la peticion JSON exacta
     * (sin materia_nombre, que solo aparece en respuestas GET).
     */
    private String toJson(MonitorDTO monitor) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("codigo", monitor.getCodigo());
        body.put("nombre_completo", monitor.getNombre_completo());
        body.put("correo", monitor.getCorreo());
        body.put("materia_id", monitor.getMateria_id());
        try {
            return objectMapper.writeValueAsString(body);
        } catch (IOException e) {
            throw new IllegalStateException("Error serializando JSON", e);
        }
    }

    private ResponseEntity<String> forward(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String contentType = response.headers().firstValue("Content-Type")
                    .orElse(MediaType.APPLICATION_JSON_VALUE);
            return ResponseEntity.status(response.statusCode())
                    .header("Content-Type", contentType)
                    .body(response.body());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .body("{\"error\":\"No se pudo conectar con el servicio PHP\"}");
        }
    }

    private ResponseEntity<String> badRequest(String mensaje) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body("{\"error\":\"" + escapeJson(mensaje) + "\"}");
    }

    private String escapeJson(String texto) {
        return texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String encode(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }

    private enum HttpMethod {
        GET, POST, PUT, DELETE
    }
}
