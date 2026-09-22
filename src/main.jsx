import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App";
// Самохостинг IBM Plex Sans вместо fonts.googleapis.com/fonts.gstatic.com —
// раньше банковский внутренний инструмент на каждой загрузке слал IP/UA
// сотрудника третьей стороне (Google), плюс это лишний внешний домен
// в CSP (см. index.html). Веса — те же, что были в @import из Google Fonts.
import "@fontsource/ibm-plex-sans/400.css";
import "@fontsource/ibm-plex-sans/500.css";
import "@fontsource/ibm-plex-sans/600.css";
import "@fontsource/ibm-plex-sans/700.css";
import "./styles/global.css";

ReactDOM.createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
