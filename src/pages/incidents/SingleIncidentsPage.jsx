import { Card } from "../../components/ui/Card";
import { Button } from "../../components/ui/Button";
import { StatusPill } from "../../components/ui/Pill";
import { DataTable } from "../../components/ui/DataTable";
import { SectionHeader } from "../../components/layout/SectionHeader";
import { useClientId } from "../../components/layout/AppShell";
import { useApi } from "../../hooks/useApi";
import { getSingleIncidents, createSingleIncident } from "../../api/client";
import { kbStubs } from "../../api/mockData";
import { INCIDENT_TABS } from "../productTabs";
import "../pages.css";

const columns = [
  { key: "id", label: "№", width: 110 },
  { key: "theme", label: "Тема" },
  { key: "created", label: "Создан", width: 120 },
  { key: "line", label: "Линия", width: 100 },
  { key: "status", label: "Статус", width: 120, render: (row) => <StatusPill status={row.status} /> },
];

export function SingleIncidentsPage() {
  const clientId = useClientId();
  const { data: incidents, loading, reload } = useApi(() => getSingleIncidents(clientId), [clientId]);

  async function handleCreate() {
    // TODO: заменить на модалку с формой (тема, приоритет, описание,
    // вложения) — см. README, пункт про обязательные поля создания ЕИ.
    await createSingleIncident(clientId, { theme: "Новый единичный инцидент" });
    reload();
  }

  return (
    <>
      <SectionHeader title="Инциденты и обращения" tooltip={kbStubs.incidentsRegulations} tabs={INCIDENT_TABS} />
      <Card as="div" className="content-panel">
        <div className="top-actions-row">
          <p>Единичные инциденты, созданные по этому клиенту.</p>
          <Button variant="primary" onClick={handleCreate}>
            + Создать ЕИ
          </Button>
        </div>

        {loading || !incidents ? (
          <div className="state-message">Загрузка…</div>
        ) : (
          <DataTable columns={columns} rows={incidents} />
        )}
      </Card>
    </>
  );
}
