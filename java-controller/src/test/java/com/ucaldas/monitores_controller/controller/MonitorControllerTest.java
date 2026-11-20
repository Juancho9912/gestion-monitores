package com.ucaldas.monitores_controller.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Suite de pruebas del controlador REST con un servidor HTTP emulando al
 * servicio PHP (intermediario) en un puerto de prueba.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MonitorControllerTest {

    private static final String VALID_MONITOR_JSON =
            "{\"codigo\":\"101\",\"nombre_completo\":\"Ana Ruiz\",\"correo\":\"ana@uni.edu.co\","
                    + "\"materia_id\":1,\"materia_nombre\":\"Arquitectura de Software\"}";

    private static final String UPDATED_MONITOR_JSON =
            "{\"codigo\":\"101\",\"nombre_completo\":\"Ana Ruiz\",\"correo\":\"ana@uni.edu.co\","
                    + "\"materia_id\":2,\"materia_nombre\":\"Arquitectura de Software\"}";

    private static final String DUPLICATE_ERROR_JSON = "{\"error\":\"Código duplicado\"}";

    private static final String PHP_DOWN_ERROR_JSON =
            "{\"error\":\"No se pudo conectar con el servicio PHP\"}";

    private static HttpServer phpServer;
    private static int phpPort;
    private static final AtomicInteger requestCount = new AtomicInteger(0);

    @Autowired
    private MockMvc mockMvc;

    @BeforeAll
    static void startPhpServer() throws IOException {
        phpServer = HttpServer.create(new InetSocketAddress(0), 0);
        phpPort = phpServer.getAddress().getPort();
        phpServer.createContext("/", new FakePhpHandler());
        phpServer.start();
    }

    @AfterAll
    static void stopPhpServer() {
        if (phpServer != null) {
            phpServer.stop(0);
        }
    }

    @DynamicPropertySource
    static void phpServiceUrl(DynamicPropertyRegistry registry) {
        registry.add("php.service.url", () -> "http://localhost:" + phpPort);
    }

    private void resetRequestCount() {
        requestCount.set(0);
    }

    // 1. Registro invalido: 400 sin llegar a PHP
    @Test
    @Order(1)
    void postInvalidoRetorna400SinLlamarPhp() throws Exception {
        resetRequestCount();

        mockMvc.perform(post("/api/monitores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"101\",\"nombre_completo\":\"\","
                                + "\"correo\":\"ana@uni.edu.co\",\"materia_id\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());

        mockMvc.perform(post("/api/monitores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"101\",\"nombre_completo\":\"Ana Ruiz\","
                                + "\"correo\":\"anauni.edu.co\",\"materia_id\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());

        org.junit.jupiter.api.Assertions.assertEquals(0, requestCount.get(),
                "No debe hacer peticiones al servidor PHP simulado si la validacion falla");
    }

    // 2. Registro valido: PHP devuelve 201 y Java propaga exactamente 201 + mismo JSON
    @Test
    @Order(2)
    void postValidoPropaga201YBody() throws Exception {
        resetRequestCount();

        mockMvc.perform(post("/api/monitores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"202\",\"nombre_completo\":\"Ana Ruiz\","
                                + "\"correo\":\"ana@uni.edu.co\",\"materia_id\":1}"))
                .andExpect(status().isCreated())
                .andExpect(content().string(VALID_MONITOR_JSON));

        org.junit.jupiter.api.Assertions.assertEquals(1, requestCount.get());
    }

    // 3. Codigo duplicado: PHP devuelve 409 y Java lo propaga sin convertirlo en 500
    @Test
    @Order(3)
    void postDuplicadoPropaga409() throws Exception {
        resetRequestCount();

        mockMvc.perform(post("/api/monitores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"101\",\"nombre_completo\":\"Ana Ruiz\","
                                + "\"correo\":\"ana@uni.edu.co\",\"materia_id\":1}"))
                .andExpect(status().isConflict())
                .andExpect(content().string(DUPLICATE_ERROR_JSON));

        org.junit.jupiter.api.Assertions.assertEquals(1, requestCount.get());
    }

    // 4. Consulta general y por codigo (200 / 404)
    @Test
    @Order(4)
    void getMonitoresListaYPorCodigo() throws Exception {
        resetRequestCount();

        mockMvc.perform(get("/api/monitores"))
                .andExpect(status().isOk())
                .andExpect(content().string("[" + VALID_MONITOR_JSON + "]"));

        mockMvc.perform(get("/api/monitores/101"))
                .andExpect(status().isOk())
                .andExpect(content().string(VALID_MONITOR_JSON));

        mockMvc.perform(get("/api/monitores/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").isNotEmpty());

        org.junit.jupiter.api.Assertions.assertEquals(3, requestCount.get());
    }

    // 5. Actualizacion y eliminacion
    @Test
    @Order(5)
    void putYDeletePropaganStatusYBody() throws Exception {
        resetRequestCount();

        mockMvc.perform(put("/api/monitores/101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre_completo\":\"Ana Ruiz\","
                                + "\"correo\":\"ana@uni.edu.co\",\"materia_id\":2}"))
                .andExpect(status().isOk())
                .andExpect(content().string(UPDATED_MONITOR_JSON));

        mockMvc.perform(delete("/api/monitores/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Monitor eliminado"));

        org.junit.jupiter.api.Assertions.assertEquals(2, requestCount.get());
    }

    // 6. PHP caido: 502 con JSON de error
    @Test
    @Order(6)
    void phpApagadoRetorna502() throws Exception {
        phpServer.stop(0);

        mockMvc.perform(get("/api/monitores"))
                .andExpect(status().isBadGateway())
                .andExpect(content().string(PHP_DOWN_ERROR_JSON));
    }

    /**
     * Servidor PHP simulado: responde segun metodo y ruta, registrando
     * cuantas peticiones llegan.
     */
    private static class FakePhpHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            requestCount.incrementAndGet();

            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            String body = readBody(exchange);

            switch (method) {
                case "GET":
                    handleGet(path, exchange);
                    break;
                case "POST":
                    handlePost(body, exchange);
                    break;
                case "PUT":
                    handlePut(path, exchange);
                    break;
                case "DELETE":
                    handleDelete(path, exchange);
                    break;
                default:
                    respond(exchange, 405, "{\"error\":\"Metodo no soportado\"}");
            }
        }

        private void handleGet(String path, HttpExchange exchange) throws IOException {
            if ("/api/materias".equals(path)) {
                respond(exchange, 200, "[{\"id\":1,\"nombre\":\"Arquitectura de Software\"}]");
            } else if ("/api/monitores".equals(path)) {
                respond(exchange, 200, "[" + VALID_MONITOR_JSON + "]");
            } else if ("/api/monitores/101".equals(path)) {
                respond(exchange, 200, VALID_MONITOR_JSON);
            } else {
                respond(exchange, 404, "{\"error\":\"Monitor no encontrado\"}");
            }
        }

        private void handlePost(String body, HttpExchange exchange) throws IOException {
            if (body != null && body.contains("\"codigo\":\"101\"")) {
                respond(exchange, 409, DUPLICATE_ERROR_JSON);
            } else {
                respond(exchange, 201, VALID_MONITOR_JSON);
            }
        }

        private void handlePut(String path, HttpExchange exchange) throws IOException {
            if ("/api/monitores/101".equals(path)) {
                respond(exchange, 200, UPDATED_MONITOR_JSON);
            } else {
                respond(exchange, 404, "{\"error\":\"Monitor no encontrado\"}");
            }
        }

        private void handleDelete(String path, HttpExchange exchange) throws IOException {
            if ("/api/monitores/101".equals(path)) {
                respond(exchange, 200, "{\"message\":\"Monitor eliminado\"}");
            } else {
                respond(exchange, 404, "{\"error\":\"Monitor no encontrado\"}");
            }
        }

        private String readBody(HttpExchange exchange) throws IOException {
            try (InputStream in = exchange.getRequestBody()) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        }

        private void respond(HttpExchange exchange, int status, String json) throws IOException {
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        }
    }
}
