Gestión de Monitores Académicos
Universidad de Caldas — Arquitectura de Software

En este taller desarrollamos una aplicación web para llevar el registro de los monitores académicos y las materias a las que están asignados. Desde la página podemos consultar los monitores, buscar uno por su código, registrar uno nuevo, modificar sus datos y eliminarlo.

El proyecto se trabajó por partes: la interfaz está hecha con HTML, CSS y JavaScript; el controlador se desarrolló en Java; PHP recibe y reenvía las solicitudes; y Python se encarga de consultar y guardar la información en SQLite. Al final conectamos las capas y probamos tanto la API como las funciones de la página.
Cómo está organizado
Navegador
   |
   v
Frontend (HTML, CSS y JavaScript)     puerto 5500
   |
   v
Controlador Java (Spring Boot)       puerto 8080
   |
   v
Intermediario PHP (cURL)             puerto 8000
   |
   v
Servicio Python (Flask)              puerto 5000
   |
   v
Base de datos SQLite
Carpeta	Qué hace
frontend/	Muestra los formularios, la tabla de monitores y los botones para consultar, editar y eliminar.
java-controller/	Recibe las peticiones que envía la interfaz y las comunica al servicio PHP.
php-broker/	Utiliza cURL para pasar las solicitudes a Python y devolver las respuestas.
python-service/	Atiende las operaciones sobre los monitores y las materias.
db/	Contiene el esquema de la base de datos SQLite.


La página se abre en http://localhost:5500 y se comunica con la API de Java en http://localhost:8080. Las capas intercambian información mediante HTTP y JSON.
Qué se necesita para ejecutarlo
- Java 17.
- Python con pip.
- PHP 8 o superior, con la extensión cURL habilitada. También sirve el PHP incluido en XAMPP.
- Un navegador web.
- Postman para volver a ejecutar las pruebas automáticas, si se desea.
El controlador ya trae el Gradle Wrapper, así que no hace falta instalar Gradle por separado.
Cómo ejecutar el proyecto en Windows
Hay que abrir cuatro terminales ubicadas inicialmente en la carpeta principal del repositorio. Las dejamos abiertas mientras utilizamos la aplicación. El orden que usamos para probarla fue Python, PHP, Java y, por último, el frontend.
1. Servicio Python y base de datos (puerto 5000)
En la primera terminal:
cd python-service
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
python app.py
Si ya se creó el entorno virtual y están instaladas las dependencias, se puede utilizar el existente. Este servicio trabaja con SQLite.
2. Intermediario PHP (puerto 8000)
En la segunda terminal:
cd php-broker
php -S localhost:8000 router.php
En nuestro equipo usamos PHP desde XAMPP. Si el comando php no funciona directamente, se puede iniciar así:
cd php-broker
& "C:\xampp\php\php.exe" -S localhost:8000 router.php
Es importante estar dentro de php-broker al iniciar el servidor. Durante las pruebas tuvimos un error por ejecutarlo desde la carpeta equivocada y PHP no encontraba router.php.

