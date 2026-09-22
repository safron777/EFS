import { useState } from "react";
import { Card } from "../components/ui/Card";
import { Button } from "../components/ui/Button";
import { InfoTooltip } from "../components/ui/InfoTooltip";
import { DataTable } from "../components/ui/DataTable";
import { StatusPill } from "../components/ui/Pill";
import { CardSwitcher } from "../components/shared/CardSwitcher";
import { EscalationBar } from "../components/shared/EscalationBar";
import { SectionHeader } from "../components/layout/SectionHeader";
import { useClientId } from "../components/layout/AppShell";
import { useApi } from "../hooks/useApi";
import { getCards, getOperations } from "../api/client";
import { kbStubs, defaultCardId } from "../api/mockData";
import { PRODUCT_TABS, PRODUCT_ID } from "./productTabs";
import "./pages.css";

const columns = [
  { key: "date", label: "Дата", width: 70 },
  { key: "op", label: "Операция" },
  { key: "sum", label: "Сумма", width: 110 },
  { key: "status", label: "Статус", width: 130, render: (row) => <StatusPill status={row.status} /> },
];

export function OperationsPage() {
  const clientId = useClientId();
  const { data: cards } = useApi(() => getCards(clientId, PRODUCT_ID), [clientId]);
  const [activeCardId, setActiveCardId] = useState(defaultCardId);
  const { data: ops, loading } = useApi(() => getOperations(activeCardId), [activeCardId]);

  return (
    <>
      <SectionHeader title="Дебетовые карты МТС Деньги" tooltip={kbStubs.section} tabs={PRODUCT_TABS} />
      <Card as="div" className="content-panel">
        {cards && <CardSwitcher cards={cards} activeCardId={activeCardId} onSelect={setActiveCardId} />}

        {loading || !ops ? (
          <div className="state-message">Загрузка операций…</div>
        ) : (
          <>
            <div className="alert-box alert-box--danger">
              <div>
                <div className="alert-box__title-row">
                  <span className="alert-box__title alert-box__title--danger">{ops.failedAlert.title}</span>
                  <InfoTooltip title="Причины отклонения СБП" text={kbStubs.sbpError} tone="danger" />
                </div>
                <div className="alert-box__text alert-box__text--danger">{ops.failedAlert.text}</div>
              </div>
              <div className="alert-box__actions">
                <Button variant="secondary">Повторить перевод</Button>
                <Button variant="secondary">Детали ошибки</Button>
              </div>
            </div>

            <DataTable columns={columns} rows={ops.rows} />
          </>
        )}

        <EscalationBar />
      </Card>
    </>
  );
}
