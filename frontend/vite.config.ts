import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "node:path";

// SPA React (remplace Next.js — voir docs/04-structure-repos.md §4.2 pour
// l'historique). Le proxy /api n'agit qu'en dev local : en prod, la Gateway
// route déjà /api vers le backend (helm/portail-tenant/templates/httproute.yaml).
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  server: {
    proxy: {
      "/api": {
        target: process.env.BACKEND_URL ?? "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
});
