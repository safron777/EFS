import js from "@eslint/js";
import reactHooks from "eslint-plugin-react-hooks";
import reactRefresh from "eslint-plugin-react-refresh";
import globals from "globals";
import eslintConfigPrettier from "eslint-config-prettier";

export default [
  { ignores: ["dist", "node_modules"] },
  js.configs.recommended,
  {
    files: ["**/*.{js,jsx}"],
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: "module",
      parserOptions: { ecmaFeatures: { jsx: true } },
      globals: globals.browser,
    },
    plugins: {
      "react-hooks": reactHooks,
      "react-refresh": reactRefresh,
    },
    rules: {
      ...reactHooks.configs.recommended.rules,
      "react-refresh/only-export-components": ["warn", { allowConstantExport: true }],
      // Неиспользуемые переменные — предупреждение, а не ошибка: сборку
      // (npm run build) это не блокирует, но видно в CI/редакторе.
      "no-unused-vars": ["warn", { argsIgnorePattern: "^_" }],
    },
  },
  {
    // useApi() намеренно принимает deps как параметр функции (аналог
    // useEffect API) и прокидывает его в useCallback — единственная точка
    // входа всего дата-фетчинга в проекте (10+ использований в pages/*).
    // react-hooks/use-memo требует deps-литерал прямо в вызове и не умеет
    // статически проверить такой проброс; переписывать сложившийся API
    // ради этого правила несоразмерно, отключаем точечно для файла.
    files: ["src/hooks/useApi.js"],
    rules: { "react-hooks/use-memo": "off" },
  },
  eslintConfigPrettier,
];
