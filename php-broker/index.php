<?php

require_once __DIR__ . '/config.php';

// Responder siempre en formato JSON
header('Content-Type: application/json; charset=utf-8');

// Obtener método HTTP y ruta solicitada
$metodo = $_SERVER['REQUEST_METHOD'];
$ruta = parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH);

// Definir los métodos permitidos por ruta
$metodosPermitidos = [];

if ($ruta === '/api/materias') {
    $metodosPermitidos = ['GET'];

} elseif ($ruta === '/api/monitores') {
    $metodosPermitidos = ['GET', 'POST'];

} elseif (preg_match('#^/api/monitores/[^/]+$#', $ruta)) {
    $metodosPermitidos = ['GET', 'PUT', 'DELETE'];
}

// Validar el método HTTP
if (!in_array($metodo, $metodosPermitidos, true)) {
    header('Allow: ' . implode(', ', $metodosPermitidos));
    http_response_code(405);

    echo json_encode([
        'error' => 'Método HTTP no permitido'
    ]);

    exit;
}

// Construir la URL del servicio Python
$urlPython = PYTHON_API_URL . $ruta;

// Conservar los parámetros de consulta
if (!empty($_SERVER['QUERY_STRING'])) {
    $urlPython .= '?' . $_SERVER['QUERY_STRING'];
}

// Inicializar cURL
$curl = curl_init($urlPython);

// Configurar la petición HTTP
curl_setopt_array($curl, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_CUSTOMREQUEST => $metodo,
    CURLOPT_CONNECTTIMEOUT => 5,
    CURLOPT_TIMEOUT => REQUEST_TIMEOUT,
    CURLOPT_HTTPHEADER => [
        'Content-Type: application/json',
        'Accept: application/json'
    ]
]);

// Reenviar el cuerpo JSON en POST y PUT
if (in_array($metodo, ['POST', 'PUT'], true)) {
    $cuerpo = file_get_contents('php://input');
    curl_setopt($curl, CURLOPT_POSTFIELDS, $cuerpo);
}

// Enviar solicitud a Python
$respuesta = curl_exec($curl);

// Obtener código HTTP y posibles errores
$codigoHttp = curl_getinfo($curl, CURLINFO_HTTP_CODE);
$errorCurl = curl_error($curl);

// Cerrar conexión
curl_close($curl);

// Error cuando Python no está disponible
if ($respuesta === false) {
    http_response_code(502);

    echo json_encode([
        'error' => 'No fue posible comunicarse con Python'
    ]);

    error_log('Error de conexion con Python: ' . $errorCurl);
    exit;
}

// Conservar el código de respuesta de Python
http_response_code($codigoHttp ?: 502);

// Devolver el JSON recibido
echo $respuesta;
