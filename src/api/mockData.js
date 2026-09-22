/**
 * Статичные данные для прототипа. Форма объектов ниже — это и есть
 * черновой контракт с бэкендом: когда появятся реальные эндпойнты,
 * их ответ должен приводиться к этим же полям (или наоборот, эти поля
 * нужно скорректировать под реальный контракт — тогда меняются только
 * файлы в src/api/, компоненты не трогаем).
 */

export const client = {
  id: "client-1",
  fullName: "Христорождественская Вильгельмина А.",
  clientSince: 2019,
  badges: ["mts-bank", "mts-dengi"],
  triggers: [
    {
      id: "passport-expired",
      icon: "passport",
      tone: "warning",
      title: "Просрочен паспорт",
      text: "Требуется обновление документа.",
      link: "/client/data",
    },
    {
      id: "vip",
      icon: "star",
      tone: "vip",
      title: "VIP-клиент",
      text: "Премиальный уровень обслуживания.",
      link: "/client/profile",
    },
    {
      id: "mobile-app",
      icon: "smartphone",
      tone: "info",
      title: "Есть мобильное приложение",
      text: "Клиент активен в приложении МТС Деньги.",
      link: null,
    },
    {
      id: "arrests",
      icon: "shield-alert",
      tone: "danger",
      title: "Есть аресты",
      text: "Наложены ограничения ФССП.",
      link: "/products/debit-mts-dengi/blocks",
    },
    {
      id: "blocks",
      icon: "lock",
      tone: "danger",
      title: "Есть блокировки",
      text: "По карте действует блокировка СФМ.",
      link: "/products/debit-mts-dengi/blocks",
    },
  ],
  openCases: [
    { id: "req-118391", label: "Запрос №118391 · смена тарифа · ответ получен" },
    { id: "complaint-118240", label: "Жалоба №118240 · перевод не пришёл · на 2 линии, 2 дня" },
  ],
};

export const offer = {
  title: "MTS CASHBACK 111 дней",
  text: "Повышенный кэшбэк на 111 дней при активации подписки",
  moreCount: 1,
};

export const cards = [
  {
    id: "card-4019",
    product: "Zero",
    masked: "•••• 4019",
    subtitle: "8_MC World Zero RUR",
    isMain: true,
    status: "active",
    balance: 12430,
    issuedAt: "12.01.2025",
    account: "****6272",
  },
  {
    id: "card-7274",
    product: "ZERO",
    masked: "•••• 7274",
    subtitle: "8_MC World Zero RUR · Виртуальная",
    isMain: false,
    status: "closed",
    balance: 0,
    closedAt: "20.10.2025",
  },
];

// Раньше "card-4019" была захардкожена отдельно в 5 файлах (BalancePage,
// OperationsPage, BlocksPage, CashbackPage, NotificationsPage) как
// начальное значение активной карты — риск разъехаться, если поменяется
// id основной карты. Выводим из cards, а не дублируем строку: единственный
// источник правды — флаг isMain у самой карты. Нужна отдельная константа
// (а не вычисление в компонентах через getCards()), потому что useState
// требует синхронного значения по умолчанию ещё до того, как отработает
// первый fetch.
export const defaultCardId = cards.find((c) => c.isMain)?.id;

export const documents = [
  { id: "doc-statement", title: "Выписка по счёту •••• 4019", subtitle: "За последние 30 дней · PDF" },
  { id: "doc-balance-cert", title: "Справка об остатке денежных средств", subtitle: "На сегодняшнюю дату" },
  { id: "doc-flow-cert", title: "Справка о движении средств", subtitle: "За произвольный период, для налоговой/визы" },
];

export const operations = {
  failedAlert: {
    title: "Ошибка при переводе по СБП",
    text: "12.09.2026 · Перевод на +7 999 ХХХ-ХХ-01 · 5 000 ₽ · отклонён банком-получателем",
  },
  rows: [
    { id: "op1", date: "12.09", op: "Перевод по СБП на +7 999 ХХХ-ХХ-01", sum: "−5 000 ₽", status: "Отклонён" },
    { id: "op2", date: "11.09", op: "Оплата · Wildberries", sum: "−1 240 ₽", status: "Проведена" },
    { id: "op3", date: "10.09", op: "Перевод другу по номеру телефона", sum: "−2 000 ₽", status: "Проведена" },
    { id: "op4", date: "09.09", op: "Оплата в приложении МТС Деньги", sum: "−890 ₽", status: "В обработке" },
    { id: "op5", date: "08.09", op: "Возврат · Ozon", sum: "+450 ₽", status: "Проведена" },
  ],
};

