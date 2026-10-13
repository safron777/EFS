import { Check } from "lucide-react";
import "./Checkbox.css";

/**
 * Нативный input спрятан, но не удалён (position: absolute + opacity: 0
 * в CSS) — клавиатура и скринридеры работают с ним как обычно, видимый
 * квадрат — соседний span. Галочка — иконка lucide, а не CSS-рисование,
 * чтобы не заводить вторую технику отрисовки значков в проекте.
 */
export function Checkbox({ label, id, checked = false, onChange, disabled = false, ...rest }) {
  return (
    <label className={`checkbox ${disabled ? "checkbox-disabled" : ""}`} htmlFor={id}>
      <input
        type="checkbox"
        id={id}
        checked={checked}
        disabled={disabled}
        onChange={(event) => onChange?.(event.target.checked)}
        {...rest}
      />
      <span className="checkbox-box" aria-hidden="true">
        {checked && <Check size={14} strokeWidth={3} />}
      </span>
      {label && <span className="checkbox-label">{label}</span>}
    </label>
  );
}
