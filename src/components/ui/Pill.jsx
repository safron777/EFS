import "./Pill.css";

/**
 * Маленький статус-бейдж. tone определяет цвет по смыслу статуса,
 * а не по месту использования — так статусы остаются согласованными
 * между экранами (например, "Проведена" и "Решён" всегда зелёные).
 */
const TONE_CLASS = {
  neutral: "pill-neutral",
  red: "pill-red",
  blue: "pill-blue",
  amber: "pill-amber",
  green: "pill-green",
  gray: "pill-gray",
};

export function Pill({ tone = "neutral", children }) {
  return <span className={`pill ${TONE_CLASS[tone] || TONE_CLASS.neutral}`}>{children}</span>;
}

/** Готовые тоны для частых статусов операций/заявок/инцидентов. */
export const STATUS_TONE = {
  Проведена: "green",
  Выдана: "green",
  Решён: "green",
  Устранён: "green",
  Удовлетворена: "green",
  Отклонён: "red",
  "В работе": "amber",
  Устраняется: "amber",
  "В обработке": "amber",
  Изготовлена: "amber",
  "На 2 линии": "amber",
  Закрыта: "gray",
};

export function StatusPill({ status }) {
  return <Pill tone={STATUS_TONE[status] || "neutral"}>{status}</Pill>;
}
