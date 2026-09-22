import "./Toggle.css";

export function Toggle({ checked, onChange, label }) {
  return (
    <button
      role="switch"
      aria-checked={checked}
      aria-label={label}
      className={`toggle ${checked ? "toggle-on" : "toggle-off"}`}
      onClick={() => onChange?.(!checked)}
    >
      <span className="toggle-knob" />
    </button>
  );
}
