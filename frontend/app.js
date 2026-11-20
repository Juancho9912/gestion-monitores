'use strict';

// El navegador solo consume el controlador Java. Java se comunica con PHP,
// y PHP con Python, según la arquitectura acordada por el equipo.
const API_BASE = 'http://localhost:8080/api';
const form = document.querySelector('#monitor-form');
const codeInput = document.querySelector('#codigo');
const nameInput = document.querySelector('#nombre_completo');
const emailInput = document.querySelector('#correo');
const subjectSelect = document.querySelector('#materia_id');
const rows = document.querySelector('#monitor-rows');
const notice = document.querySelector('#notice');
const apiStatus = document.querySelector('#api-status');
const formTitle = document.querySelector('#form-title');
const formMode = document.querySelector('#form-mode');
const saveButton = document.querySelector('#save-button');
const cancelEditButton = document.querySelector('#cancel-edit');
const tableCaption = document.querySelector('#table-caption');
let editingCode = null;
let noticeTimer;

function showNotice(message, type = 'info') {
  window.clearTimeout(noticeTimer);
  notice.textContent = message;
  notice.className = `notice ${type}`;
  notice.hidden = false;
  noticeTimer = window.setTimeout(() => { notice.hidden = true; }, 6500);
}

function setApiStatus(state, label) {
  apiStatus.className = `api-status ${state}`;
  apiStatus.lastChild.textContent = ` ${label}`;
}

async function request(path, options = {}) {
  const headers = { Accept: 'application/json', ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...options.headers };
  let response;
  try {
    response = await fetch(`${API_BASE}${path}`, { ...options, headers });
  } catch (error) {
    setApiStatus('offline', 'Sin conexión con Java');
    throw new Error('No fue posible conectar con el controlador Java (localhost:8080). Verifica que el servicio esté iniciado.');
  }
  setApiStatus('online', 'Controlador Java disponible');
  const raw = await response.text();
  let data = null;
  if (raw) {
    try { data = JSON.parse(raw); }
    catch { data = { error: raw }; }
  }
  if (!response.ok) {
    const messages = { 400: 'Revisa los campos ingresados.', 404: 'No se encontró el monitor solicitado.', 409: 'Ya existe un monitor con ese código.' };
    const serverMessage = data && typeof data.error === 'string' ? data.error : '';
    throw new Error(serverMessage || messages[response.status] || `La solicitud falló (HTTP ${response.status}).`);
  }
  return data;
}

function getMonitorList(data) {
  if (Array.isArray(data)) return data;
  if (data && Array.isArray(data.monitores)) return data.monitores;
  if (data && Array.isArray(data.data)) return data.data;
  return [];
}

function renderMonitors(monitors) {
  rows.replaceChildren();
  if (!monitors.length) {
    const tr = document.createElement('tr');
    const td = document.createElement('td');
    td.colSpan = 5;
    td.className = 'empty-cell';
    td.textContent = 'No hay monitores para mostrar.';
    tr.append(td);
    rows.append(tr);
    tableCaption.textContent = '0 registros';
    return;
  }
  for (const monitor of monitors) {
    const tr = document.createElement('tr');
    const values = [monitor.codigo, monitor.nombre_completo, monitor.correo, monitor.materia_nombre || `Materia ${monitor.materia_id}`];
    values.forEach((value, index) => {
      const td = document.createElement('td');
      td.textContent = value == null ? '' : String(value);
      if (index === 0) td.className = 'code-cell';
      tr.append(td);
    });
    const actionCell = document.createElement('td');
    const actions = document.createElement('div');
    actions.className = 'row-actions';
    const edit = document.createElement('button');
    edit.type = 'button'; edit.className = 'action-button'; edit.textContent = 'Editar';
    edit.dataset.action = 'edit'; edit.dataset.code = String(monitor.codigo);
    edit.setAttribute('aria-label', `Editar monitor ${monitor.codigo}`);
    const remove = document.createElement('button');
    remove.type = 'button'; remove.className = 'action-button delete'; remove.textContent = 'Eliminar';
    remove.dataset.action = 'delete'; remove.dataset.code = String(monitor.codigo);
    remove.setAttribute('aria-label', `Eliminar monitor ${monitor.codigo}`);
    actions.append(edit, remove); actionCell.append(actions); tr.append(actionCell); rows.append(tr);
  }
  tableCaption.textContent = `${monitors.length} ${monitors.length === 1 ? 'registro' : 'registros'}`;
}

