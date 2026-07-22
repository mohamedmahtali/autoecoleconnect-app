import { StrictMode } from "react";
import { createRoot } from "react-dom/client";

import App from "./App";
import { consommerJetonDeLUrl } from "@/lib/session";
import "./styles/globals.css";

// Avant le premier rendu : un gérant arrivant depuis son tableau de bord
// porte son jeton dans le fragment de l'URL. Le consommer ici évite que
// l'application s'affiche déconnectée puis bascule (docs/18 §18.3 lot 5).
consommerJetonDeLUrl();

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
