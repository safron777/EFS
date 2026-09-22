import { useState } from "react";
import { useApi } from "./useApi";
import { getCards } from "../api/client";
import { defaultCardId } from "../api/mockData";

/**
 * Общий паттерн для вкладок продукта, где данные привязаны к конкретной
 * карте (Операции, Блокировки, Кэшбэк, Уведомления): подгружает список
 * карт клиента, держит activeCardId (переключается через <CardSwitcher>)
 * и подгружает card-scoped данные через переданный fetcher(cardId).
 * Раньше эти 4 строки (getCards + useState + второй useApi) были
 * скопированы в 4 файла отдельно.
 *
 * fetcher, как и в useApi, — стрелочная функция вида (cardId) => getXxx(cardId);
 * не обязана быть стабильной по ссылке между рендерами, см. useApi.js —
 * достаточно, чтобы она не замыкала ничего, кроме activeCardId (тот и так
 * в deps).
 *
 * const { cards, activeCardId, setActiveCardId, data, loading, reload } =
 *   useCardScopedData(clientId, PRODUCT_ID, (cardId) => getOperations(cardId));
 */
export function useCardScopedData(clientId, productId, fetcher) {
  const { data: cards } = useApi(() => getCards(clientId, productId), [clientId, productId]);
  const [activeCardId, setActiveCardId] = useState(defaultCardId);
  const { data, loading, reload } = useApi(() => fetcher(activeCardId), [activeCardId]);

  return { cards, activeCardId, setActiveCardId, data, loading, reload };
}