async function loadSubjects() {
  try {
    const data = await request('/materias');
    const subjects = Array.isArray(data) ? data : (data?.materias || data?.data || []);
    subjectSelect.replaceChildren(new Option('Selecciona una materia', ''));
    for (const subject of subjects) {
      const id = subject.materia_id ?? subject.id;
      const name = subject.materia_nombre ?? subject.nombre ?? subject.name;
      if (id != null && name != null) subjectSelect.add(new Option(String(name), String(id)));
    }
    if (!subjects.length) showNotice('Java respondió, pero no se encontraron materias para el formulario.', 'error');
  } catch (error) {
    subjectSelect.replaceChildren(new Option('No se pudieron cargar las materias', ''));
    showNotice(error.message, 'error');
  }
}

async function loadMonitors() {
  rows.innerHTML = '<tr><td class="empty-cell" colspan="5">Cargando monitores...</td></tr>';
  try {
    renderMonitors(getMonitorList(await request('/monitores')));
  } catch (error) {
    renderMonitors([]);
    showNotice(error.message, 'error');
  }
}

function resetForm() {
  form.reset();
  editingCode = null;
  codeInput.disabled = false;
  formTitle.textContent = 'Registrar monitor';
  formMode.textContent = 'Nuevo';
  saveButton.textContent = 'Registrar monitor';
  cancelEditButton.hidden = true;
}

function beginEdit(monitor) {
  editingCode = String(monitor.codigo);
  codeInput.value = editingCode;
  codeInput.disabled = true;
  nameInput.value = monitor.nombre_completo || '';
  emailInput.value = monitor.correo || '';
  subjectSelect.value = String(monitor.materia_id ?? '');
  formTitle.textContent = `Editar monitor ${editingCode}`;
  formMode.textContent = 'Edición';
  saveButton.textContent = 'Guardar cambios';
  cancelEditButton.hidden = false;
  form.scrollIntoView({ behavior: 'smooth', block: 'start' });
  nameInput.focus({ preventScroll: true });
}

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  if (!form.reportValidity()) return;
  const payload = {
    codigo: editingCode || codeInput.value.trim(),
    nombre_completo: nameInput.value.trim(),
    correo: emailInput.value.trim(),
    materia_id: Number(subjectSelect.value)
  };
  if (!payload.codigo || !payload.nombre_completo || !payload.correo || !payload.materia_id) {
    showNotice('Completa todos los campos antes de continuar.', 'error'); return;
  }
  saveButton.disabled = true;
  try {
    if (editingCode) {
      await request(`/monitores/${encodeURIComponent(editingCode)}`, { method: 'PUT', body: JSON.stringify(payload) });
      showNotice('La información del monitor se actualizó correctamente.', 'success');
    } else {
      await request('/monitores', { method: 'POST', body: JSON.stringify(payload) });
      showNotice('El monitor se registró correctamente.', 'success');
    }
    resetForm();
    await loadMonitors();
  } catch (error) {
    showNotice(error.message, 'error');
  } finally {
    saveButton.disabled = false;
  }
});

cancelEditButton.addEventListener('click', resetForm);
document.querySelector('#refresh-button').addEventListener('click', loadMonitors);
document.querySelector('#clear-search').addEventListener('click', () => {
  document.querySelector('#search-code').value = '';
  loadMonitors();
});
document.querySelector('#search-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  const code = document.querySelector('#search-code').value.trim();
  if (!code) { showNotice('Escribe el código del monitor que deseas buscar.', 'info'); return; }
  try {
    const result = await request(`/monitores/${encodeURIComponent(code)}`);
    const monitor = result?.monitor || result?.data || result;
    if (!monitor || monitor.codigo == null) { renderMonitors([]); return; }
    renderMonitors([monitor]);
  } catch (error) {
    renderMonitors([]);
    showNotice(error.message, 'error');
  }
});

rows.addEventListener('click', async (event) => {
  const button = event.target.closest('button[data-action]');
  if (!button) return;
  const code = button.dataset.code;
  if (button.dataset.action === 'edit') {
    try {
      const result = await request(`/monitores/${encodeURIComponent(code)}`);
      beginEdit(result?.monitor || result?.data || result);
    } catch (error) { showNotice(error.message, 'error'); }
  }
  if (button.dataset.action === 'delete') {
    if (!window.confirm(`¿Deseas eliminar el monitor ${code}? Esta acción no se puede deshacer.`)) return;
    button.disabled = true;
    try {
      await request(`/monitores/${encodeURIComponent(code)}`, { method: 'DELETE' });
      if (editingCode === code) resetForm();
      showNotice(`El monitor ${code} fue eliminado.`, 'success');
      await loadMonitors();
    } catch (error) { button.disabled = false; showNotice(error.message, 'error'); }
  }
});

(async function init() {
  setApiStatus('', 'Conectando');
  await Promise.all([loadSubjects(), loadMonitors()]);
})();
