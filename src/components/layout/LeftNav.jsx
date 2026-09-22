import { NavLink } from "react-router-dom";
import { AlertTriangle, CreditCard } from "lucide-react";
import { Card } from "../ui/Card";
import { Pill } from "../ui/Pill";
import { PRODUCT_SUBNAV } from "../../pages/productTabs";
import "./LeftNav.css";

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
          <NavLink key={t.to} to={t.to} className={({ isActive }) => `subnav-item ${isActive ? "is-active" : ""}`}>
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
