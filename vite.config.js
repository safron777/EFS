import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// CSP как defense-in-depth на случай будущего XSS — только для
// продакшен-сборки (apply: "build"). В dev Vite подключает CSS через
// рантайм-инжект <style> (HMR), а не через <link>, и строгий style-src
// 'self' это ломает — страница рендерится вообще без стилей (см. index.html
// для истории вопроса). В dist/index.html инлайновых <script>/<style> нет
// вообще (весь JS/CSS — внешние бандлы), поэтому в проде можно позволить
// CSP без единого unsafe-inline. При реальном деплое стоит продублировать
// те же правила HTTP-заголовком Content-Security-Policy на сервере/прокси —
// meta-CSP слабее (не защищает контент до самого тега, нет report-uri).
function cspMetaForBuild() {
  const content =
    "default-src 'self'; script-src 'self'; style-src 'self'; font-src 'self'; " +
    "img-src 'self' data:; connect-src 'self'; object-src 'none'; base-uri 'self'; frame-ancestors 'self'";
  return {
    name: "csp-meta-for-build",
    apply: "build",
    transformIndexHtml(html) {
      return {
        html,
        tags: [
          {
            tag: "meta",
            attrs: { "http-equiv": "Content-Security-Policy", content },
            injectTo: "head-prepend",
          },
        ],
      };
    },
  };
}

export default defineConfig({
  plugins: [react(), cspMetaForBuild()],
  server: {
    port: 5173,
  },
  test: {
    environment: "jsdom",
    setupFiles: "./src/setupTests.js",
    globals: true,
  },
});
