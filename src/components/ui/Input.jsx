import "./Field.css";

/**
 * Текстовое поле формы. label оборачивает управление (htmlFor/id не
 * обязательны для связи, но добавлены — пригодятся, если label и
 * input когда-нибудь окажутся не рядом). error важнее hint — обе
 * строки сразу не показываются.
 */
export function Input({ label, id, error, hint, required = false, ...rest }) {
  return (
    <label className="field" htmlFor={id}>
      {label && (
        <span className="field-label">
          {label}
          {required && <span className="field-required">*</span>}
        </span>
      )}
      <input
        id={id}
        className={`field-control ${error ? "field-control-error" : ""}`}
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
