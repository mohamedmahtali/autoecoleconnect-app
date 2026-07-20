import { useNavigate } from "react-router-dom";

import { clearToken } from "@/lib/session";

// Plus de cookie httpOnly à effacer côté serveur : le token n'existe qu'en
// sessionStorage, la déconnexion est donc purement locale.
export function BoutonDeconnexion() {
  const navigate = useNavigate();

  function seDeconnecter() {
    clearToken();
    navigate("/login");
  }

  return (
    <button className="bouton-secondaire" onClick={seDeconnecter}>
      Se déconnecter
    </button>
  );
}
