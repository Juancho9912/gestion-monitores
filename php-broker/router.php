<?php

// Obtener la ruta solicitada
$ruta = parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH);

// Obtener el método HTTP
$metodo = $_SERVER['REQUEST_METHOD'];

// Rutas permitidas por el sistema
$rutasPermitidas = [
    '/api/materias',
    '/api/monitores'
];

// Verificar si la ruta es válida
$rutaValida = in_array($ruta, $rutasPermitidas, true);

// Permitir rutas con código de monitor
if (preg_match('#^/api/monitores/[^/]+$#', $ruta)) {
    $rutaValida = true;
}

// Si la ruta no existe, devolver error 404
if (!$rutaValida) {
    header('Content-Type: application/json; charset=utf-8');
    http_response_code(404);

    echo json_encode([
        'error' => 'Ruta no encontrada'
    ]);

    return true;
}

// Enviar la solicitud al intermediario PHP
require __DIR__ . '/index.php';
