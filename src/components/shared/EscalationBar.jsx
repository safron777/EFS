import { FileText, Flag } from "lucide-react";
import { useNavigate } from "react-router-dom";
import "./EscalationBar.css";

/**
 * Подвал эскалации. Живёт внутри карточки-контента (не отдельным
 * модулем) как full-bleed полоса, но логически — переиспользуемый
 * компонент: подключается на каждой вкладке продукта одинаково.
 *
 * Реальное создание ЕИ/жалобы сейчас просто уводит в раздел
 * "Инциденты и обращения" — форму создания стоит открыть отдельной
 * задачей, когда определитесь с обязательными полями.
 */
export function EscalationBar() {
  const navigate = useNavigate();
  return (
    <div className="escalation-bar">
      <span>Не получилось закрыть вопрос на месте?</span>
      <div className="escalation-bar__actions">
        <button className="btn btn-secondary btn-md" onClick={() => navigate("/incidents/single")}>
          <FileText size={15} />
          Создать ЕИ · 2 линия
        </button>
        <button className="btn btn-danger btn-md" onClick={() => navigate("/incidents/complaints")}>
          <Flag size={15} />
          Оставить жалобу
        </button>
        <a href="#history">История по теме →</a>
      </div>
    </div>
  );
}
