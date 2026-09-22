import { Link } from "react-router-dom";
import { FileWarning, Star, Smartphone, ShieldAlert, Lock, Percent } from "lucide-react";
import { Card } from "../ui/Card";
import { Pill } from "../ui/Pill";
import "./IdentityBar.css";

const TRIGGER_ICON = {
  passport: FileWarning,
  star: Star,
  smartphone: Smartphone,
  "shield-alert": ShieldAlert,
  lock: Lock,
};

const BADGE_LABEL = {
  "mts-bank": { label: "Клиент МТС Банка", tone: "red" },
  "mts-dengi": { label: "Клиент МТС Деньги", tone: "blue" },
};

function TriggerIcon({ trigger }) {
  const Icon = TRIGGER_ICON[trigger.icon];
  const body = (
    <span className={`trigger-chip trigger-chip--${trigger.tone}`} aria-label={trigger.title}>
      <Icon size={17} />
    </span>
  );
  return (
    <span className="trigger" title={trigger.title}>
      {trigger.link ? <Link to={trigger.link}>{body}</Link> : body}
      <span className="trigger-tooltip">
        <b>{trigger.title}</b>
        <span>{trigger.text}</span>
      </span>
    </span>
  );
}

/**
 * Модуль идентификации клиента. Всегда одинаковый для всех ролей —
 * различаются только роль-бар над ним и контент карточки под ним.
 */
export function IdentityBar({ client, offer, onOpenOffer }) {
  return (
    <Card as="div" className="identity-bar">
      <div className="identity-bar__main">
        <div className="identity-bar__name">{client.fullName}</div>
        <div className="identity-bar__row">
          <div className="identity-bar__badges">
            {client.badges.map((b) => (
              <Pill key={b} tone={BADGE_LABEL[b].tone}>
                {BADGE_LABEL[b].label}
              </Pill>
            ))}
          </div>
          <div className="identity-bar__divider" />
          <div className="identity-bar__triggers">
            {client.triggers.map((t) => (
              <TriggerIcon key={t.id} trigger={t} />
            ))}
          </div>
        </div>
        <div className="identity-bar__meta">Клиент с {client.clientSince}</div>
      </div>

      <div className="identity-bar__vdivider" />

      <div className="identity-bar__offer">
        <div className="identity-bar__offer-head">
          <span className="section-title section-title--blue">Предложить клиенту</span>
          {offer.moreCount > 0 && <a href="#more-offers">Ещё {offer.moreCount} →</a>}
        </div>
        <div className="identity-bar__offer-body">
          <div className="identity-bar__offer-icon">
            <Percent size={18} color="#fff" />
          </div>
          <div className="identity-bar__offer-text">
            <div className="identity-bar__offer-title">{offer.title}</div>
            <div className="identity-bar__offer-sub">{offer.text}</div>
          </div>
          <button className="btn btn-primary btn-md" onClick={onOpenOffer}>
            Оформить
          </button>
        </div>
      </div>

      <div className="identity-bar__vdivider" />

      <div className="identity-bar__cases">
        <span className="section-title">Обращения клиента</span>
        <button className="identity-bar__cases-toggle">
          <Pill tone="amber">{client.openCases.length} открыты</Pill>
          <span>Показать ▾</span>
        </button>
        <div className="identity-bar__cases-list">
          {client.openCases.map((c) => (
            <div key={c.id}>{c.label}</div>
          ))}
        </div>
      </div>
    </Card>
  );
}
