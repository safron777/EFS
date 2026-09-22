import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { StatusPill, STATUS_TONE } from "./Pill";

describe("StatusPill", () => {
  it.each(Object.entries(STATUS_TONE))('маппит статус "%s" на тон "%s"', (status, tone) => {
    render(<StatusPill status={status} />);
    const el = screen.getByText(status);
    expect(el.className).toContain(`pill-${tone}`);
  });

  it("для статуса, которого нет в STATUS_TONE, использует neutral", () => {
    render(<StatusPill status="Совсем новый статус" />);
    const el = screen.getByText("Совсем новый статус");
    expect(el.className).toContain("pill-neutral");
  });
});
