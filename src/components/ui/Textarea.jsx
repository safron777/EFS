import "./Field.css";
import "./Textarea.css";

/** Многострочное текстовое поле — та же оболочка, что у Input. */
export function Textarea({ label, id, error, hint, required = false, rows = 4, ...rest }) {
  return (
    <label className="field" htmlFor={id}>
      {label && (
        <span className="field-label">
          {label}
          {required && <span className="field-required">*</span>}
        </span>
      )}
      <textarea
        id={id}
        rows={rows}
        className={`field-control textarea-control ${error ? "field-control-error" : ""}`}
        aria-invalid={!!error}
        required={required}
        {...rest}
      />
      {error ? (
        <span className="field-message field-message-error">{error}</span>
      ) : hint ? (
        <span className="field-message field-message-hint">{hint}</span>
      ) : null}
    </label>
  );
}