export const blocks = {
  sfm: {
    title: "Блокировка СФМ / Списки",
    text: "Карта •••• 4019 · с 15.09.2026 · причина: проверка операции на 5 000 ₽",
  },
  arrest: {
    title: "Арест ФССП",
    text: "Пристав: ОСП по г. Москве · сумма ареста 15 000 ₽ · с 02.08.2026",
  },
  af: {
    title: "Блокировка АФ",
    text: "Карта •••• 7274 · неактивна",
  },
};

export const cashback = {
  accumulated: 1240,
  categories: [
    { id: "cafe", label: "Кафе и рестораны", percent: "5%", tone: "green" },
    { id: "azs", label: "АЗС", percent: "3%", tone: "green" },
    { id: "pharmacy", label: "Аптеки", percent: "7%", tone: "green" },
    { id: "other", label: "Прочее", percent: "1%", tone: "gray" },
  ],
  promo: {
    title: "Скидка 30% на связь МТС при оплате картой",
    text: "Действует до 31.12.2026 · автоматически при оплате связи картой Zero",
  },
};

export const notifications = [
  {
    id: "sms-ops",
    title: "SMS по операциям",
    subtitle: "Списания и зачисления по карте •••• 4019",
    on: true,
    action: "Отключить",
  },
  {
    id: "sms-plan",
    title: "SMS о плановых платежах",
    subtitle: "Напоминания за 3 дня до списания",
    on: true,
    action: "Отключить",
  },
  {
    id: "push",
    title: "Push-уведомления в приложении",
    subtitle: "Операции, кэшбэк, акции",
    on: true,
    action: "Настроить",
  },
];

export const notificationMethod = {
  title: "Способ уведомлений (ОМТ)",
  current: "Email · v***@mail.ru",
};

export const issuance = {
  currentApplication: {
    card: "Zero",
    status: "Изготовлена",
    text: "Способ получения: курьер · ожидаемая дата 22.09.2026",
  },
  newCard: {
    text: "Виртуальная — мгновенно · пластиковая — 3–5 дней",
  },
  history: [
    { id: "app1", date: "01.09", card: "Zero (пластик)", method: "Курьер", status: "Изготовлена" },
    { id: "app2", date: "12.01.2025", card: "Zero (виртуальная)", method: "Мгновенно", status: "Выдана" },
    { id: "app3", date: "20.10.2025", card: "ZERO (виртуальная)", method: "Мгновенно", status: "Закрыта" },
  ],
};

export const massIncidents = [
  {
    id: "MI-4471",
    theme: "Отклонения переводов по СБП",
    product: "Дебетовые карты МТС Деньги",
    start: "12.09.2026",
    status: "В работе",
    affected: "~3 200 клиентов",
    matchesClient: true,
  },
  {
    id: "MI-4438",
    theme: "Задержка начисления кэшбэка",
    product: "Дебетовые карты МТС Деньги",
    start: "05.09.2026",
    status: "Устраняется",
    affected: "~640 клиентов",
    matchesClient: false,
  },
  {
    id: "MI-4390",
    theme: "Ошибка отображения баланса в приложении",
    product: "Дебетовые карты МТС Деньги",
    start: "28.08.2026",
    status: "Устранён",
    affected: "~1 100 клиентов",
    matchesClient: false,
  },
];

export const singleIncidents = [
  {
    id: "ЕИ-118225",
    theme: "Некорректный баланс по карте",
    created: "02.09.2026",
    line: "2 линия",
    status: "В работе",
  },
  { id: "ЕИ-117904", theme: "Ошибка активации карты", created: "14.08.2026", line: "2 линия", status: "Решён" },
];

export const complaints = [
  {
    id: "Жалоба №118240",
    theme: "Перевод не пришёл получателю",
    created: "10.09.2026",
    term: "2 дня",
    status: "На 2 линии",
  },
  {
    id: "Претензия №117580",
    theme: "Возврат комиссии за обслуживание",
    created: "20.07.2026",
    term: "—",
    status: "Удовлетворена",
  },
];

/** Подсказки базы знаний — сейчас статичный текст, см. InfoTooltip.jsx */
export const kbStubs = {
  section: "Место для статьи по теме раздела (подключается позже)",
  tariff: "Сюда подключается статья с полными условиями тарифа",
  sfm: "Служба финансового мониторинга. Сюда подключается статья с порядком снятия",
  arrest: "Сюда подключается статья с порядком работы с приставами",
  sbpError: "Сюда подключается статья с типовыми причинами и что говорить клиенту",
  cashbackRules: "Сюда подключается статья с условиями по категориям",
  promoRules: "Сюда подключается статья с условиями участия",
  omt: "Основной метод связи с клиентом. Сюда подключается статья с порядком смены",
  issuanceTerms: "Сюда подключается статья со сроками и способами получения",
  incidentsRegulations:
    "Массовые инциденты приходят по интеграции из системы мониторинга — сюда подключается статья с регламентом работы",
};
