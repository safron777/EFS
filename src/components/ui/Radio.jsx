import "./Field.css";
import "./Radio.css";

/** Один вариант. Обычно используется через RadioGroup, а не напрямую. */
export function Radio({ label, name, value, checked = false, onChange, disabled = false, ...rest }) {
  return (
    <label className={`radio ${disabled ? "radio-disabled" : ""}`}>
      <input
        type="radio"
        name={name}
        value={value}
        checked={checked}
        disabled={disabled}
        onChange={() => onChange?.(value)}
        {...rest}
      />
      <span className="radio-dot" aria-hidden="true" />
      {label && <span className="radio-label">{label}</span>}
    </label>
  );
}

/**
 * options: [{ value, label }]. fieldset/legend вместо div/label — группа
 * радио семантически единое поле формы, и это даёт скринридеру общее
 * имя группы без отдельного aria-labelledby.
 */
export function RadioGroup({ label, name, options = [], value, onChange, required = false, error, hint }) {
  return (
    <fieldset className="radio-group">
      {label && (
        <legend className="field-label">
          {label}
          {required && <span className="field-required">*</span>}
        </legend>
      )}
      <div className="radio-group-options">
        {options.map((option) => (
          <Radio
            key={option.value}
            name={name}
            value={option.value}
            label={option.label}
            checked={value === option.value}
            onChange={onChange}
          />
        ))}
      </div>
      {error ? (
        <span className="field-message field-message-error">{error}</span>
      ) : hint ? (
        <span className="field-message field-message-hint">{hint}</span>
      ) : null}
    </fieldset>
  );
}
