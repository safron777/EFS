import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Checkbox } from "./Checkbox";

describe("Checkbox", () => {
  it("вызывает onChange с новым значением при клике", async () => {
    const onChange = vi.fn();
    render(<Checkbox id="agree" label="Согласен" checked={false} onChange={onChange} />);

    await userEvent.click(screen.getByLabelText("Согласен"));
    expect(onChange).toHaveBeenCalledWith(true);
  });

  it("не вызывает onChange, когда disabled", async () => {
    const onChange = vi.fn();
    render(<Checkbox id="agree" label="Согласен" checked={false} onChange={onChange} disabled />);

    await userEvent.click(screen.getByLabelText("Согласен"));
    expect(onChange).not.toHaveBeenCalled();
  });

  it("показывает галочку только когда checked=true", () => {
    const { rerender, container } = render(<Checkbox id="agree" label="Согласен" checked={false} onChange={() => {}} />);
    expect(container.querySelector("svg")).toBeNull();

    rerender(<Checkbox id="agree" label="Согласен" checked onChange={() => {}} />);
    expect(container.querySelector("svg")).not.toBeNull();
  });
});
