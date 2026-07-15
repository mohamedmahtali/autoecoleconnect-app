"use client";

export function BoutonDeconnexion() {
  async function seDeconnecter() {
    await fetch("/api/auth/logout", { method: "POST" });
    window.location.href = "/login";
  }

  return (
    <button className="bouton-secondaire" onClick={seDeconnecter}>
      Se déconnecter
    </button>
  );
}
