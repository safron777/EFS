import * as mock from "./mockData";

/**
 * Единая точка подключения реального бэкенда.
 *
 * Сейчас имитирует сеть (задержка + клон данных, чтобы никто случайно
 * не мутировал общий мок). Когда появятся реальные эндпойнты —
 * достаточно переписать ТОЛЬКО эту функцию на настоящий fetch/axios
 * с базовым URL и авторизацией; все функции ниже (getClientCard,
 * getOperations и т.д.) менять не придётся, если сохранить форму ответа.
 *
 * TODO(backend): заменить на реальный HTTP-клиент, например:
 *   const res = await fetch(`${API_BASE_URL}${path}`, { headers: authHeaders(), ...init });
 *   if (!res.ok) throw new ApiError(res.status, await res.text());
 *   return res.json();
 *
 * TODO(backend): отдельно обработать 401/403 в этой точке (протухший
 * токен/нет прав на клиента) — не как обычную сетевую ошибку. useApi
 * сейчас кладёт любой reject в error и ничего не знает про статус-коды;
 * useApi/AppShellOffice, скорее всего, нужно научить на 401 сбрасывать
 * сессию и уводить на логин, а не показывать "устаревшие" данные клиента
 * поверх старого state. Сейчас проверить нечем — fetchJson ничего не
 * запрашивает по-настоящему.
 */
async function fetchJson(path, { data, delay = 350 } = {}) {
  await new Promise((resolve) => setTimeout(resolve, delay));
  return JSON.parse(JSON.stringify(data));
}

// ---- Чтение ---------------------------------------------------------

export function getClientCard(clientId) {
  return fetchJson(`/clients/${clientId}`, { data: mock.client });
}

export function getOffer(clientId) {
  return fetchJson(`/clients/${clientId}/offers/top`, { data: mock.offer });
}

export function getCards(clientId, productId) {
  return fetchJson(`/clients/${clientId}/products/${productId}/cards`, { data: mock.cards });
}

export function getDocuments(clientId, cardId) {
  return fetchJson(`/clients/${clientId}/cards/${cardId}/documents`, { data: mock.documents });
}

export function getOperations(cardId) {
  return fetchJson(`/cards/${cardId}/operations`, { data: mock.operations });
}

export function getBlocks(cardId) {
  return fetchJson(`/cards/${cardId}/blocks`, { data: mock.blocks });
}

export function getCashback(cardId) {
  return fetchJson(`/cards/${cardId}/cashback`, { data: mock.cashback });
}

export function getNotifications(cardId) {
  return fetchJson(`/cards/${cardId}/notifications`, {
    data: { items: mock.notifications, method: mock.notificationMethod },
  });
}

export function getIssuance(clientId, productId) {
  return fetchJson(`/clients/${clientId}/products/${productId}/issuance`, { data: mock.issuance });
}

export function getMassIncidents() {
  return fetchJson(`/incidents/mass`, { data: mock.massIncidents });
}

export function getSingleIncidents(clientId) {
  return fetchJson(`/clients/${clientId}/incidents/single`, { data: mock.singleIncidents });
}

export function getComplaints(clientId) {
  return fetchJson(`/clients/${clientId}/complaints`, { data: mock.complaints });
}

// ---- Действия (мутации) ---------------------------------------------
// Возвращают { ok: true } — реальные ручки, скорее всего, вернут
// созданную сущность (id тикета и т.п.); тогда вызывающий код в
// pages/*.jsx нужно будет чуть дополнить, сама сигнатура функций
// меняться не должна.

export function attachClientToIncident(clientId, incidentId) {
  console.info(`[stub] attachClientToIncident(${clientId}, ${incidentId})`);
  return fetchJson(`/incidents/mass/${incidentId}/attach`, { data: { ok: true }, delay: 300 });
}

export function createSingleIncident(clientId, payload) {
  console.info(`[stub] createSingleIncident(${clientId})`, payload);
  return fetchJson(`/clients/${clientId}/incidents/single`, { data: { ok: true }, delay: 300 });
}

export function createComplaint(clientId, payload) {
  console.info(`[stub] createComplaint(${clientId})`, payload);
  return fetchJson(`/clients/${clientId}/complaints`, { data: { ok: true }, delay: 300 });
}

export function toggleNotification(cardId, notificationId, on) {
  console.info(`[stub] toggleNotification(${cardId}, ${notificationId}, ${on})`);
  return fetchJson(`/cards/${cardId}/notifications/${notificationId}`, { data: { ok: true }, delay: 250 });
}

export function sendDocument(clientId, documentId, channel = "email") {
  console.info(`[stub] sendDocument(${clientId}, ${documentId}, ${channel})`);
  return fetchJson(`/clients/${clientId}/documents/${documentId}/send`, { data: { ok: true }, delay: 300 });
}

export function printDocument(clientId, documentId) {
  console.info(`[stub] printDocument(${clientId}, ${documentId})`);
  return fetchJson(`/clients/${clientId}/documents/${documentId}/print`, { data: { ok: true }, delay: 300 });
}
