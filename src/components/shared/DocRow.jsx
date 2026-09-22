import { Button } from "../ui/Button";
import "./DocRow.css";

/**
 * Строка справки/выписки. canPrint включён только для роли "Офис"
 * (см. AppShell/BalancePage) — у КЦ и чата документ можно только
 * отправить, распечатать в отделении может только офис/лёгкая сеть.
 */
export function DocRow({ title, subtitle, onSend, onPrint, canPrint = false }) {
  return (
    <div className="doc-row">
      <div>
        <div className="doc-row__title">{title}</div>
        <div className="doc-row__subtitle">{subtitle}</div>
      </div>
      <div className="doc-row__actions">
        <Button variant="secondary" onClick={onSend}>
          Отправить
        </Button>
        {canPrint && (
          <Button variant="secondary" onClick={onPrint}>
            Распечатать
          </Button>
        )}
      </div>
    </div>
  );
}
