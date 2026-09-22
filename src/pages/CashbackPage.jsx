import { useState } from "react";
import { Card } from "../components/ui/Card";
import { Button } from "../components/ui/Button";
import { Pill } from "../components/ui/Pill";
import { InfoTooltip } from "../components/ui/InfoTooltip";
import { CardSwitcher } from "../components/shared/CardSwitcher";
import { EscalationBar } from "../components/shared/EscalationBar";
import { SectionHeader } from "../components/layout/SectionHeader";
import { useClientId } from "../components/layout/AppShell";
import { useApi } from "../hooks/useApi";
import { getCards, getCashback } from "../api/client";
import { kbStubs } from "../api/mockData";
import { PRODUCT_TABS } from "./productTabs";
import "./pages.css";

const PRODUCT_ID = "debit-mts-dengi";

export function CashbackPage() {
  const clientId = useClientId();
  const { data: cards } = useApi(() => getCards(clientId, PRODUCT_ID), [clientId]);
  const [activeCardId, setActiveCardId] = useState("card-4019");
  const { data: cashback, loading } = useApi(() => getCashback(activeCardId), [activeCardId]);

  return (
    <>
      <SectionHeader title="Дебетовые карты МТС Деньги" tooltip={kbStubs.section} tabs={PRODUCT_TABS} />
      <Card as="div" className="content-panel">
        {cards && <CardSwitcher cards={cards} activeCardId={activeCardId} onSelect={setActiveCardId} />}

        {loading || !cashback ? (
          <div className="state-message">Загрузка…</div>
        ) : (
          <>
            <div className="summary-card">
              <div>
                <div className="summary-card__label">Накоплено кэшбэка</div>
                <div className="summary-card__value">{cashback.accumulated.toLocaleString("ru-RU")} ₽</div>
              </div>
              <Button variant="primary">Обменять на рубли</Button>
            </div>

            <div>
              <div className="block-title-row block-title-row--spaced">
                <span className="section-title">Категории кэшбэка</span>
                <InfoTooltip title="Как считается %" text={kbStubs.cashbackRules} />
              </div>
              <div className="category-row">
                {cashback.categories.map((c) => (
                  <div key={c.id} className="category-pill">
                    <span>{c.label}</span>
                    <Pill tone={c.tone}>{c.percent}</Pill>
                  </div>
                ))}
              </div>
            </div>

            <div className="promo-card">
              <div className="block-title-row">
                <span className="block-title">{cashback.promo.title}</span>
                <InfoTooltip title="Правила акции" text={kbStubs.promoRules} />
              </div>
              <div className="block-subtext">{cashback.promo.text}</div>
            </div>
          </>
        )}

        <EscalationBar />
      </Card>
    </>
  );
}
