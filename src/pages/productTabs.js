// Единственное место, где живёт id продукта — раньше строка
// "debit-mts-dengi" была захардкожена отдельной константой в каждой
// из 6 страниц вкладок; импортируйте PRODUCT_ID отсюда вместо копирования.
export const PRODUCT_ID = "debit-mts-dengi";

const base = `/products/${PRODUCT_ID}`;

// Единственный источник правды по вкладкам продукта. Раньше это были два
// независимых массива (PRODUCT_TABS здесь и PRODUCT_SUBNAV в LeftNav.jsx)
// с ручным требованием в комментарии "пути должны совпадать между двумя
// списками" — теперь совпадать нечему, оба выводятся из одного списка.
// shortLabel — узкие вкладки в шапке раздела (SectionHeader), fullLabel —
// подменю каталога слева (LeftNav).
const PRODUCT_SECTIONS = [
  { path: "balance", shortLabel: "Баланс и карты", fullLabel: "Баланс и карты" },
  { path: "operations", shortLabel: "Операции", fullLabel: "Операции" },
  { path: "blocks", shortLabel: "Блокировки/арест", fullLabel: "Блокировки и арест" },
  { path: "cashback", shortLabel: "Кэшбэк", fullLabel: "Кэшбэк" },
  { path: "notifications", shortLabel: "Уведомления", fullLabel: "Уведомления" },
  { path: "issuance", shortLabel: "Оформление", fullLabel: "Оформление/перевыпуск" },
];

export const PRODUCT_TABS = PRODUCT_SECTIONS.map((s) => ({ to: `${base}/${s.path}`, label: s.shortLabel }));

export const PRODUCT_SUBNAV = PRODUCT_SECTIONS.map((s) => ({ to: `${base}/${s.path}`, label: s.fullLabel }));

export const INCIDENT_TABS = [
  { to: "/incidents/mass", label: "Массовые инциденты" },
  { to: "/incidents/single", label: "Единичные инциденты" },
  { to: "/incidents/complaints", label: "Жалобы и претензии" },
];
