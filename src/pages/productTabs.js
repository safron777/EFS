// Единственное место, где живёт id продукта — раньше строка
// "debit-mts-dengi" была захардкожена отдельной константой в каждой
// из 6 страниц вкладок; импортируйте PRODUCT_ID отсюда вместо копирования.
export const PRODUCT_ID = "debit-mts-dengi";

const base = `/products/${PRODUCT_ID}`;

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