Para comprobar que responde:
curl.exe -i http://localhost:8000/api/materias
3. Controlador Java (puerto 8080)
En la tercera terminal:
cd java-controller
.\gradlew.bat bootRun
Java debe ejecutarse con JDK 17. En java-controller/src/main/resources/application.properties dejamos la dirección de PHP configurable:
spring.application.name=monitores-controller
server.port=8080
php.service.url=${PHP_SERVICE_URL:http://localhost:8000}
Normalmente basta con http://localhost:8000. En nuestro computador encontramos que PHP estaba respondiendo por IPv6 y Java no lograba conectarse con la dirección predeterminada. Para ese caso usamos esta variable antes de iniciar Java, en la misma terminal:
$env:PHP_SERVICE_URL = "http://[::1]:8000"
.\gradlew.bat bootRun
No es necesario configurar IPv6 si ambos servicios ya se comunican normalmente.

Con los tres servicios encendidos, se puede revisar la API de Java:
curl.exe -i http://localhost:8080/api/materias
curl.exe -i http://localhost:8080/api/monitores
4. Interfaz web (puerto 5500)
En la cuarta terminal:
cd frontend
python -m http.server 5500
Después abrimos http://localhost:5500 en el navegador. Allí aparece el formulario de registro y la tabla con los monitores, además de las opciones de búsqueda, edición y eliminación.
Los comandos anteriores parten de la carpeta principal del proyecto. Si la terminal ya está dentro de frontend, php-broker o alguna otra carpeta, no hay que repetir el cd.

Funciones disponibles y rutas de la API
Método	Ruta	Para qué se utiliza
GET	/api/materias	Ver las 8 materias precargadas.
GET	/api/monitores	Listar los monitores.
GET	/api/monitores/{codigo}	Buscar un monitor por su código.
POST	/api/monitores	Registrar un monitor.
PUT	/api/monitores/{codigo}	Actualizar sus datos.
DELETE	/api/monitores/{codigo}	Eliminar un monitor.


Por ejemplo, para registrar un monitor enviamos un JSON con estos campos:
{
  "codigo": "202698",
  "nombre_completo": "Monitor Prueba Frontend",
  "correo": "prueba.frontend@ucaldas.edu.co",
  "materia_id": 1
}
El materia_id debe existir en la lista de materias. Cuando consultamos los monitores, también se devuelve el nombre de la materia en el campo materia_nombre.

Además de comprobar las operaciones, revisamos las respuestas HTTP: 200 para operaciones correctas, 201 cuando se crea un registro, 400 para datos inválidos, 404 cuando no existe el registro, 405 para métodos no permitidos y 409 cuando intentamos usar un código repetido.
Pruebas con Postman
La colección que utilizamos está guardada en:

postman/Gestion_Monitores_QA.postman_collection.json

Para usarla, se importa ese archivo en Postman y se revisan estas variables:
Variable	Valor
base_url	http://localhost:8000 para probar PHP o http://localhost:8080 para probar Java.
codigo_prueba	202699


La colección está dividida en tres carpetas:
1. 01 - Consultas sin modificar datos: lista las materias y los monitores, busca por código y comprueba respuestas 404 y 405.
2. 02 - Validaciones de errores: comprueba qué ocurre si faltan campos o se intenta registrar un código que ya existe.
3. 03 - CRUD secuencial (ejecutar en orden): crea un monitor de prueba, lo consulta, lo actualiza, lo elimina y verifica que ya no aparezca.
La tercera carpeta debe ejecutarse en el orden indicado. También hay que asegurarse de que 202699 no exista antes de comenzar otra ejecución.
Resultados que obtuvimos
Primero ejecutamos las pruebas contra PHP (puerto 8000) y después repetimos la misma colección pasando por el controlador Java (puerto 8080).
Grupo de pruebas	Desde PHP	Desde Java
Consultas	15/15	15/15
Validaciones de errores	6/6	6/6
CRUD secuencial	18/18	18/18
Total	39/39	39/39


En total aprobamos 78 validaciones y no hubo pruebas fallidas. Son dos ejecuciones de la colección: 39 contra PHP y 39 contra Java.

También probamos la interfaz en el navegador. Comprobamos que cargaran las materias y los monitores y que funcionaran la búsqueda, el registro, la edición y la eliminación. Esas operaciones se hicieron desde la página, no solo desde Postman.
Problemas que resolvimos
Durante la integración no todo funcionó al primer intento. Estos fueron los inconvenientes que encontramos y cómo los corregimos:
- Java no compilaba correctamente: agregamos la dependencia Jackson en build.gradle, que necesitaba el controlador para trabajar con JSON.
- Una prueba de Java tenía un problema de importación: actualizamos AutoConfigureMockMvc para la versión de Spring Boot utilizada.
- La configuración pedía Java 17: instalamos y usamos esa versión para ejecutar Gradle y el controlador.
- Java no lograba comunicarse con PHP: comprobamos que PHP estaba escuchando por IPv6 en nuestro equipo. Dejamos PHP_SERVICE_URL como variable configurable y así pudimos hacer la conexión.
- PHP no encontraba router.php: estábamos iniciando el servidor desde otra carpeta. Lo solucionamos ejecutándolo dentro de php-broker.
Después de estas correcciones repetimos las pruebas contra Java y pasaron las 39 validaciones.
Archivos que sirven para revisar el trabajo
- Diagrama de secuencia del controlador Java, elaborado para el entregable de Pablo: java-controller/docs/entregable-2-diagrama-secuencia.md.
- Colección de Postman: postman/Gestion_Monitores_QA.postman_collection.json.
- Código de la interfaz: frontend/.
- Controlador Java: java-controller/.
- Intermediario PHP: php-broker/.
- Servicio Python y esquema de base de datos: python-service/ y db/.
Recomendaciones al volver a ejecutarlo
- Iniciar los servicios en el orden indicado y comprobar que estén libres los puertos 5000, 8000, 8080 y 5500.
- Mantener las cuatro terminales abiertas mientras se usa la aplicación.
- Para las pruebas CRUD, utilizar un código temporal y no modificar los monitores precargados.
- No subir al repositorio entornos virtuales, archivos temporales ni resultados de compilación.
Resultado del taller
Conseguimos conectar la interfaz con Java, PHP, Python y SQLite. Probamos el CRUD desde la página y confirmamos las respuestas de la API mediante Postman. El resultado fue de 78 validaciones aprobadas, además de las comprobaciones manuales en el navegador. El sistema quedó funcionando con las capas que planteamos para el taller.