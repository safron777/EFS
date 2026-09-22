import { Card } from "../../components/ui/Card";
import { Button } from "../../components/ui/Button";
import { StatusPill } from "../../components/ui/Pill";
import { DataTable } from "../../components/ui/DataTable";
import { SectionHeader } from "../../components/layout/SectionHeader";
import { useClientId } from "../../components/layout/AppShell";
import { useApi } from "../../hooks/useApi";
import { getMassIncidents, attachClientToIncident } from "../../api/client";
import { kbStubs } from "../../api/mockData";
import { INCIDENT_TABS } from "../productTabs";
import "../pages.css";

/**
 * Массовые инциденты приходят по интеграции из системы мониторинга —
 * этот экран только читает и позволяет привязать текущего клиента,
 * сотрудник не создаёт МИ вручную (в отличие от ЕИ и жалоб).
 */
export function MassIncidentsPage() {
  const clientId = useClientId();
  const { data: incidents, loading, reload } = useApi(() => getMassIncidents(), []);

  async function handleAttach(incidentId) {
    await attachClientToIncident(clientId, incidentId);
    reload();
  }

  const columns = [
    { key: "id", label: "ID", width: 80 },
    { key: "theme", label: "Тема" },
    { key: "product", label: "Продукт", width: 220 },
    { key: "start", label: "Начало", width: 100 },
    { key: "status", label: "Статус", width: 120, render: (row) => <StatusPill status={row.status} /> },
    { key: "affected", label: "Затронуто", width: 130 },
    {
      key: "action",
      label: "",
      width: 170,
      render: (row) =>
        row.status === "Устранён" ? (
          <Button variant="secondary" disabled>
            Устранён
          </Button>
        ) : (
          <Button variant={row.matchesClient ? "primary" : "secondary"} onClick={() => handleAttach(row.id)}>
            Привязать клиента
          </Button>
        ),
    },
  ];

  const matched = incidents?.find((i) => i.matchesClient && i.status !== "Устранён");

  return (
    <>
      <SectionHeader title="Инциденты и обращения" tooltip={kbStubs.incidentsRegulations} tabs={INCIDENT_TABS} />
      <Card as="div" className="content-panel">
        <div className="top-actions-row">
          <p>Список массовых инцидентов формируется автоматически по интеграции. Ниже — активные на сегодня.</p>
          <Button variant="secondary" onClick={reload}>
            Обновить список
          </Button>
        </div>

        {loading || !incidents ? (
          <div className="state-message">Загрузка…</div>
        ) : (
          <>
            <DataTable columns={columns} rows={incidents} />

            {matched && (
              <div className="alert-box alert-box--danger">
                <div className="alert-box__text alert-box__text--danger">
                  <b>Похоже на совпадение:</b>
                  у клиента есть отклонённый перевод по СБП 12.09 — возможно, относится к {matched.id}.
                </div>
                <Button variant="danger" onClick={() => handleAttach(matched.id)}>
                  Привязать к {matched.id}
                </Button>
              </div>
            )}
          </>
        )}
      </Card>
    </>
  );
}
