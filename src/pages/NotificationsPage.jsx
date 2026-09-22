import { Card } from "../components/ui/Card";
import { Button } from "../components/ui/Button";
import { Toggle } from "../components/ui/Toggle";
import { InfoTooltip } from "../components/ui/InfoTooltip";
import { CardSwitcher } from "../components/shared/CardSwitcher";
import { EscalationBar } from "../components/shared/EscalationBar";
import { SectionHeader } from "../components/layout/SectionHeader";
import { useClientId } from "../components/layout/AppShell";
import { useCardScopedData } from "../hooks/useCardScopedData";
import { getNotifications, toggleNotification } from "../api/client";
import { kbStubs } from "../api/mockData";
import { PRODUCT_TABS, PRODUCT_ID } from "./productTabs";
import "./pages.css";

export function NotificationsPage() {
  const clientId = useClientId();
  const { cards, activeCardId, setActiveCardId, data, loading, reload } = useCardScopedData(
    clientId,
    PRODUCT_ID,
    (cardId) => getNotifications(cardId)
  );

  async function handleToggle(item) {
    // Оптимистично не обновляем — вызываем заглушку и просто перезапрашиваем,
    // чтобы поведение сразу было готово под реальный бэкенд с его же данными.
    await toggleNotification(activeCardId, item.id, !item.on);
    reload();
  }

  return (
    <>
      <SectionHeader title="Дебетовые карты МТС Деньги" tooltip={kbStubs.section} tabs={PRODUCT_TABS} />
      <Card as="div" className="content-panel">
        {cards && <CardSwitcher cards={cards} activeCardId={activeCardId} onSelect={setActiveCardId} />}

        {loading || !data ? (
          <div className="state-message">Загрузка…</div>
        ) : (
          <div>
            {data.items.map((item) => (
              <div key={item.id} className="toggle-row">
                <div>
                  <div className="toggle-row__title">{item.title}</div>
                  <div className="toggle-row__subtitle">{item.subtitle}</div>
                </div>
                <div className="toggle-row__right">
                  <Toggle checked={item.on} onChange={() => handleToggle(item)} label={item.title} />
                  <Button variant="secondary">{item.action}</Button>
                </div>
              </div>
            ))}
            <div className="toggle-row">
              <div>
                <div className="toggle-row__title">
                  {data.method.title}
                  <InfoTooltip title="Что такое ОМТ" text={kbStubs.omt} />
                </div>
                <div className="toggle-row__subtitle">Сейчас: {data.method.current}</div>
              </div>
              <Button variant="secondary">Изменить</Button>
            </div>
          </div>
        )}

        <EscalationBar />
      </Card>
    </>
  );
}
