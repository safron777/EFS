import { useState } from "react";
import { Card } from "../components/ui/Card";
import { Button } from "../components/ui/Button";
import { Input } from "../components/ui/Input";
import { Textarea } from "../components/ui/Textarea";
import { Select } from "../components/ui/Select";
import { Checkbox } from "../components/ui/Checkbox";
import { RadioGroup } from "../components/ui/Radio";
import { Modal } from "../components/ui/Modal";
import "./DesignKitPage.css";

/**
 * Витрина новых примитивов формы (Modal, Input, Textarea, Select,
 * Checkbox, Radio) — стартовый набор дизайн-кита, вне AppShellOffice,
 * без зависимости от клиента/бэкенда, чтобы её можно было открыть
 * и проверить изолированно. Не часть ролевых маршрутов приложения.
 */
export function DesignKitPage() {
  const [checked, setChecked] = useState(true);
  const [priority, setPriority] = useState("normal");
  const [modalOpen, setModalOpen] = useState(false);

  return (
    <div className="design-kit">
      <h1>Дизайн-кит — стартовый набор</h1>
      <p className="design-kit-intro">
        Новые примитивы форм поверх существующих токенов (<code>src/styles/tokens.css</code>). Закрывают TODO из
        README про модалки создания ЕИ/жалобы — пока только как компоненты, без подключения к страницам (список
        обязательных полей формы ещё не определён продуктом).
      </p>

      <Card as="section" className="design-kit-section">
        <h2>Input</h2>
        <div className="design-kit-grid">
          <Input id="theme" label="Тема" placeholder="Коротко опишите проблему" required />
          <Input id="theme-error" label="Тема" defaultValue="" error="Заполните тему обращения" required />
          <Input id="theme-hint" label="Тема" hint="Видно только сотруднику банка" />
          <Input id="theme-disabled" label="Тема" defaultValue="Недоступно для правки" disabled />
        </div>
      </Card>

      <Card as="section" className="design-kit-section">
        <h2>Textarea</h2>
        <div className="design-kit-grid">
          <Textarea id="description" label="Описание" placeholder="Что произошло, когда, что уже пробовали" />
          <Textarea id="description-error" label="Описание" error="Минимум 10 символов" />
        </div>
      </Card>

      <Card as="section" className="design-kit-section">
        <h2>Select</h2>
        <div className="design-kit-grid">
          <Select
            id="line"
            label="Линия поддержки"
            placeholder="Выберите линию"
            options={[
              { value: "1", label: "1 линия" },
              { value: "2", label: "2 линия" },
            ]}
          />
          <Select
            id="line-error"
            label="Линия поддержки"
            placeholder="Выберите линию"
            error="Обязательное поле"
            options={[
              { value: "1", label: "1 линия" },
              { value: "2", label: "2 линия" },
            ]}
          />
        </div>
      </Card>

      <Card as="section" className="design-kit-section">
        <h2>Checkbox</h2>
        <div className="design-kit-grid design-kit-grid-inline">
          <Checkbox id="notify" label="Уведомить клиента по SMS" checked={checked} onChange={setChecked} />
          <Checkbox id="notify-disabled" label="Недоступно" checked={false} onChange={() => {}} disabled />
        </div>
      </Card>

      <Card as="section" className="design-kit-section">
        <h2>Radio / RadioGroup</h2>
        <RadioGroup
          label="Приоритет"
          name="priority"
          value={priority}
          onChange={setPriority}
          options={[
            { value: "low", label: "Низкий" },
            { value: "normal", label: "Обычный" },
            { value: "high", label: "Высокий" },
          ]}
        />
      </Card>

      <Card as="section" className="design-kit-section">
        <h2>Modal</h2>
        <Button variant="primary" onClick={() => setModalOpen(true)}>
          Открыть модалку
        </Button>
        <Modal
          open={modalOpen}
          onClose={() => setModalOpen(false)}
          title="Новый единичный инцидент"
          footer={
            <>
              <Button variant="secondary" onClick={() => setModalOpen(false)}>
                Отмена
              </Button>
              <Button variant="primary" onClick={() => setModalOpen(false)}>
                Создать
              </Button>
            </>
          }
        >
          <Input id="modal-theme" label="Тема" placeholder="Коротко опишите проблему" required />
          <Textarea id="modal-description" label="Описание" placeholder="Подробности" />
        </Modal>
      </Card>
    </div>
  );
}
