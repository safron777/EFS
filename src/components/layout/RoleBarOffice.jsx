import { Search, Bell, Check } from "lucide-react";
import { Card } from "../ui/Card";
import "./RoleBarOffice.css";

/**
 * Роль-бар для сотрудника офиса / лёгкой сети: нет голосовой панели
 * (как у КЦ) и нет ленты вкладок клиентов (как у чата) — сотрудник
 * обслуживает одного клиента, пришедшего лично.
 *
 * Аналоги для других ролей — RoleBarVoice.jsx и RoleBarChat.jsx —
 * в этой поставке не реализованы (см. README), но подключаются
 * тем же контрактом: получают agent и рендерятся в AppShell.
 */
export function RoleBarOffice({ clientPhone, agentInitials = "МК" }) {
  return (
    <Card as="div" className="role-bar-office">
      <div className="role-bar-office__search">
        <Search size={15} color="var(--color-text-faint)" />
        <label htmlFor="client-search" className="sr-only">
          Поиск клиента
        </label>
        <input id="client-search" type="text" defaultValue={clientPhone} readOnly />
      </div>

      <span className="role-bar-office__verified">
        <Check size={13} /> Личность подтверждена · паспорт проверен
      </span>

      <div className="role-bar-office__right">
        <button className="icon-btn" aria-label="Уведомления">
          <Bell size={16} color="var(--color-text-secondary)" />
        </button>
        <div className="avatar">{agentInitials}</div>
      </div>
    </Card>
  );
}
