import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Image minimale pour Docker/K8s : le build embarque son propre serveur Node
  output: "standalone",
};

export default nextConfig;
