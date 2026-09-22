import "./DataTable.css";

/**
 * columns: [{ key, label, width }]
 * rows: массив объектов; значение колонки со render-функцией рисуется
 *   через columns[].render(row), иначе — row[key] как текст.
 */
export function DataTable({ columns, rows, rowKey = "id" }) {
  return (
    <table className="data-table">
      <thead>
        <tr>
          {columns.map((col) => (
            <th key={col.key} style={{ width: col.width }}>
              {col.label}
            </th>
          ))}
        </tr>
      </thead>
      <tbody>
        {rows.map((row) => (
          <tr key={row[rowKey]}>
            {columns.map((col) => (
              <td key={col.key}>{col.render ? col.render(row) : row[col.key]}</td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  );
}
