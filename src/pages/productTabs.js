const base = "/products/debit-mts-dengi";

// Подписи здесь короче, чем в PRODUCT_SUBNAV (components/layout/LeftNav.jsx) —
// это вкладки в узкой шапке раздела, а не подменю каталога. Пути и порядок
// должны совпадать между двумя списками.
export const PRODUCT_TABS = [
  { to: `${base}/balance`, label: "Баланс и карты" },
  { to: `${base}/operations`, label: "Операции" },
  { to: `${base}/blocks`, label: "Блокировки/арест" },
  { to: `${base}/cashback`, label: "Кэшбэк" },
  { to: `${base}/notifications`, label: "Уведомления" },
  { to: `${base}/issuance`, label: "Оформление" },
];

export const INCIDENT_TABS = [
  { to: "/incidents/mass", label: "Массовые инциденты" },
  { to: "/incidents/single", label: "Единичные инциденты" },
  { to: "/incidents/complaints", label: "Жалобы и претензии" },
];
