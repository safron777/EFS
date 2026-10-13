import { ChevronDown } from "lucide-react";
import "./Field.css";
import "./Select.css";

/**
 * options: [{ value, label }]. placeholder рисуется как первый,
 * неотмечаемый option — тот же приём, что в обычных формах, без
 * отдельного "пустого" состояния компонента.
 */
export function Select({ label, id, error, hint, required = false, options = [], placeholder, ...rest }) {
  return (
    <label className="field" htmlFor={id}>
      {label && (
        <span className="field-label">
          {label}
          {required && <span className="field-required">*</span>}
        </span>
      )}
      <span className="select-wrap">
        <select
          id={id}
          className={`field-control select-control ${error ? "field-control-error" : ""}`}
          aria-invalid={!!error}
          required={required}
          {...rest}
        >
          {placeholder && (
            <option value="" disabled>
              {placeholder}
            </option>
          )}
          {options.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
        <ChevronDown className="select-chevron" size={16} aria-hidden="true" />
      </span>
      {error ? (
        <span className="field-message field-message-error">{error}</span>
      ) : hint ? (
        <span className="field-message field-message-hint">{hint}</span>
      ) : null}
    </label>
  );
}
