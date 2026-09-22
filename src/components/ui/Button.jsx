import "./Button.css";

/**
 * Базовая кнопка. variant задаёт смысл действия, а не просто цвет:
 * - primary   — основное действие модуля ("Обменять на рубли", "Оформить")
 * - secondary — второстепенное действие ("Реквизиты", "Отправить")
 * - danger    — деструктивное или эскалационное действие ("Заблокировать",
 *               "Оставить жалобу")
 */
export function Button({ variant = "secondary", size = "md", icon, children, ...rest }) {
  return (
    <button className={`btn btn-${variant} btn-${size}`} {...rest}>
      {icon}
      {children}
    </button>
  );
}
