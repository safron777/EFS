import { describe, it, expect, vi } from "vitest";
import { renderHook, waitFor, act } from "@testing-library/react";
import { useApi } from "./useApi";

describe("useApi", () => {
  it("стартует в loading, переходит в data после успешного fetch", async () => {
    const fetcher = vi.fn().mockResolvedValue({ foo: "bar" });
    const { result } = renderHook(() => useApi(fetcher, []));

    expect(result.current.loading).toBe(true);
    expect(result.current.data).toBe(null);

    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.data).toEqual({ foo: "bar" });
    expect(result.current.error).toBe(null);
    expect(fetcher).toHaveBeenCalledTimes(1);
  });

  it("кладёт ошибку в error при отклонённом промисе, data остаётся null", async () => {
    const err = new Error("network fail");
    const fetcher = vi.fn().mockRejectedValue(err);
    const { result } = renderHook(() => useApi(fetcher, []));

    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.error).toBe(err);
    expect(result.current.data).toBe(null);
  });

  it("reload() запускает fetcher повторно", async () => {
    const fetcher = vi.fn().mockResolvedValue("ok");
    const { result } = renderHook(() => useApi(fetcher, []));

    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(fetcher).toHaveBeenCalledTimes(1);

    act(() => {
      result.current.reload();
    });

    await waitFor(() => expect(fetcher).toHaveBeenCalledTimes(2));
  });

  it("перезапрашивает данные при изменении deps", async () => {
    const fetcher = vi.fn().mockResolvedValue("ok");
    const { rerender } = renderHook(({ id }) => useApi(fetcher, [id]), {
      initialProps: { id: 1 },
    });

    await waitFor(() => expect(fetcher).toHaveBeenCalledTimes(1));

    rerender({ id: 2 });

    await waitFor(() => expect(fetcher).toHaveBeenCalledTimes(2));
  });

  it("не перезапрашивает данные, если deps не изменились между рендерами", async () => {
    const fetcher = vi.fn().mockResolvedValue("ok");
    const { rerender } = renderHook(({ id }) => useApi(fetcher, [id]), {
      initialProps: { id: 1 },
    });

    await waitFor(() => expect(fetcher).toHaveBeenCalledTimes(1));

    rerender({ id: 1 });

    // Даём шанс лишнему вызову случиться, если бы он был.
    await new Promise((r) => setTimeout(r, 10));
    expect(fetcher).toHaveBeenCalledTimes(1);
  });
});
