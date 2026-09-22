import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { AppShellOffice } from "./components/layout/AppShell";
import { BalancePage } from "./pages/BalancePage";
import { OperationsPage } from "./pages/OperationsPage";
import { BlocksPage } from "./pages/BlocksPage";
import { CashbackPage } from "./pages/CashbackPage";
import { NotificationsPage } from "./pages/NotificationsPage";
import { IssuancePage } from "./pages/IssuancePage";
import { MassIncidentsPage } from "./pages/incidents/MassIncidentsPage";
import { SingleIncidentsPage } from "./pages/incidents/SingleIncidentsPage";
import { ComplaintsPage } from "./pages/incidents/ComplaintsPage";

/**
 * Маршруты роли "Офис". Когда добавятся роли КЦ/Чат, у них будет
 * свой AppShellVoice/AppShellChat с тем же набором дочерних роутов —
 * проще всего вынести <Route> детей в общий массив и переиспользовать
 * под каждым AppShell (см. README).
 */
export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<AppShellOffice />}>
          <Route index element={<Navigate to="/products/debit-mts-dengi/balance" replace />} />

          <Route path="products/debit-mts-dengi/balance" element={<BalancePage />} />
          <Route path="products/debit-mts-dengi/operations" element={<OperationsPage />} />
          <Route path="products/debit-mts-dengi/blocks" element={<BlocksPage />} />
          <Route path="products/debit-mts-dengi/cashback" element={<CashbackPage />} />
          <Route path="products/debit-mts-dengi/notifications" element={<NotificationsPage />} />
          <Route path="products/debit-mts-dengi/issuance" element={<IssuancePage />} />

          <Route path="incidents/mass" element={<MassIncidentsPage />} />
          <Route path="incidents/single" element={<SingleIncidentsPage />} />
          <Route path="incidents/complaints" element={<ComplaintsPage />} />

          <Route path="*" element={<Navigate to="/products/debit-mts-dengi/balance" replace />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
