import { NavLink } from "react-router-dom";
import { Card } from "../ui/Card";
import { InfoTooltip } from "../ui/InfoTooltip";
import "./SectionHeader.css";

/**
 * Шапка раздела — отдельный модуль от контента (см. обсуждение в макете:
 * заголовок+вкладки и сам контент должны быть разными карточками,
 * чтобы ими было проще управлять независимо).
 *
 * tabs: [{ to, label }]
 */
export function SectionHeader({ title, tooltip, tabs }) {
  return (
    <Card as="div" className="section-header">
      <div className="section-header__title">
        <h1>{title}</h1>
        {tooltip && <InfoTooltip title="База знаний" text={tooltip} />}
      </div>
      <nav className="section-header__tabs" aria-label="Вкладки раздела">
        {tabs.map((t) => (
          <NavLink key={t.to} to={t.to} className={({ isActive }) => `tab ${isActive ? "is-active" : ""}`}>
            {t.label}
          </NavLink>
        ))}
      </nav>
    </Card>
  );
}
