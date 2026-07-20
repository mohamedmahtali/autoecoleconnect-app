import { BrowserRouter, Route, Routes } from "react-router-dom";

import Home from "@/pages/Home";
import Login from "@/pages/Login";
import { RequireAuth } from "@/routes/RequireAuth";
import AdminLayout from "@/pages/admin/AdminLayout";
import Dashboard from "@/pages/admin/Dashboard";
import ClientsListe from "@/pages/admin/clients/ClientsListe";
import ClientNouveau from "@/pages/admin/clients/ClientNouveau";
import MoniteursListe from "@/pages/admin/moniteurs/MoniteursListe";
import VoituresListe from "@/pages/admin/voitures/VoituresListe";
import ForfaitsListe from "@/pages/admin/forfaits/ForfaitsListe";

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/login" element={<Login />} />
        <Route
          path="/admin"
          element={
            <RequireAuth role="DIRECTEUR">
              <AdminLayout />
            </RequireAuth>
          }
        >
          <Route index element={<Dashboard />} />
          <Route path="clients" element={<ClientsListe />} />
          <Route path="clients/nouveau" element={<ClientNouveau />} />
          <Route path="moniteurs" element={<MoniteursListe />} />
          <Route path="voitures" element={<VoituresListe />} />
          <Route path="forfaits" element={<ForfaitsListe />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
