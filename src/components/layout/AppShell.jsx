import { Outlet, useOutletContext } from "react-router-dom";
import { RoleBarOffice } from "./RoleBarOffice";
import { IdentityBar } from "./IdentityBar";
import { LeftNav } from "./LeftNav";
import { useApi } from "../../hooks/useApi";
import { getClientCard, getOffer } from "../../api/client";
import "../../styles/global.css";

// Пока один захардкоженный клиент — карточка открывается по клиенту,
// пришедшему к сотруднику; в реальном приложении id берётся из
// поиска/маршрута (например /clients/:clientId/...).
const CLIENT_ID = "client-1";

/**
 * Корневой каркас роли "Офис". Роль-бар — единственное место, которое
 * отличается между ролями; всё остальное (идентификация, каталог,
 * шапка раздела, контент) переиспользуется как есть.
 */
export function AppShellOffice() {
  const { data: client, loading: clientLoading } = useApi(() => getClientCard(CLIENT_ID), []);
  const { data: offer, loading: offerLoading } = useApi(() => getOffer(CLIENT_ID), []);

  if (clientLoading || offerLoading || !client || !offer) {
    return <div className="app-page app-page--loading">Загрузка карточки клиента…</div>;
  }

  return (
    <div className="app-page">
      <RoleBarOffice clientPhone="+7 913 125-19-02" />
      <IdentityBar client={client} offer={offer} onOpenOffer={() => alert("TODO: оформление предложения")} />
      <div className="app-body">
        <LeftNav openMassIncidentsCount={2} />
        <div className="workspace-column">
          <Outlet context={{ clientId: CLIENT_ID }} />
        </div>
      </div>
    </div>
  );
}

/** Хук для страниц: достаёт clientId, переданный через <Outlet context>. */
export function useClientId() {
  return useOutletContext().clientId;
}
