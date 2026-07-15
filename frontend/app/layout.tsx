import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "AutoEcoleConnect",
  description: "Plateforme SaaS multi-tenant pour auto-écoles",
};

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="fr">
      <body>{children}</body>
    </html>
  );
}
