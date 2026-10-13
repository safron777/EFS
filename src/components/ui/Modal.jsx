import { useEffect, useRef } from "react";
import { createPortal } from "react-dom";
import { X } from "lucide-react";
import "./Modal.css";

/**
 * Базовое модальное окно: оверлей + панель с заголовком, телом и
 * опциональным футером действий (обычно пара <Button>). Рендерится
 * через портал в document.body — не зависит от z-index/overflow
 * родителя (карточки используют overflow для теней, см. Card.css).
 *
 * Закрывается по Escape и клику по подложке; dismissable=false
 * отключает оба способа — для форм, где случайное закрытие потеряло
 * бы ввод пользователя без явного подтверждения.
 */
export function Modal({ open, onClose, title, children, footer, dismissable = true }) {
  const panelRef = useRef(null);

  useEffect(() => {
    if (!open) return;

    function handleKeyDown(event) {
      if (event.key === "Escape" && dismissable) {
        onClose?.();
      }
    }

    document.addEventListener("keydown", handleKeyDown);
    panelRef.current?.focus();
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [open, dismissable, onClose]);

  if (!open) return null;

  return createPortal(
    <div className="modal-overlay" onMouseDown={() => dismissable && onClose?.()}>
      <div
        ref={panelRef}
        className="modal-panel"
        role="dialog"
        aria-modal="true"
        aria-label={title}
        tabIndex={-1}
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="modal-header">
          <h2 className="modal-title">{title}</h2>
          <button type="button" className="modal-close" onClick={() => onClose?.()} aria-label="Закрыть">
            <X size={18} />
          </button>
        </div>
        <div className="modal-body">{children}</div>
        {footer && <div className="modal-footer">{footer}</div>}
      </div>
    </div>,
    document.body
  );
}
