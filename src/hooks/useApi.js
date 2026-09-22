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
    // deps и fetcher приходят параметрами (как в useEffect), а не
    // замыкаются напрямую — react-hooks/exhaustive-deps не умеет
    // статически проверить такой проброс. Осознанный компромисс ради
    // единого API useApi(fetcher, deps), см. JSDoc выше и eslint.config.js
    // (там же отключён react-hooks/use-memo по той же причине).
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  useEffect(() => load(), [load]);

  return { ...state, reload: load };
}
