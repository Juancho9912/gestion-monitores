import sqlite3
from flask import Flask, request, jsonify

app = Flask(__name__)
DB_NAME = "monitores.db"

def get_db_connection():
    conn = sqlite3.connect(DB_NAME)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys = ON;")
    return conn

def init_db():
    conn = get_db_connection()
    cursor = conn.cursor()
    
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS materias (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            nombre TEXT NOT NULL UNIQUE
        )
    """)
    
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS monitores (
            codigo TEXT PRIMARY KEY,
            nombre_completo TEXT NOT NULL,
            correo TEXT NOT NULL,
            materia_id INTEGER NOT NULL,
            FOREIGN KEY (materia_id) REFERENCES materias(id)
        )
    """)
    
    # Precargar materias si la tabla está vacía
    cursor.execute("SELECT COUNT(*) FROM materias")
    if cursor.fetchone()[0] == 0:
        materias_iniciales = [
            (1, 'Arquitectura de Software'),
            (2, 'Ingeniería de Software III'),
            (3, 'Bases de Datos II'),
            (4, 'Programación Concurrente y Distribuida'),
            (5, 'Redes y Comunicaciones'),
            (6, 'Estructuras de Datos'),
            (7, 'Sistemas Operativos'),
            (8, 'Inteligencia Artificial')
        ]
        cursor.executemany("INSERT INTO materias (id, nombre) VALUES (?, ?)", materias_iniciales)
        
    # Precargar monitores de demostración si la tabla está vacía
    cursor.execute("SELECT COUNT(*) FROM monitores")
    if cursor.fetchone()[0] == 0:
        monitores_iniciales = [
            ('202601', 'Camila Andrea Gómez', 'camila.gomez@ucaldas.edu.co', 1),
            ('202602', 'Sebastián Restrepo Toro', 'sebastian.restrepo@ucaldas.edu.co', 3),
            ('202603', 'Valentina Ríos Cardona', 'valentina.rios@ucaldas.edu.co', 4),
            ('202604', 'Mateo López Salazar', 'mateo.lopez@ucaldas.edu.co', 6)
        ]
        cursor.executemany(
            "INSERT INTO monitores (codigo, nombre_completo, correo, materia_id) VALUES (?, ?, ?, ?)",
            monitores_iniciales
        )
    
    conn.commit()
    conn.close()

# --- ENDPOINTS REST ---

@app.route('/api/materias', methods=['GET'])
def get_materias():
    conn = get_db_connection()
    materias = conn.execute("SELECT id, nombre FROM materias ORDER BY id ASC").fetchall()
    conn.close()
    return jsonify([dict(row) for row in materias]), 200

@app.route('/api/monitores', methods=['GET'])
def get_monitores():
    conn = get_db_connection()
    query = """
        SELECT m.codigo, m.nombre_completo, m.correo, m.materia_id, mat.nombre AS materia_nombre
        FROM monitores m
        INNER JOIN materias mat ON m.materia_id = mat.id
        ORDER BY m.codigo ASC
    """
    monitores = conn.execute(query).fetchall()
    conn.close()
    return jsonify([dict(row) for row in monitores]), 200

@app.route('/api/monitores/<codigo>', methods=['GET'])
def get_monitor(codigo):
    conn = get_db_connection()
    query = """
        SELECT m.codigo, m.nombre_completo, m.correo, m.materia_id, mat.nombre AS materia_nombre
        FROM monitores m
        INNER JOIN materias mat ON m.materia_id = mat.id
        WHERE m.codigo = ?
    """
    monitor = conn.execute(query, (codigo,)).fetchone()
    conn.close()
    
    if monitor is None:
        return jsonify({"error": f"No se encontró ningún monitor con el código {codigo}"}), 404
        
    return jsonify(dict(monitor)), 200

@app.route('/api/monitores', methods=['POST'])
def create_monitor():
    data = request.get_json(silent=True)
    if not data:
        return jsonify({"error": "Cuerpo de petición JSON inválido o vacío"}), 400

    codigo = str(data.get("codigo", "")).strip()
    nombre_completo = str(data.get("nombre_completo", "")).strip()
    correo = str(data.get("correo", "")).strip()
    materia_id = data.get("materia_id")

    if not codigo or not nombre_completo or not correo or materia_id is None:
        return jsonify({"error": "Todos los campos (codigo, nombre_completo, correo, materia_id) son obligatorios"}), 400

    conn = get_db_connection()
    cursor = conn.cursor()

    materia = cursor.execute("SELECT nombre FROM materias WHERE id = ?", (materia_id,)).fetchone()
    if materia is None:
        conn.close()
        return jsonify({"error": f"La materia con id {materia_id} no existe"}), 400

    try:
        cursor.execute(
            "INSERT INTO monitores (codigo, nombre_completo, correo, materia_id) VALUES (?, ?, ?, ?)",
            (codigo, nombre_completo, correo, materia_id)
        )
        conn.commit()
    except sqlite3.IntegrityError:
        conn.close()
        return jsonify({"error": f"El código {codigo} ya se encuentra registrado"}), 409

    conn.close()
    return jsonify({
        "codigo": codigo,
        "nombre_completo": nombre_completo,
        "correo": correo,
        "materia_id": int(materia_id),
        "materia_nombre": materia["nombre"]
    }), 201

@app.route('/api/monitores/<codigo>', methods=['PUT'])
def update_monitor(codigo):
    data = request.get_json(silent=True)
    if not data:
        return jsonify({"error": "Cuerpo de petición JSON inválido o vacío"}), 400

    nombre_completo = str(data.get("nombre_completo", "")).strip()
    correo = str(data.get("correo", "")).strip()
    materia_id = data.get("materia_id")

    if not nombre_completo or not correo or materia_id is None:
        return jsonify({"error": "Los campos nombre_completo, correo y materia_id son obligatorios"}), 400

    conn = get_db_connection()
    cursor = conn.cursor()

    existente = cursor.execute("SELECT codigo FROM monitores WHERE codigo = ?", (codigo,)).fetchone()
    if existente is None:
        conn.close()
        return jsonify({"error": f"No se encontró el monitor con código {codigo}"}), 404

    materia = cursor.execute("SELECT nombre FROM materias WHERE id = ?", (materia_id,)).fetchone()
    if materia is None:
        conn.close()
        return jsonify({"error": f"La materia con id {materia_id} no existe"}), 400

    cursor.execute(
        "UPDATE monitores SET nombre_completo = ?, correo = ?, materia_id = ? WHERE codigo = ?",
        (nombre_completo, correo, materia_id, codigo)
    )
    conn.commit()
    conn.close()

    return jsonify({
        "codigo": codigo,
        "nombre_completo": nombre_completo,
        "correo": correo,
        "materia_id": int(materia_id),
        "materia_nombre": materia["nombre"]
    }), 200

@app.route('/api/monitores/<codigo>', methods=['DELETE'])
def delete_monitor(codigo):
    conn = get_db_connection()
    cursor = conn.cursor()

    existente = cursor.execute("SELECT codigo FROM monitores WHERE codigo = ?", (codigo,)).fetchone()
    if existente is None:
        conn.close()
        return jsonify({"error": f"No se encontró el monitor con código {codigo}"}), 404

    cursor.execute("DELETE FROM monitores WHERE codigo = ?", (codigo,))
    conn.commit()
    conn.close()

    return jsonify({"mensaje": f"Monitor con código {codigo} eliminado correctamente"}), 200

if __name__ == '__main__':
    init_db()
    app.run(host='0.0.0.0', port=5000, debug=True)