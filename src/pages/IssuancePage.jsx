import { Card } from "../components/ui/Card";
import { Button } from "../components/ui/Button";
import { StatusPill } from "../components/ui/Pill";
import { InfoTooltip } from "../components/ui/InfoTooltip";
import { DataTable } from "../components/ui/DataTable";
import { EscalationBar } from "../components/shared/EscalationBar";
import { SectionHeader } from "../components/layout/SectionHeader";
import { useClientId } from "../components/layout/AppShell";
import { useApi } from "../hooks/useApi";
import { getIssuance } from "../api/client";
import { kbStubs } from "../api/mockData";
import { PRODUCT_TABS, PRODUCT_ID } from "./productTabs";
import "./pages.css";

const columns = [
  { key: "date", label: "Дата", width: 110 },
  { key: "card", label: "Карта" },
  { key: "method", label: "Способ получения", width: 160 },
  { key: "status", label: "Статус", width: 130, render: (row) => <StatusPill status={row.status} /> },
];

/**
 * Единственная вкладка продукта без переключателя карты — речь о
 * заявках на выпуск, а не о состоянии уже активной карты, так что
 * контекст "какая карта выбрана" здесь не имеет смысла (см. договорённость
 * из макета).
 */
export function IssuancePage() {
  const clientId = useClientId();
  const { data, loading } = useApi(() => getIssuance(clientId, PRODUCT_ID), [clientId]);

  return (
    <>
      <SectionHeader title="Дебетовые карты МТС Деньги" tooltip={kbStubs.section} tabs={PRODUCT_TABS} />
      <Card as="div" className="content-panel">
        {loading || !data ? (
          <div className="state-message">Загрузка…</div>
        ) : (
          <>
            <div className="two-col">
              <div className="summary-card summary-card--column">
                <div className="block-title-row">
                  <span className="block-title">Текущая заявка · {data.currentApplication.card}</span>
                  <StatusPill status={data.currentApplication.status} />
                </div>
                <div className="block-subtext">{data.currentApplication.text}</div>
                <div className="block-actions-row">
                  <Button variant="secondary">Изменить способ получения</Button>
                  <Button variant="primary">Активировать карту</Button>
                </div>
              </div>

              <div className="summary-card summary-card--column">
                <div className="block-title-row">
                  <span className="block-title">Оформить новую карту</span>
                  <InfoTooltip title="Условия выпуска" text={kbStubs.issuanceTerms} />
                </div>
                <div className="block-subtext">{data.newCard.text}</div>
                <div className="block-actions-row">
                  <Button variant="secondary">Начать оформление</Button>
                </div>
              </div>
            </div>

            <div>
              <div className="section-title" style={{ marginBottom: 8 }}>
                История заявок
              </div>
              <DataTable columns={columns} rows={data.history} />
            </div>
          </>
        )}

        <EscalationBar />
      </Card>
    </>
  );
}
