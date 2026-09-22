import { useState } from "react";
import { Card } from "../components/ui/Card";
import { Button } from "../components/ui/Button";
import { InfoTooltip } from "../components/ui/InfoTooltip";
import { CardSwitcher } from "../components/shared/CardSwitcher";
import { EscalationBar } from "../components/shared/EscalationBar";
import { SectionHeader } from "../components/layout/SectionHeader";
import { useClientId } from "../components/layout/AppShell";
import { useApi } from "../hooks/useApi";
import { getCards, getBlocks } from "../api/client";
import { kbStubs, defaultCardId } from "../api/mockData";
import { PRODUCT_TABS, PRODUCT_ID } from "./productTabs";
import "./pages.css";

export function BlocksPage() {
  const clientId = useClientId();
  const { data: cards } = useApi(() => getCards(clientId, PRODUCT_ID), [clientId]);
  const [activeCardId, setActiveCardId] = useState(defaultCardId);
  const { data: blocks, loading } = useApi(() => getBlocks(activeCardId), [activeCardId]);

  return (
    <>
      <SectionHeader title="Дебетовые карты МТС Деньги" tooltip={kbStubs.section} tabs={PRODUCT_TABS} />
      <Card as="div" className="content-panel">
        {cards && <CardSwitcher cards={cards} activeCardId={activeCardId} onSelect={setActiveCardId} />}

        {loading || !blocks ? (
          <div className="state-message">Загрузка…</div>
        ) : (
          <>
            <div className="two-col">
              <div className="alert-box alert-box--danger alert-box--column">
                <div className="alert-box__title-row">
                  <span className="alert-box__title alert-box__title--danger">{blocks.sfm.title}</span>
                  <InfoTooltip title="Что такое СФМ" text={kbStubs.sfm} tone="danger" />
                </div>
                <div className="alert-box__text alert-box__text--danger">{blocks.sfm.text}</div>
                <div className="alert-box__actions">
                  <Button variant="secondary">Узнать статус проверки</Button>
                  <Button variant="danger">Снять блокировку</Button>
                </div>
              </div>

              <div className="alert-box alert-box--neutral alert-box--column">
                <div className="alert-box__title-row">
                  <span className="alert-box__title">{blocks.arrest.title}</span>
                  <InfoTooltip title="Как снять арест" text={kbStubs.arrest} />
                </div>
                <div className="alert-box__text alert-box__text--muted">{blocks.arrest.text}</div>
                <div className="alert-box__actions">
                  <Button variant="secondary">Снять часть средств</Button>
                  <Button variant="secondary">История арестов</Button>
                </div>
              </div>
            </div>

            <div className="summary-card">
              <div>
                <div className="block-title">{blocks.af.title}</div>
                <div className="block-subtext">{blocks.af.text}</div>
              </div>
              <Button variant="secondary">Разблокировать карту</Button>
            </div>
          </>
        )}

        <EscalationBar />
      </Card>
    </>
  );
}
