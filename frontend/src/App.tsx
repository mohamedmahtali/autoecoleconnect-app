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
import ReservationsListe from "@/pages/admin/reservations/ReservationsListe";
import ReservationNouvelle from "@/pages/admin/reservations/ReservationNouvelle";
import ExamensListe from "@/pages/admin/examens/ExamensListe";
import ExamenNouveau from "@/pages/admin/examens/ExamenNouveau";
import ExamenModifier from "@/pages/admin/examens/ExamenModifier";
import MoniteurLayout from "@/pages/moniteur/MoniteurLayout";
import Planning from "@/pages/moniteur/Planning";
import EleveLayout from "@/pages/eleve/EleveLayout";
import ElevePlanning from "@/pages/eleve/Planning";

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
          <Route path="reservations" element={<ReservationsListe />} />
          <Route path="reservations/nouvelle" element={<ReservationNouvelle />} />
          <Route path="examens" element={<ExamensListe />} />
          <Route path="examens/nouveau" element={<ExamenNouveau />} />
          <Route path="examens/:id" element={<ExamenModifier />} />
        </Route>
        <Route
          path="/moniteur"
          element={
            <RequireAuth role="MONITEUR">
              <MoniteurLayout />
            </RequireAuth>
          }
        >
          <Route index element={<Planning />} />
        </Route>
        <Route
          path="/eleve"
          element={
            <RequireAuth role="CLIENT">
              <EleveLayout />
            </RequireAuth>
          }
        >
          <Route index element={<ElevePlanning />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
