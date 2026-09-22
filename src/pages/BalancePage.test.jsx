import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { createMemoryRouter, RouterProvider, Outlet } from "react-router-dom";
import { BalancePage } from "./BalancePage";

// BalancePage читает clientId через useClientId() -> useOutletContext(),
// который AppShellOffice в реальном приложении прокидывает через
// <Outlet context={{ clientId }} />. Здесь воспроизводим тот же контракт
// минимальной обёрткой, не поднимая весь AppShell с его собственными
// fetch'ами клиента/оффера.
function TestShell() {
  return <Outlet context={{ clientId: "client-1" }} />;
}

function renderBalancePage() {
  const router = createMemoryRouter(
    [{ path: "/", element: <TestShell />, children: [{ index: true, element: <BalancePage /> }] }],
    { initialEntries: ["/"] }
  );
  return render(<RouterProvider router={router} />);
}

describe("BalancePage", () => {
  it("показывает загрузку, затем основную карту с балансом и действиями роли «Офис»", async () => {
    renderBalancePage();

    expect(screen.getByText("Загрузка…")).toBeInTheDocument();

    // getCards/getDocuments в api/client.js — заглушки с искусственной
    // задержкой сети (см. fetchJson), поэтому ждём реальных ~350мс.
    await screen.findByText(/12.430 ₽/, {}, { timeout: 2000 });

    expect(screen.getByText("Zero")).toBeInTheDocument();
    expect(screen.getByText("Основная")).toBeInTheDocument();
    // canReissue/canPrint захардкожены true для роли "Офис" (см. BalancePage.jsx)
    expect(screen.getByText("Перевыпустить карту")).toBeInTheDocument();
    expect(screen.getAllByText("Распечатать").length).toBeGreaterThan(0);
  });
});
