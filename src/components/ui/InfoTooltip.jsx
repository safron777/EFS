import "./InfoTooltip.css";

/**
 * Иконка (i) с подсказкой. Сейчас показывает статичный текст —
 * это ровно то место, куда позже подключится конкретная статья
 * базы знаний по articleId (см. TODO в JSDoc ниже).
 *
 * @param {string} title - заголовок подсказки
 * @param {string} text - текст-заглушка, пока не подключена база знаний
 * @param {string} [articleId] - TODO: когда появится интеграция с базой
 *   знаний, сюда передаётся id статьи, и компонент вместо статичного
 *   текста показывает fetchArticle(articleId) или ссылку на неё.
 * @param {'default'|'danger'} [tone]
 */
export function InfoTooltip({ title, text, articleId, tone = "default" }) {
  return (
    <span className="info-tooltip" data-article-id={articleId}>
      <span className={`info-icon info-icon-${tone}`} tabIndex={0} role="button" aria-label={title}>
        i
      </span>
      <span className="info-tooltip-bubble" role="tooltip">
        <b>{title}</b>
        <span>{text}</span>
      </span>
    </span>
  );
}
