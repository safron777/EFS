import { Card } from "../../components/ui/Card";
import { Button } from "../../components/ui/Button";
import { StatusPill } from "../../components/ui/Pill";
import { DataTable } from "../../components/ui/DataTable";
import { SectionHeader } from "../../components/layout/SectionHeader";
import { useClientId } from "../../components/layout/AppShell";
import { useApi } from "../../hooks/useApi";
import { getComplaints, createComplaint } from "../../api/client";
import { kbStubs } from "../../api/mockData";
import { INCIDENT_TABS } from "../productTabs";
import "../pages.css";

const columns = [
  { key: "id", label: "№", width: 160 },
  { key: "theme", label: "Тема" },
  { key: "created", label: "Создана", width: 120 },
  { key: "term", label: "Срок", width: 90 },
  { key: "status", label: "Статус", width: 140, render: (row) => <StatusPill status={row.status} /> },
];

export function ComplaintsPage() {
  const clientId = useClientId();
  const { data: complaints, loading, reload } = useApi(() => getComplaints(clientId), [clientId]);

  async function handleCreate() {
    // TODO: заменить на модалку с формой жалобы/претензии — обязательные
    // поля уточнить у продукта (см. README).
    await createComplaint(clientId, { theme: "Новая жалоба" });
    reload();
  }

  return (
    <>
      <SectionHeader title="Инциденты и обращения" tooltip={kbStubs.incidentsRegulations} tabs={INCIDENT_TABS} />
      <Card as="div" className="content-panel">
        <div className="top-actions-row">
          <p>Жалобы и претензии, оставленные по этому клиенту.</p>
          <Button variant="danger-solid" onClick={handleCreate}>
            + Оставить жалобу
          </Button>
        </div>

        {loading || !complaints ? (
          <div className="state-message">Загрузка…</div>
        ) : (
          <DataTable columns={columns} rows={complaints} />
        )}
      </Card>
    </>
  );
}
