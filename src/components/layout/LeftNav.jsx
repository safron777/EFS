import { NavLink } from "react-router-dom";
import { AlertTriangle, CreditCard } from "lucide-react";
import { Card } from "../ui/Card";
import { Pill } from "../ui/Pill";
import "./LeftNav.css";

/**
 * Короткий список для подменю каталога — с полными подписями
 * ("Блокировки и арест", "Оформление/перевыпуск"). Специально НЕ тот же
 * массив, что PRODUCT_TABS в ../pages/productTabs.js — там подписи короче,
 * под узкие вкладки в шапке раздела. Пути (path) и порядок должны совпадать
 * между двумя списками — при добавлении/переименовании продукта обновляйте
 * оба.
 */
const PRODUCT_SUBNAV = [
  { path: "balance", label: "Баланс и карты" },
  { path: "operations", label: "Операции" },
  { path: "blocks", label: "Блокировки и арест" },
  { path: "cashback", label: "Кэшбэк" },
  { path: "notifications", label: "Уведомления" },
  { path: "issuance", label: "Оформление/перевыпуск" },
];

/**
 * Каталог слева — три независимых по смыслу раздела в одной карточке
 * (разделители между ними), как согласовано в макете:
 * "Инциденты и обращения" (корень каталога, видно из любой вкладки),
 * "Продукты" (текущий продукт развёрнут, остальные — заглушки на Siebel),
 * "Клиент" (общие, не привязанные к продукту разделы).
 */
export function LeftNav({ openMassIncidentsCount = 0 }) {
  return (
    <Card as="nav" className="left-nav" aria-label="Каталог">
      <div className="section-title">Инциденты и обращения</div>
      <NavLink to="/incidents/mass" className={({ isActive }) => `nav-item nav-item--critical ${isActive ? "is-active" : ""}`}>
        <span>
          <AlertTriangle size={14} className="nav-icon" />
          Массовые инциденты
        </span>
        {openMassIncidentsCount > 0 && <Pill tone="red">{openMassIncidentsCount}</Pill>}
      </NavLink>
      <NavLink to="/incidents/single" className={({ isActive }) => `nav-item ${isActive ? "is-active" : ""}`}>
        <span>Единичные инциденты</span>
      </NavLink>
      <NavLink to="/incidents/complaints" className={({ isActive }) => `nav-item ${isActive ? "is-active" : ""}`}>
        <span>Жалобы и претензии</span>
      </NavLink>

      <div className="left-nav__divider" />

      <div className="section-title">Продукты</div>
      <div className="nav-item nav-item--product is-active">
        <span>
          <CreditCard size={14} className="nav-icon" />
          Деб. карты МТС Деньги
        </span>
      </div>
      <div className="left-nav__subgroup">
        {PRODUCT_SUBNAV.map((t) => (
          <NavLink
            key={t.path}
            to={`/products/debit-mts-dengi/${t.path}`}
            className={({ isActive }) => `subnav-item ${isActive ? "is-active" : ""}`}
          >
            {t.label}
          </NavLink>
        ))}
      </div>

      {["Карты МТС Банка", "Кредиты", "Вклады и счета"].map((label) => (
        <div key={label} className="nav-item nav-item--locked">
          <span>{label}</span>
          <Pill tone="gray">Siebel</Pill>
        </div>
      ))}

      <div className="left-nav__divider" />

      <div className="section-title">Клиент</div>
      <div className="nav-item">
        <span>Данные клиента</span>
      </div>
      <div className="nav-item">
        <span>История обращений</span>
      </div>
    </Card>
  );
}
