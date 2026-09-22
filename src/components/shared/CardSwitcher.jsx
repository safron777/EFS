import "./CardSwitcher.css";

/**
 * "КАРТА: ..." — переключатель контекста карты. Показывается на всех
 * вкладках, где данные относятся к конкретной карте (Операции,
 * Блокировки, Кэшбэк, Уведомления), но не на "Баланс и карты"
 * (там и так видны сразу все карты) и не на "Оформление"
 * (речь о заявках, а не о конкретной активной карте).
 */
export function CardSwitcher({ cards, activeCardId, onSelect }) {
  return (
    <div className="card-switcher">
      <span className="card-switcher__label">Карта:</span>
      {cards.map((c) => {
        const active = c.id === activeCardId;
        return (
          <button
            key={c.id}
            className={`card-switcher__chip ${active ? "is-active" : ""}`}
            onClick={() => onSelect(c.id)}
            aria-pressed={active}
          >
            <span className={`card-switcher__dot ${c.status === "active" ? "is-active-dot" : ""}`} />
            {c.product} · {c.masked}
            {c.status === "closed" && " · закрыта"}
          </button>
        );
      })}
    </div>
  );
}
