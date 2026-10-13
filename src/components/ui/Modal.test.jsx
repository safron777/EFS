import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Modal } from "./Modal";

describe("Modal", () => {
  it("ничего не рендерит, когда open=false", () => {
    render(
      <Modal open={false} onClose={() => {}} title="Заголовок">
        Содержимое
      </Modal>
    );
    expect(screen.queryByRole("dialog")).toBeNull();
  });

  it("рендерит title и children, когда open=true", () => {
    render(
      <Modal open onClose={() => {}} title="Заголовок">
        Содержимое
      </Modal>
    );
    expect(screen.getByRole("dialog")).toBeInTheDocument();
    expect(screen.getByText("Содержимое")).toBeInTheDocument();
  });

  it("вызывает onClose по Escape", async () => {
    const onClose = vi.fn();
    render(
      <Modal open onClose={onClose} title="Заголовок">
        Содержимое
      </Modal>
    );
    await userEvent.keyboard("{Escape}");
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it("вызывает onClose по клику на подложку, но не по клику на панель", async () => {
    const onClose = vi.fn();
    render(
      <Modal open onClose={onClose} title="Заголовок">
        Содержимое
      </Modal>
    );
    await userEvent.click(screen.getByText("Содержимое"));
    expect(onClose).not.toHaveBeenCalled();

    await userEvent.click(screen.getByRole("dialog").parentElement);
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it("не закрывается по Escape/подложке, когда dismissable=false", async () => {
    const onClose = vi.fn();
    render(
      <Modal open onClose={onClose} title="Заголовок" dismissable={false}>
        Содержимое
      </Modal>
    );
    await userEvent.keyboard("{Escape}");
    await userEvent.click(screen.getByRole("dialog").parentElement);
    expect(onClose).not.toHaveBeenCalled();
  });

  it("рендерит close-кнопку и footer", async () => {
    const onClose = vi.fn();
    render(
      <Modal open onClose={onClose} title="Заголовок" footer={<button>Готово</button>}>
        Содержимое
      </Modal>
    );
    expect(screen.getByText("Готово")).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "Закрыть" }));
    expect(onClose).toHaveBeenCalledTimes(1);
  });
});
