import { NextResponse } from "next/server";

// Healthcheck du frontend (utilisé par docker-compose, puis par les probes K8s).
export function GET() {
  return NextResponse.json({ status: "ok", service: "autoeecoleconnect-frontend" });
}
