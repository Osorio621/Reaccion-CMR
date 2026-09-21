// ============================================================
// CRM -> Sistema de Tickets: recepcion de clientes
//
// Ubicacion en tu proyecto Next.js (App Router):
//   app/api/clientes/route.ts
//
// El CRM hace POST con header:  X-API-Key: <valor compartido>
// Responde: { ok: true, ticketClienteId: <id> }
// ============================================================

import { NextRequest, NextResponse } from "next/server";
import { z } from "zod";
import { prisma } from "@/lib/prisma"; // ajusta al acceso a BD de tu proyecto

const ClienteCrmSchema = z.object({
  crmClienteId: z.number().int().positive(),
  nombre: z.string().min(1).max(200),
  razonSocial: z.string().max(200).nullable().optional(),
  rfc: z.string().max(20).nullable().optional(),
  email: z.string().email().max(150).nullable().optional(),
  telefono: z.string().max(20).nullable().optional(),
  direccion: z.string().max(300).nullable().optional(),
  ciudad: z.string().max(100).nullable().optional(),
  estadoRegion: z.string().max(100).nullable().optional(),
  sitioWeb: z.string().max(300).nullable().optional(),
  ejecutivoEmail: z.string().email().nullable().optional(),
  origen: z.literal("CRM_REACTIVOS_DEL_VALLE"),
  fechaEnvio: z.string(), // ISO-8601
});

function sinAutorizacion() {
  return NextResponse.json(
    { ok: false, error: "X-API-Key invalida o ausente" },
    { status: 401 }
  );
}

export async function POST(req: NextRequest) {
  const apiKey = req.headers.get("x-api-key");
  if (!apiKey || apiKey !== process.env.CRM_API_KEY) {
    return sinAutorizacion();
  }

  let body: unknown;
  try {
    body = await req.json();
  } catch {
    return NextResponse.json({ ok: false, error: "JSON invalido" }, { status: 400 });
  }

  const parsed = ClienteCrmSchema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json(
      { ok: false, error: "Datos invalidos", detalles: parsed.error.flatten().fieldErrors },
      { status: 400 }
    );
  }
  const c = parsed.data;

  // Upsert: si el cliente ya fue enviado antes, se actualiza en lugar de duplicar.
  try {
    const cliente = await prisma.cliente.upsert({
      where: { crmClienteId: c.crmClienteId },
      update: {
        nombre: c.nombre,
        razonSocial: c.razonSocial ?? null,
        email: c.email ?? null,
        telefono: c.telefono ?? null,
        direccion: c.direccion ?? null,
        ciudad: c.ciudad ?? null,
        sincronizadoEn: new Date(),
      },
      create: {
        crmClienteId: c.crmClienteId,
        nombre: c.nombre,
        razonSocial: c.razonSocial ?? null,
        email: c.email ?? null,
        telefono: c.telefono ?? null,
        direccion: c.direccion ?? null,
        ciudad: c.ciudad ?? null,
        origen: c.origen,
      },
    });

    return NextResponse.json({ ok: true, ticketClienteId: cliente.id }, { status: 201 });
  } catch (e) {
    console.error("[api/clientes] error guardando cliente del CRM:", e);
    return NextResponse.json(
      { ok: false, error: "Error interno guardando el cliente" },
      { status: 500 }
    );
  }
}
