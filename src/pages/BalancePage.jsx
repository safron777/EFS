import { Card } from "../components/ui/Card";
import { Button } from "../components/ui/Button";
import { Pill } from "../components/ui/Pill";
import { InfoTooltip } from "../components/ui/InfoTooltip";
import { DocRow } from "../components/shared/DocRow";
import { EscalationBar } from "../components/shared/EscalationBar";
import { SectionHeader } from "../components/layout/SectionHeader";
import { useClientId } from "../components/layout/AppShell";
import { useApi } from "../hooks/useApi";
import { getCards, getDocuments, sendDocument, printDocument } from "../api/client";
import { kbStubs, defaultCardId } from "../api/mockData";
import { PRODUCT_TABS, PRODUCT_ID } from "./productTabs";
import "./pages.css";

/**
 * Роль "Офис" — единственная, где на балансовой вкладке доступен
 * "Перевыпустить карту" (пластик выдаётся лично) и "Распечатать"
 * у справок (в офисе есть принтер). У КЦ/чата этих действий нет —
 * см. TODO в README про параметризацию canReissue/canPrint по роли.
 */
export function BalancePage() {
  const clientId = useClientId();
  const canReissue = true; // роль "Офис"
  const canPrint = true; // роль "Офис"

  const { data: cards, loading: cardsLoading } = useApi(() => getCards(clientId, PRODUCT_ID), [clientId]);
  const { data: docs, loading: docsLoading } = useApi(() => getDocuments(clientId, defaultCardId), [clientId]);

  if (cardsLoading || docsLoading) return <div className="state-message">Загрузка…</div>;

  const mainCard = cards.find((c) => c.isMain);
  const otherCards = cards.filter((c) => !c.isMain);

  return (
    <>
      <SectionHeader title="Дебетовые карты МТС Деньги" tooltip={kbStubs.section} tabs={PRODUCT_TABS} />

      <Card as="div" className="cards-overview">
        <div className="card-tile">
          <div className="card-tile__top">
            <div>
              <div className="card-tile__name-row">
                <span className="card-tile__name">{mainCard.product}</span>
                <Pill tone="green">Основная</Pill>
                <InfoTooltip title="Условия тарифа Zero" text={kbStubs.tariff} />
              </div>
              <div className="card-tile__subtitle">
                {mainCard.subtitle} · {mainCard.masked}
              </div>
            </div>
            <span className="card-tile__status card-tile__status--active">● Активна</span>
          </div>
          <div className="card-tile__balance">{mainCard.balance.toLocaleString("ru-RU")} ₽</div>
          <div className="card-tile__meta">
            Оформлена {mainCard.issuedAt} · Счёт {mainCard.account}
          </div>
          <div className="card-tile__actions">
            <Button variant="secondary">Отключить SMS</Button>
            <Button variant="secondary">Реквизиты</Button>
            <Button variant="secondary">Платёжное поручение</Button>
            {canReissue && <Button variant="secondary">Перевыпустить карту</Button>}
            <Button variant="secondary">Улучшить тариф</Button>
            <Button variant="danger">Заблокировать</Button>
            <Button variant="danger">Закрыть карту</Button>
          </div>
        </div>

        {otherCards.map((c) => (
          <div key={c.id} className="card-tile card-tile--closed">
            <div className="card-tile__top">
              <div>
                <span className="card-tile__name">{c.product}</span>
                <div className="card-tile__subtitle">{c.subtitle}</div>
              </div>
              <span className="card-tile__status card-tile__status--closed">● Закрыта {c.closedAt}</span>
            </div>
            <div className="card-tile__balance card-tile__balance--closed">{c.balance} ₽</div>
            <div className="card-tile__actions">
              <Button variant="secondary">Реквизиты (архив)</Button>
            </div>
          </div>
        ))}
      </Card>

      <Card as="div" className="content-panel">
        <div className="docs-section">
          <div className="section-title">Справки и выписки</div>
          <div className="docs-list">
            {docs.map((d) => (
              <DocRow
                key={d.id}
                title={d.title}
                subtitle={d.subtitle}
                canPrint={canPrint}
                onSend={() => sendDocument(clientId, d.id)}
                onPrint={() => printDocument(clientId, d.id)}
              />
            ))}
          </div>
        </div>

        <div className="note-bar">
          <span>Тема обращения определяется автоматически по журналу действий — отдельный выбор не требуется.</span>
          <a href="#balance-mismatch">Баланс не совпадает с ожидаемым? →</a>
        </div>

        <EscalationBar />
      </Card>
    </>
  );
}
