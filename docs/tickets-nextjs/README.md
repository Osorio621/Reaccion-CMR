# Integración CRM → Sistema de Tickets

Flujo: un usuario presiona **"Enviar cliente a tickets"** en la ficha de un
cliente del CRM; el CRM manda los datos por HTTPS a este endpoint de tu
proyecto Next.js. Si el sistema de tickets está caído, el CRM marca el envío
como ERROR y lo reintenta automáticamente cada 5 minutos (hasta 5 veces).

```
CRM (Spring Boot) ──POST /api/clientes──► Tickets (Next.js)
                        X-API-Key: secreto-compartido
◄── { ok: true, ticketClienteId: 555 } ──
```

## Qué instalar en el proyecto Next.js

1. Copia `app/api/clientes/route.ts` dentro de tu proyecto (App Router).
2. Define las variables en `.env.local`:

   ```
   CRM_API_KEY=un-secreto-largo-y-aleatorio
   DATABASE_URL=postgres://...   # el que ya uses
   ```

3. Ajusta el modelo Prisma (o tu capa de datos) para tener al menos:

   ```prisma
   model Cliente {
     id             Int       @id @default(autoincrement())
     crmClienteId   Int       @unique   // clave de sincronía con el CRM
     nombre         String
     razonSocial    String?
     email          String?
     telefono       String?
     direccion      String?
     ciudad         String?
     origen         String?
     sincronizadoEn DateTime?
     createdAt      DateTime  @default(now())
   }
   ```

4. Instala Zod si no lo tienes: `npm install zod`.

## Qué configurar en el CRM (`application.properties`)

```properties
app.tickets.enabled=true
app.tickets.url=https://tu-sistema-tickets.com/api/clientes
# En producción mejor por variable de entorno: APP_TICKETS_API_KEY
app.tickets.api-key=el-mismo-secreto-de-CRM_API_KEY
app.tickets.max-reintentos=5
```

Mientras `enabled=false`, el botón responde con el código
`TICKETS_DESHABILITADO` (HTTP 400) y nada sale del CRM.

## Contrato del endpoint

`POST /api/clientes` — header obligatorio `X-API-Key`.

Cuerpo que envía el CRM:

```json
{
  "crmClienteId": 3,
  "nombre": "Laboratorio Central UTM",
  "razonSocial": null,
  "rfc": null,
  "email": "lab@utmorelia.edu.mx",
  "telefono": "4433221100",
  "direccion": null,
  "ciudad": null,
  "estadoRegion": null,
  "sitioWeb": null,
  "ejecutivoEmail": "admin@reactivosdelvalle.com",
  "origen": "CRM_REACTIVOS_DEL_VALLE",
  "fechaEnvio": "2026-08-25T16:44:37.74"
}
```

Respuestas:

| Caso | HTTP | Cuerpo |
|---|---|---|
| Creado/actualizado | 201 | `{"ok": true, "ticketClienteId": 555}` |
| API key inválida | 401 | `{"ok": false, "error": "..."}` |
| Datos inválidos | 400 | `{"ok": false, "error": "...", "detalles": {...}}` |
| Error interno | 500 | `{"ok": false, "error": "..."}` |

El upsert usa `crmClienteId`, así que reenviar un cliente nunca lo duplica:
lo actualiza.

## Endpoints nuevos del CRM para tu frontend React

| Método y ruta | Qué hace |
|---|---|
| `POST /api/clientes/{id}/enviar-tickets` | Botón "Enviar cliente a tickets". Devuelve `{clienteId, estado, ticketClienteId, intentos, ultimoError, enviadoEn}` |
| `GET /api/clientes/{id}/tickets` | Estado de sincronización: `NO_ENVIADO`, `PENDIENTE`, `ENVIADO` o `ERROR` (para badge/botón deshabilitado) |

Sugerencia de UI: botón siempre visible; si el estado es `ENVIADO`
mostrar "Reenviar", si es `ERROR` mostrar el `ultimoError` como tooltip.
