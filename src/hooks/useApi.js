import { useEffect, useState, useCallback } from "react";

/**
 * Универсальный хук для вызовов src/api/client.js.
 *
 * const { data, loading, error, reload } = useApi(() => getOperations(cardId), [cardId]);
 *
 * fetcher должен быть стабильной функцией без аргументов (оборачивайте
 * вызов в стрелочную функцию на месте использования); deps — как в useEffect.
 */
export function useApi(fetcher, deps = []) {
  const [state, setState] = useState({ data: null, loading: true, error: null });

  const load = useCallback(() => {
    let cancelled = false;
    setState((s) => ({ ...s, loading: true, error: null }));
    fetcher()
      .then((data) => {
        if (!cancelled) setState({ data, loading: false, error: null });
      })
      .catch((error) => {
        if (!cancelled) setState({ data: null, loading: false, error });
      });
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  useEffect(() => load(), [load]);

  return { ...state, reload: load };
}
