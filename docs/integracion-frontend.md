# 🚀 Guía Definitiva de Integración Frontend → Backend CRM

> **Objetivo:** Cero errores al enviar JSON, autenticación perfecta y manejo correcto de todas las respuestas. Seguir esta guía al pie de la letra garantiza una integración sin problemas.

---

## 📋 TABLA DE CONTENIDO

1. [Convenciones Globales OBLIGATORIAS](#1-convenciones-globales-obligatorias)
2. [✅ Checklist Anti-Errores JSON (IMPORTANTE)](#2--checklist-anti-errores-json-importante)
3. [Autenticación JWT Paso a Paso](#3-autenticación-jwt-paso-a-paso)
4. [Formato de Errores Estándar y Cómo Manejarlos](#4-formato-de-errores-estándar-y-cómo-manejarlos)
5. [Formatos JSON Exactos por Endpoint](#5-formatos-json-exactos-por-endpoint)
6. [Ejemplos Funcionales (Fetch + Axios)](#6-ejemplos-funcionales-fetch--axios)
7. [Mapa Completo de Rutas](#7-mapa-completo-de-rutas)
8. [🔧 Troubleshooting de Errores Comunes](#8--troubleshooting-de-errores-comunes)
9. [Swagger UI (Documentación Viva)](#9-swagger-ui-documentación-viva)

---

## 1. Convenciones Globales OBLIGATORIAS

| Aspecto | Detalle EXACTO |
|---|---|
| **URL Base (local)** | `http://localhost:8080` |
| **URL Base (producción)** | Definir en variable de entorno `VITE_API_URL` o similar |
| **Formato de datos** | **SIEMPRE** JSON en request y response |
| **Header Content-Type** | `Content-Type: application/json` — **EN TODAS LAS PETICIONES** que envíen body |
| **Autenticación** | JWT Bearer: `Authorization: Bearer <accessToken>` en **TODAS** las rutas excepto `/api/auth/**` |
| **Encoding de caracteres** | `UTF-8` (incluido en Content-Type: `application/json;charset=UTF-8`) |
| **Métodos HTTP** | `GET` (leer) · `POST` (crear) · `PUT` (actualizar completo) · `PATCH` (actualizar parcial) · `DELETE` (eliminar) |
| **CORS** | ✅ Configurado en backend para aceptar cualquier origen. NO necesita proxy en desarrollo. |
| **Roles** | `ADMIN` · `GERENTE` · `EJECUTIVO`. Rutas `/api/admin/**` → solo `ADMIN` |
| **Trazabilidad** | Cada respuesta incluye header **`X-Request-Id`**. Guardarlo y reportarlo si algo falla. |
| **Fechas** | Formato **ISO 8601** sin tiempo: `YYYY-MM-DD` (ej: `"2026-11-15"`). NO enviar strings de fecha en otros formatos. |
| **Decimales/Moneda** | Enviar como **número** JSON, NO como string. Ej: `"valor": 150000.00` ✅ · `"valor": "150000"` ❌ |
| **IDs** | Enviar como **número entero** (Long). Ej: `"clienteId": 7` ✅ · `"clienteId": "7"` ❌ |
| **Enums** | Enviar como **string** con valor exacto del enum. Ej: `"estado": "GANADA"` |
| **Booleans** | Enviar como `true` / `false` JSON. NO `"true"` (string) ❌ |
| **Nulls** | Campos opcionales sin valor → enviar `null` o **NO incluir el campo**. NO enviar `""` (string vacío) en campos numéricos/date. |

---

## 2. ✅ Checklist Anti-Errores JSON (IMPORTANTE)

> **Antes de cada `fetch`/`axios.post`, verificar TODOS estos puntos.** El 95% de los errores `400 VALIDACION_FALLIDA` se evitan con este checklist.

### 🟢 ANTES de enviar el request:

- [ ] **Header `Content-Type: application/json`** está presente (solo en POST/PUT/PATCH con body)
- [ ] El objeto JavaScript fue **serializado** con `JSON.stringify(body)` — NO enviar objeto plano a `fetch`
- [ ] **Ningún campo `@NotBlank` es `""` (string vacío)** → debe ser `null` o un valor válido
- [ ] **Ningún campo `@NotNull` es `null` o `undefined`** → debe tener valor
- [ ] IDs (`clienteId`, `etapaId`, etc.) son **números** (`7`) no strings (`"7"`)
- [ ] Valores monetarios/decimales son **números** (`150000.00`) no strings (`"150000"`)
- [ ] Fechas son strings en formato **`YYYY-MM-DD`** (ej: `"2026-11-15"`) — NO objetos Date directamente
- [ ] Enums son **strings mayúsculas exactos**: `"GANADA"` · `"PENDIENTE"` · `"CLIENTE"` — revisar valores válidos más abajo
- [ ] Booleans son `true`/`false` (JSON) — NO `"true"` / `"false"` (strings)
- [ ] Strings no exceden el `@Size(max=X)` máximo → ver límites en cada DTO abajo
- [ ] Emails pasan regex básico: contienen `@` y un dominio
- [ ] Campos numéricos están en rango (`@Min` / `@Max`): probabilidad 0-100, mes 1-12, etc.
- [ ] El body NO contiene campos extra que no existen en el DTO (Spring ignora los extra, pero es buena práctica limpiarlos)
- [ ] En el body de `PATCH /reasignar`: parámetro va como **query param** (`?nuevoEjecutivoId=5`), NO en el body JSON

### 🔵 DESPUÉS de recibir la respuesta:

- [ ] Verificar `response.ok` (status 2xx) **antes** de hacer `.json()`
- [ ] Siempre hacer `.json()` incluso en error — el backend **siempre** devuelve JSON con detalle
- [ ] Capturar el `X-Request-Id` del header: `response.headers.get('X-Request-Id')` para debugging
- [ ] Si status es `401` → ejecutar flujo de refresh token **inmediatamente**
- [ ] Si status es `403` → mostrar "No tienes permiso" y **no** volver a intentar
- [ ] Si status es `400` y `error === "VALIDACION_FALLIDA"` → leer el objeto `campos` y mostrar cada mensaje debajo del input correspondiente

---

## 3. Autenticación JWT Paso a Paso

### Duración de tokens (definida en backend):
- **accessToken**: 1 hora (`3600000` ms) → enviar en `Authorization: Bearer <accessToken>`
- **refreshToken**: 7 días (`604800000` ms) → solo usar en `/api/auth/refresh`

### Flujo correcto de autenticación:

```
┌──────────────────────────────────────────────────────────┐
│  PASO 1: LOGIN                                           │
│  POST /api/auth/login → recibir {accessToken, refreshToken, usuario} │
│  → Guardar BOTH tokens en localStorage o secure cookie   │
│  → Guardar objeto usuario (contiene .rol para permisos)  │
└──────────────────────────┬───────────────────────────────┘
                           ▼
┌──────────────────────────────────────────────────────────┐
│  PASO 2: CUALQUIER PETICIÓN AUTENTICADA                  │
│  Header: Authorization: Bearer {accessToken}             │
│  → Si responde OK → normal                               │
│  → Si responde 401 NO_AUTENTICADO → ir PASO 3 (refresh)  │
└──────────────────────────┬───────────────────────────────┘
                           ▼
┌──────────────────────────────────────────────────────────┐
│  PASO 3: REFRESH TOKEN (AUTO)                            │
│  POST /api/auth/refresh  body: {refreshToken}            │
│  → Si responde 200: guardar NUEVOS tokens y reintentar   │
│    la petición original transparente para el usuario     │
│  → Si responde 400 TOKEN_INVALIDO/EXPIRADO:              │
│    → BORRAR tokens del storage                           │
│    → Redirigir a /login                                  │
└──────────────────────────────────────────────────────────┘
```

### Credenciales por defecto (desarrollo):
```
Email:    admin@reactivosdelvalle.com
Password: Admin123!
```

---

## 4. Formato de Errores Estándar y Cómo Manejarlos

> **TODOS** los errores devuelven la misma estructura JSON. No hay excepciones. Manejar este formato en un interceptor global.

### Estructura universal de error:
```json
{
  "status": 400,
  "error": "CODIGO_ERROR",
  "mensaje": "Texto listo para mostrar al usuario final",
  "timestamp": "2026-08-21T19:22:01.552421700Z",
  "campos": {
    "nombre": "El nombre es obligatorio (Regla de Oro)",
    "email": "El formato del correo electrónico no es válido"
  }
}
```

> ⚠️ `campos` SÓLO existe cuando `error === "VALIDACION_FALLIDA"`. En otros errores, ese campo NO viene.

### Tabla completa de códigos de error:

| HTTP | error | Causa | Acción en frontend |
|---|---|---|---|
| `400` | `VALIDACION_FALLIDA` | JSON con campos inválidos | Mostrar `campos.{nombreCampo}` debajo de cada input |
| `400` | `TOKEN_INVALIDO` | Refresh token inválido/usado | Borrar storage + redirigir /login |
| `400` | `TOKEN_EXPIRADO` | Refresh token venció (7 días) | Borrar storage + redirigir /login |
| `400` | `PASSWORD_ACTUAL_INCORRECTA` | En cambio de password | Mostrar mensaje en campo "passwordActual" |
| `400` | `PASSWORD_IGUAL` | Nueva = actual | Mostrar mensaje |
| `400` | `PROSPECTO_CONVERTIDO` | Prospecto ya fue convertido a cliente | Deshabilitar botón |
| `400` | `REGLA_5_INCUMPLIDA` | Etapa inválida en pipeline | Validar etapa antes de enviar |
| `400` | `TICKETS_DESHABILITADO` | Integración tickets apagada | Ocultar botón de envío |
| `401` | `NO_AUTENTICADO` | Sin token / token vencido / credenciales malas | Intentar refresh token; si falla → /login |
| `403` | `ACCESO_DENEGADO` | Rol insuficiente (ej: EJECUTIVO intentando /api/admin) | Mostrar pantalla "Sin permiso" |
| `404` | `RECURSO_NO_ENCONTRADO` | Ruta o ID no existe | Mostrar 404 o notificación |
| `409` | `EMAIL_DUPLICADO` | Email ya existe en creación de usuario/cliente | Resaltar campo email |
| `409` | `PROSPECTO_CONVERTIDO` | Ya convertido | Ídem arriba |
| `500` | `ERROR_INTERNO` | Error inesperado en servidor | Mostrar mensaje genérico; **reportar con `X-Request-Id`** |

---

## 5. Formatos JSON Exactos por Endpoint

> Aquí están LOS FORMATOS EXACTOS que espera el backend, con todas las validaciones. Copiar y pegar estos objetos en el frontend.

---

### 🔐 AUTENTICACIÓN

#### POST `/api/auth/login`
```json
{
  "email": "admin@reactivosdelvalle.com",
  "password": "Admin123!"
}
```
✅ Validaciones: `email` (@NotBlank + @Email), `password` (@NotBlank)

#### POST `/api/auth/refresh`
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

#### POST `/api/auth/olvide-password`
```json
{
  "email": "usuario@empresa.com"
}
```
> 💡 Responde **siempre 200** con el mismo mensaje, exista o no el correo (seguridad: no revelar emails registrados).

#### POST `/api/auth/restablecer-password`
```json
{
  "token": "<TOKEN_DE_LA_URL>",
  "nuevaPassword": "NuevaClave999"
}
```
✅ Validación password: mínimo 8 caracteres (backend valida).

---

### 👤 PERFIL

#### PUT `/api/perfil`
```json
{
  "nombre": "Administrador",
  "apellido": "Sistema",
  "telefono": "3001234567",
  "fotoUrl": null
}
```
> ⚠️ NO enviar `rol`, `email` ni `activo` aquí (se ignoran; se cambian desde admin).

#### PUT `/api/perfil/password`
```json
{
  "passwordActual": "Admin123!",
  "nuevaPassword": "NuevaClave999"
}
```

---

### 🧑 CLIENTES (`/api/clientes`)

#### POST/PUT `/api/clientes` [ClienteRequest]
```json
{
  "nombre": "Químicos XYZ SA de CV",
  "razonSocial": "Químicos XYZ SA de CV",
  "rfc": "QXY123456ABC",
  "tipoId": 1,
  "industriaId": 2,
  "zonaId": 3,
  "ejecutivoId": 5,
  "telefonoPrincipal": "5551234",
  "emailPrincipal": "ventas@quimicosxyz.com",
  "sitioWeb": "https://quimicosxyz.com",
  "direccion": "Av. Industrial 123, Col. Centro",
  "ciudad": "Monterrey",
  "estadoRegion": "Nuevo León",
  "notas": "Cliente prioritario - contacto frecuente",
  "fechaPrimeraCompra": "2025-03-15"
}
```
✅ Validaciones:
- `nombre`: **@NotBlank**, máx 200
- `razonSocial`: máx 200
- `rfc`: máx 20
- `telefonoPrincipal`: máx 20
- `emailPrincipal`: @Email, máx 150
- `sitioWeb` / `direccion`: máx 300
- `ciudad` / `estadoRegion`: máx 100
- Todos los `*Id`: Long (número entero), `null` permitido
- `fechaPrimeraCompra`: `YYYY-MM-DD` o `null`

#### POST/PUT `/api/clientes/{id}/contactos` [ContactoRequest]
```json
{
  "nombre": "María González",
  "cargo": "Gerente de Compras",
  "telefono": "8112345678",
  "email": "maria.gonzalez@quimicosxyz.com",
  "esPrincipal": true
}
```
✅ Validaciones:
- `nombre`: **@NotBlank**, máx 200
- `cargo`: máx 100
- `telefono`: máx 20
- `email`: @Email, máx 150

#### PATCH `/api/clientes/{id}/reasignar?nuevoEjecutivoId=5`
> ⚠️ **SIN BODY JSON** — el parámetro `nuevoEjecutivoId` va COMO QUERY PARAM en la URL. Error común: enviarlo en JSON.

---

### 📋 PROSPECTOS (`/api/prospectos`)

#### POST/PUT `/api/prospectos` [ProspectoRequest]
```json
{
  "nombre": "Juan Pérez",
  "empresa": "Nueva Empresa SAS",
  "tipoId": 1,
  "industriaId": 4,
  "zonaId": 2,
  "responsableId": 3,
  "etapaId": 1,
  "telefono": "3009876543",
  "email": "juan@nuevaempresa.com",
  "sitioWeb": "https://nuevaempresa.com",
  "notas": "Interesado en línea de reactivos para laboratorio",
  "proximaAccion": "Llamada de seguimiento",
  "fechaProximaAccion": "2026-08-30"
}
```
✅ Validaciones OBLIGATORIAS (Regla 5):
- `nombre`: **@NotBlank**
- `etapaId`: **@NotNull** (catálogo ETAPA_PIPELINE)
- `proximaAccion`: **@NotBlank**
- `fechaProximaAccion`: **@NotNull** (`YYYY-MM-DD`)

#### POST `/api/prospectos/{id}/convertir` [ConvertirProspectoRequest]
> El bloque `oportunidad` es **OPCIONAL**. Si se envía, sus campos internos pasan a ser obligatorios.

```json
{
  "razonSocial": "Químicos XYZ SA de CV",
  "rfc": "QXY123456ABC",
  "tipoId": 1,
  "industriaId": 2,
  "zonaId": 3,
  "telefonoPrincipal": "5551234",
  "emailPrincipal": "ventas@quimicosxyz.com",
  "sitioWeb": "https://quimicosxyz.com",
  "direccion": "Av. Industrial 123",
  "ciudad": "Monterrey",
  "estadoRegion": "Nuevo León",
  "notas": "Convertido desde prospecto #4",
  "fechaPrimeraCompra": "2026-09-01",
  "oportunidad": {
    "nombre": "Suministro anual de reactivos",
    "etapaId": 2,
    "valor": 150000.00,
    "probabilidad": 30,
    "fechaEstimadaCierre": "2026-11-15",
    "proximaAccion": "Enviar cotización formal",
    "fechaProximaAccion": "2026-08-27",
    "descripcion": "Contrato anual de suministro",
    "competencia": "Empresa Competidora XYZ"
  }
}
```

---

### 💼 OPORTUNIDADES (`/api/oportunidades`)

#### POST/PUT `/api/oportunidades` [OportunidadRequest]
```json
{
  "nombre": "Suministro reactivos anuales Q1-Q4",
  "clienteId": 7,
  "prospectoId": null,
  "ejecutivoId": 5,
  "etapaId": 3,
  "valor": 250000.00,
  "probabilidad": 60,
  "fechaEstimadaCierre": "2026-12-15",
  "proximaAccion": "Reunión con director de operaciones",
  "fechaProximaAccion": "2026-09-05",
  "descripcion": "Contrato marco para todo el año siguiente",
  "competencia": "Proveedor Anterior SA"
}
```
✅ Validaciones OBLIGATORIAS (Regla de Oro) — estos 6 campos NUNCA deben faltar:
- `nombre`: **@NotBlank**, máx 200
- `clienteId`: **@NotNull** (Long)
- `etapaId`: **@NotNull** (Long)
- `valor`: **@NotNull**, `@DecimalMin("0.01")` (número > 0)
- `probabilidad`: **@NotNull**, `@Min(0)` `@Max(100)` (entero 0-100)
- `fechaEstimadaCierre`: **@NotNull** (`YYYY-MM-DD`)
- `proximaAccion`: **@NotBlank**
- `fechaProximaAccion`: **@NotNull** (`YYYY-MM-DD`)

#### PATCH `/api/oportunidades/{id}/etapa` [CambioEtapaRequest]
```json
{
  "etapaNuevaId": 4,
  "notas": "Cliente aprobó cotización - pasamos a negociación"
}
```
✅ Validación: `etapaNuevaId`: **@NotNull**

#### PATCH `/api/oportunidades/{id}/cerrar` [CerrarOportunidadRequest]
```json
{
  "estado": "GANADA",
  "motivoPerdida": null,
  "fechaCierreReal": "2026-09-10"
}
```
✅ `estado` es un enum: **`EstadoOportunidad`** — valores válidos:
- `"ABIERTA"` · `"GANADA"` · `"PERDIDA"` · `"CONGELADA"`
> 💡 `motivoPerdida` solo es relevante si `estado === "PERDIDA"`

---

### 📞 SEGUIMIENTOS (`/api/seguimientos`)

#### POST/PUT [SeguimientoRequest]
```json
{
  "oportunidadId": 3,
  "tipo": "Llamada telefónica",
  "fechaProgramada": "2026-08-28",
  "fechaRealizada": "2026-08-28",
  "estado": "REALIZADO",
  "notas": "Cliente confirmó recepción de muestra",
  "proximaAccion": "Esperar resultados de prueba"
}
```
✅ Validaciones:
- `oportunidadId`: **@NotNull**
- `tipo`: **@NotBlank**
- `fechaProgramada`: **@NotNull**
- `estado` enum **EstadoSeguimiento**: `"PENDIENTE"` · `"REALIZADO"` · `"VENCIDO"` · `"CANCELADO"` (si no se envía, default `PENDIENTE`)

---

### 🏢 VISITAS (`/api/visitas`)

#### POST/PUT [VisitaRequest]
```json
{
  "tipoEntidad": "CLIENTE",
  "clienteId": 7,
  "prospectoId": null,
  "oportunidadId": 3,
  "fecha": "2026-09-02",
  "objetivo": "Presentar nueva línea de reactivos de alta pureza",
  "necesidadDetectada": "Necesitan reemplazar proveedor actual por demoras",
  "competenciaMencionada": "Marca Competidora ABC",
  "resultadoId": 2,
  "oportunidadGenerada": true,
  "compromiso": "Enviar cotización semanal próxima",
  "notasAdicionales": "Mostraron mucho interés en nuestro catálogo de solución estándar"
}
```
✅ Validaciones:
- `tipoEntidad`: **@NotNull** enum **TipoEntidadVisita**: `"CLIENTE"` · `"PROSPECTO"`
- `fecha`: **@NotNull**
- `objetivo`: **@NotBlank**
- `necesidadDetectada`: **@NotBlank**
- `competenciaMencionada`: **@NotBlank**
- `resultadoId`: **@NotNull**
- `oportunidadGenerada`: **@NotNull** (boolean: `true` / `false`)
- `compromiso`: **@NotBlank**
> 💡 Regla: si `tipoEntidad = "CLIENTE"` → debe venir `clienteId`. Si `"PROSPECTO"` → `prospectoId`.

---

### 💰 VENTAS (`/api/ventas`)

#### POST/PUT [VentaRequest]
```json
{
  "ejecutivoId": 5,
  "anio": 2026,
  "mes": 9,
  "meta": 500000.00,
  "ventaReal": 380000.00,
  "forecast": 450000.00,
  "notas": "Cierre de 2 contratos importantes esperados"
}
```
✅ Validaciones:
- `anio`: **@NotNull**, `@Min(2020)`, `@Max(2100)`
- `mes`: **@NotNull**, `@Min(1)`, `@Max(12)` (mes 1 = Enero, 12 = Diciembre)
- `meta`: **@NotNull** (número)

---

### 👥 USUARIOS (ADMIN) `/api/admin/usuarios`

#### POST/PUT [UsuarioRequest]
```json
{
  "nombre": "Carlos",
  "apellido": "Ramírez",
  "email": "carlos.ramirez@reactivosdelvalle.com",
  "password": "ClaveSegura123",
  "rol": "EJECUTIVO",
  "telefono": "3005551234",
  "fotoUrl": null,
  "activo": true
}
```
✅ Validaciones:
- `nombre`: **@NotBlank**
- `apellido`: **@NotBlank**
- `email`: **@NotBlank** + @Email
- `password`: Obligatorio en **POST** (crear). En **PUT** (editar), `null` = no cambiar password.
- `rol`: **@NotNull** enum **RolUsuario**: `"ADMIN"` · `"GERENTE"` · `"EJECUTIVO"`

---

### 📚 CATÁLOGOS (ADMIN) `/api/admin/catalogos`

#### POST/PUT [CatalogoRequest]
```json
{
  "tipo": "ETAPA_PIPELINE",
  "clave": "NEGOCIACION",
  "valor": "Negociación",
  "orden": 4,
  "activo": true
}
```
> Catálogos existentes (campo `tipo`): `ETAPA_PIPELINE`, `TIPO_CLIENTE`, `INDUSTRIA`, `ZONA`, `RESULTADO_VISITA`, `ETAPA_PROSPECTO`, etc.

---

## 6. Ejemplos Funcionales (Fetch + Axios)

### 📦 Opción 1: Cliente HTTP base con Fetch (Nativo - sin dependencias)

Crear archivo `src/lib/api-client.js`:

```javascript
// ============================================================
// Cliente HTTP CRM API - USAR ESTE PARA TODAS LAS PETICIONES
// Maneja: JSON, headers, auth, auto-refresh, errores estándar
// ============================================================

const BASE_URL = import.meta.env.VITE_API_URL || "http://localhost:8080";
const STORAGE_KEYS = { access: "crm_access", refresh: "crm_refresh", user: "crm_user" };

// --- Utilidades internas ---
const getTokens = () => ({
  access: localStorage.getItem(STORAGE_KEYS.access),
  refresh: localStorage.getItem(STORAGE_KEYS.refresh),
});

const saveTokens = (access, refresh, usuario) => {
  localStorage.setItem(STORAGE_KEYS.access, access);
  localStorage.setItem(STORAGE_KEYS.refresh, refresh);
  if (usuario) localStorage.setItem(STORAGE_KEYS.user, JSON.stringify(usuario));
};

const clearTokens = () => {
  localStorage.removeItem(STORAGE_KEYS.access);
  localStorage.removeItem(STORAGE_KEYS.refresh);
  localStorage.removeItem(STORAGE_KEYS.user);
};

let refreshPromise = null; // Evita llamadas refresh simultáneas

const tryRefresh = async () => {
  if (refreshPromise) return refreshPromise;
  refreshPromise = (async () => {
    const { refresh } = getTokens();
    if (!refresh) throw new Error("NO_REFRESH");
    try {
      const res = await fetch(`${BASE_URL}/api/auth/refresh`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ refreshToken: refresh }),
      });
      if (!res.ok) throw new Error("REFRESH_FAILED");
      const data = await res.json();
      saveTokens(data.accessToken, data.refreshToken);
      return data.accessToken;
    } catch {
      clearTokens();
      window.location.href = "/login";
      throw new Error("REFRESH_FAILED");
    } finally {
      refreshPromise = null;
    }
  })();
  return refreshPromise;
};

// --- Cliente principal ---
export async function api(path, { method = "GET", body, params, auth = true } = {}) {
  // 1. Construir URL con query params si existen
  let url = BASE_URL + path;
  if (params) {
    const search = new URLSearchParams(params).toString();
    if (search) url += `?${search}`;
  }

  // 2. Construir headers
  const headers = {
    "Content-Type": "application/json",
    "Accept": "application/json",
  };

  // 3. Obtener token si requiere auth
  let token = auth ? getTokens().access : null;

  // Función interna para ejecutar la petición
  const doRequest = async (accessToken) => {
    if (accessToken) headers["Authorization"] = `Bearer ${accessToken}`;
    const res = await fetch(url, {
      method,
      headers,
      body: body ? JSON.stringify(body) : undefined,
    });
    const requestId = res.headers.get("X-Request-Id");
    const data = await res.json().catch(() => ({}));
    return { res, data, requestId };
  };

  // 4. Primer intento
  let { res, data, requestId } = await doRequest(token);

  // 5. Si 401 y tenemos refresh → intentar renovar UNA vez y reintentar
  if (res.status === 401 && auth) {
    try {
      token = await tryRefresh();
      ({ res, data, requestId } = await doRequest(token));
    } catch {
      // Refresh falló → ya redirigió a /login
      throw { status: 401, error: "NO_AUTENTICADO", mensaje: "Sesión expirada" };
    }
  }

  // 6. Si no es 2xx → lanzar error con formato estándar
  if (!res.ok) {
    const err = { ...data, requestId, status: res.status };
    console.warn(`[API ERROR ${res.status}] ${path}`, err);
    throw err;
  }

  return data;
}

// --- Helpers específicos (azúcar sintáctico) ---
export const apiGet = (p, o) => api(p, { ...o, method: "GET" });
export const apiPost = (p, body, o) => api(p, { ...o, method: "POST", body });
export const apiPut = (p, body, o) => api(p, { ...o, method: "PUT", body });
export const apiPatch = (p, body, o) => api(p, { ...o, method: "PATCH", body });
export const apiDelete = (p, o) => api(p, { ...o, method: "DELETE" });

// --- Uso fácil del storage de sesión ---
export const session = {
  save: (loginRes) => saveTokens(loginRes.accessToken, loginRes.refreshToken, loginRes.usuario),
  clear: clearTokens,
  getUser: () => {
    try { return JSON.parse(localStorage.getItem(STORAGE_KEYS.user) || "null"); }
    catch { return null; }
  },
  isAuthenticated: () => !!getTokens().access,
};
```

### Cómo usar el cliente (ejemplos reales):
```javascript
import { api, apiGet, apiPost, session } from "@/lib/api-client";

// --- Login ---
const loginData = await apiPost("/api/auth/login",
  { email: "admin@reactivosdelvalle.com", password: "Admin123!" },
  { auth: false }
);
session.save(loginData); // guarda tokens + usuario

// --- Listar clientes ---
const clientes = await apiGet("/api/clientes");

// --- Crear cliente (validación en vivo) ---
try {
  const nuevo = await apiPost("/api/clientes", {
    nombre: "Nuevo Cliente SA",
    emailPrincipal: "contacto@nuevocliente.com",
    ejecutivoId: 5,
  });
  console.log("Cliente creado:", nuevo);
} catch (err) {
  if (err.error === "VALIDACION_FALLIDA") {
    // err.campos = { nombre?: "...", emailPrincipal?: "..." }
    Object.entries(err.campos).forEach(([campo, mensaje]) => {
      document.getElementById(`err-${campo}`).textContent = mensaje;
    });
  } else {
    alert(err.mensaje); // mostrar al usuario
    console.error("X-Request-Id:", err.requestId);
  }
}

// --- Reasignar cliente (QUERY PARAM, no body) ---
await apiPatch(`/api/clientes/${id}/reasignar`, undefined, { params: { nuevoEjecutivoId: 5 } });

// --- Envío correcto de decimales (NÚMERO no string) ---
await apiPost("/api/oportunidades", {
  nombre: "Cotización X",
  clienteId: 7,
  etapaId: 1,
  valor: 125000.50,   // ✅ número. NO "125000.50" ❌
  probabilidad: 50,    // ✅ entero. NO "50" ❌
  fechaEstimadaCierre: "2026-12-31", // ✅ string YYYY-MM-DD
  proximaAccion: "Enviar cotización",
  fechaProximaAccion: "2026-09-01",
});
```

---

### 📦 Opción 2: Cliente con Axios (con interceptor)

Si el proyecto usa Axios, crear `src/lib/axios-client.js`:

```javascript
import axios from "axios";

const BASE_URL = import.meta.env.VITE_API_URL || "http://localhost:8080";
const STORAGE_KEYS = { access: "crm_access", refresh: "crm_refresh", user: "crm_user" };

const http = axios.create({
  baseURL: BASE_URL,
  headers: {
    "Content-Type": "application/json",
    "Accept": "application/json",
  },
  // No transformar request: queremos control total del JSON
});

// ============== INTERCEPTOR REQUEST: Inyectar token ==============
http.interceptors.request.use((config) => {
  const token = localStorage.getItem(STORAGE_KEYS.access);
  if (token && !config.url.includes("/api/auth/")) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// ============== INTERCEPTOR RESPONSE: Auto-refresh 401 ==============
let refreshPromise = null;

http.interceptors.response.use(
  (res) => res, // 2xx → OK
  async (error) => {
    const originalReq = error.config;
    const status = error.response?.status;
    const data = error.response?.data || {};
    const requestId = error.response?.headers?.["x-request-id"];

    // Adjuntar requestId al error para debug
    error.requestId = requestId;

    // Si 401 y no es reintento → refresh token y reintentar
    if (status === 401 && !originalReq._retry) {
      originalReq._retry = true;
      if (!refreshPromise) {
        refreshPromise = (async () => {
          const refreshToken = localStorage.getItem(STORAGE_KEYS.refresh);
          if (!refreshToken) throw new Error("NO_REFRESH");
          try {
            const { data } = await axios.post(`${BASE_URL}/api/auth/refresh`,
              { refreshToken },
              { headers: { "Content-Type": "application/json" } }
            );
            localStorage.setItem(STORAGE_KEYS.access, data.accessToken);
            localStorage.setItem(STORAGE_KEYS.refresh, data.refreshToken);
            if (data.usuario) {
              localStorage.setItem(STORAGE_KEYS.user, JSON.stringify(data.usuario));
            }
            return data.accessToken;
          } catch {
            localStorage.clear();
            window.location.href = "/login";
            throw error;
          } finally {
            refreshPromise = null;
          }
        })();
      }
      try {
        const newToken = await refreshPromise;
        originalReq.headers.Authorization = `Bearer ${newToken}`;
        return http(originalReq); // Reintentar petición original
      } catch { return Promise.reject(error); }
    }

    // Loggear en consola para debugging
    console.warn(`[AXIOS ${status}] ${originalReq.url}`, { data, requestId });

    return Promise.reject({ ...data, status, requestId });
  }
);

export default http;
```

### Con Axios + React Hook Form (patrón recomendado):
```tsx
// Ejemplo formulario de crear cliente con React Hook Form
import { useForm } from "react-hook-form";
import http from "@/lib/axios-client";

type ClienteForm = {
  nombre: string; emailPrincipal?: string; ejecutivoId?: number; // ...
};

export const CrearClienteForm = () => {
  const { register, handleSubmit, setError, formState: { errors } } = useForm<ClienteForm>();

  const onSubmit = async (body: ClienteForm) => {
    try {
      const { data } = await http.post("/api/clientes", body);
      alert("Cliente creado: ID " + data.id);
    } catch (err: any) {
      // Mapear errores de validación a campos de React Hook Form
      if (err.error === "VALIDACION_FALLIDA" && err.campos) {
        Object.entries(err.campos).forEach(([field, msg]) => {
          setError(field as any, { type: "server", message: msg as string });
        });
      } else {
        alert(err.mensaje || "Error desconocido. ID: " + err.requestId);
      }
    }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <input {...register("nombre", { required: true })} placeholder="Nombre" />
      {errors.nombre && <span className="text-red-500">{errors.nombre.message}</span>}

      <input {...register("emailPrincipal")} placeholder="Email" />
      {errors.emailPrincipal && <span className="text-red-500">{errors.emailPrincipal.message}</span>}

      <button type="submit">Crear</button>
    </form>
  );
};
```

---

## 7. Mapa Completo de Rutas

### Públicas (sin token)
| Método | Ruta | Body | Descripción |
|---|---|---|---|
| POST | `/api/auth/login` | [LoginRequest](#post-apiauthlogin) | Iniciar sesión |
| POST | `/api/auth/refresh` | `{refreshToken}` | Renovar tokens |
| POST | `/api/auth/logout` | `{}` | Cerrar sesión (front debe borrar tokens) |
| POST | `/api/auth/olvide-password` | `{email}` | Paso 1 recuperación |
| POST | `/api/auth/restablecer-password` | `{token, nuevaPassword}` | Paso 2 recuperación |
| GET | `/swagger-ui/index.html` | - | Documentación Swagger UI |
| GET | `/v3/api-docs` | - | Especificación OpenAPI JSON |

### Autenticado (cualquier rol)
| Método | Ruta | Body / Params |
|---|---|---|
| GET/PUT | `/api/perfil` | [Perfil](#put-aperfil) |
| PUT | `/api/perfil/password` | `{passwordActual, nuevaPassword}` |
| GET | `/api/dashboard/resumen` | - |
| GET | `/api/pipeline` | - |
| GET/POST | `/api/clientes` | [ClienteRequest](#postput-apiclientes-clienterequest) |
| GET/PUT/DELETE | `/api/clientes/{id}` | ID en URL |
| PATCH | `/api/clientes/{id}/reasignar` | **Query param** `?nuevoEjecutivoId=N` |
| POST/GET | `/api/clientes/{id}/enviar-tickets` | - |
| GET | `/api/clientes/{id}/tickets` | - |
| GET/POST | `/api/clientes/{id}/contactos` | [ContactoRequest](#contactorequest) |
| PUT/DELETE | `/api/clientes/{id}/contactos/{cId}` | [ContactoRequest](#contactorequest) |
| GET/POST | `/api/prospectos` | [ProspectoRequest](#prospectorequest) |
| GET/PUT/DELETE | `/api/prospectos/{id}` | - |
| POST | `/api/prospectos/{id}/convertir` | [ConvertirProspectoRequest](#convertirprospectorequest) |
| GET/POST | `/api/oportunidades` | [OportunidadRequest](#oportunidadrequest) |
| GET/PUT/DELETE | `/api/oportunidades/{id}` | - |
| GET | `/api/oportunidades/{id}/historial` | - |
| PATCH | `/api/oportunidades/{id}/etapa` | [CambioEtapaRequest](#cambioetaparequest) |
| PATCH | `/api/oportunidades/{id}/cerrar` | [CerrarOportunidadRequest](#cerraroportunidadrequest) |
| GET/POST | `/api/visitas` | [VisitaRequest](#visitarequest) |
| GET/PUT/DELETE | `/api/visitas/{id}` | - |
| GET/POST | `/api/seguimientos` | [SeguimientoRequest](#seguimientorequest) |
| GET/PUT/DELETE | `/api/seguimientos/{id}` | - |
| GET | `/api/seguimientos/vencidos` | - |
| GET/POST | `/api/ventas` | [VentaRequest](#ventarequest) |
| GET/PUT/DELETE | `/api/ventas/{id}` | - |

### Solo ADMIN (rol = ADMIN)
| Método | Ruta | Body |
|---|---|---|
| GET/POST | `/api/admin/usuarios` | [UsuarioRequest](#usuariorequest) |
| GET/PUT/DELETE | `/api/admin/usuarios/{id}` | - |
| GET/POST | `/api/admin/catalogos` | [CatalogoRequest](#catalogorequest) |
| GET/PUT/DELETE | `/api/admin/catalogos/{id}` | - |
| GET | `/api/admin/auditoria?limite=100` | - |
| GET | `/api/admin/auditoria/tabla/{tabla}/registro/{id}` | - |
| GET | `/api/admin/auditoria/usuario/{usuarioId}` | - |

---

## 8. 🔧 Troubleshooting de Errores Comunes

### ❌ Error: `400 VALIDACION_FALLIDA` pero no veo por qué
**Diagnóstico:** El JSON no cumple las validaciones.
- **Acción:** Abrir DevTools → Network → pestaña fallida → Response. Mirar el objeto `campos`:
  ```json
  {"campos": {"valor": "El valor debe ser mayor a cero"}}
  ```
  El campo problemático está ahí escrito. NO adivines — usa ese objeto.

### ❌ Error: `401 NO_AUTENTICADO` en petición autenticada
- **Causa 1 (más común):** Olvidaste enviar `Authorization: Bearer <token>` → revisar headers en DevTools → Network.
- **Causa 2:** AccessToken venció (1 hora) → el interceptor DEBE refrescar automáticamente. Si no lo hace, revisar el interceptor.
- **Causa 3:** RefreshToken también venció (7 días sin usar la app) → redirigir a /login.

### ❌ Error: `403 ACCESO_DENEGADO`
- El usuario autenticado tiene un rol que no puede acceder a esa ruta.
- Rutas `/api/admin/**` requieren `rol = "ADMIN"`. Verificar `usuario.rol` del login.
- Borrado de clientes (`DELETE /api/clientes/{id}`): solo `ADMIN` y `GERENTE`.

### ❌ Error: JSON parse error / `Unexpected token <`
- **Causa 99%:** El servidor respondió HTML (ej: error 404 de Vite proxy, o respuesta genérica de error de Spring sin JSON).
- **Diagnóstico:** Revisar Network → Response. Si sale HTML, la URL está mal o el backend está caído.
- **Check:** `BASE_URL` termina sin `/` → `http://localhost:8080` ✅ · `http://localhost:8080/` ❌ (duplica slash).

### ❌ `JSON.stringify` no está serializando bien un Date
- **Problema:** `JSON.stringify(new Date())` → `"2026-08-25T19:22:01.552Z"` (con hora y Z). El backend espera **solo fecha**: `YYYY-MM-DD`.
- **Solución:** Formatear a string ANTES de stringify:
  ```javascript
  const fecha = new Date();
  const fechaStr = fecha.toISOString().split("T")[0]; // "2026-08-25" ✅
  ```

### ❌ Campos numéricos se envían como string (ej: `"probabilidad": "50"`)
- **Problema:** `<input type="number">` devuelve string en React/HTML.
- **Solución 1:** Convertir antes de enviar:
  ```javascript
  const body = {
    probabilidad: Number(form.probabilidad), // ✅ 50
    valor: parseFloat(form.valor),            // ✅ 1500.50
    clienteId: Number(form.clienteId),        // ✅ 7
  };
  ```
- **Solución 2:** En React Hook Form usar `valueAsNumber`:
  ```tsx
  <input type="number" {...register("probabilidad", { valueAsNumber: true })} />
  ```

### ❌ CORS error en consola
- **Causa 1:** Backend no está arrancado (no responde en absoluto).
- **Causa 2:** `BASE_URL` apunta a dominio equivocado.
- **Causa 3:** Front usa `https://` y backend `http://` (mixed content).
- ✅ El backend tiene CORS completamente abierto para desarrollo — si falla es conectividad, no configuración.

### ❌ Campo opcional se envía como `""` y falla
- **Problema:** Email vacío = `""` — el validador `@Email` rechaza strings vacíos.
- **Solución:** Clean function antes de enviar — convertir `""` → `null` o quitar campo:
  ```javascript
  const clean = (obj) => Object.fromEntries(
    Object.entries(obj).filter(([_, v]) => v !== "" && v !== undefined)
  );
  const body = clean({ emailPrincipal: form.email, /* ... */ });
  ```

---

## 9. Swagger UI (Documentación Viva)

> **Si tienes dudas sobre cualquier endpoint en tiempo real, usa Swagger — siempre está actualizado con el código.**

- **URL (local):** [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **OpenAPI JSON (para generar SDK):** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

### Cómo usar Swagger para probar endpoints:
1. Abrir Swagger UI
2. Ejecutar **POST /api/auth/login** con credenciales → copiar `accessToken`
3. Click botón **"Authorize"** 🔓 (arriba a la derecha) → pegar: `Bearer <accessToken>`
4. Ahora todos los endpoints pueden probarse directamente desde el navegador.

### Generar tipos TypeScript automáticamente (opcional pero potente):
Con el JSON de `/v3/api-docs`, herramientas como **OpenAPI Generator** o **Orval** crean interfaces TS + cliente HTTP listos para usar, **garantizando que el JSON enviado coincida 100%** con lo que espera Spring Boot.

---

## 🎯 RESUMEN FINAL (Las 5 Reglas de Oro)

1. **SIEMPRE** usar `Content-Type: application/json` y `JSON.stringify()`
2. **NUNCA** enviar strings vacíos `""` en campos obligatorios ni números como string
3. **VALIDAR** antes de enviar: campos `@NotNull` / `@NotBlank` presentes y en formato correcto
4. **MANEJAR** siempre la respuesta 401 con refresh token automático
5. **NO ADIVINAR ERRORES** — usar el objeto `campos` de `VALIDACION_FALLIDA` y el `X-Request-Id`

> 📩 **Si después de seguir esta guía hay un error inesperado:** Capturar `X-Request-Id`, la URL, el body enviado, y la respuesta JSON. Con esos 4 elementos, el equipo backend lo soluciona en minutos.

---

*Última actualización: 2026-08-25 · Aplicable a versión actual del CRM API*

---

## 8. Reportes y Exportación (GET /api/reportes/export)

> **⚠️ IMPORTANTE**: Este endpoint es 100% por **query params** (URL). NO envía JSON en el body. Método `GET`. Todos los parámetros deben estar **URL-encoded**. Requiere autenticación (Header `Authorization: Bearer <accessToken>`).

---

### 8.1 Qué debe enviar exactamente el frontend

#### URL Final
```
GET https://<TU_BACKEND>/api/reportes/export?PARAMS
```

#### Headers obligatorios
| Header | Valor |
|---|---|
| `Authorization` | `Bearer <accessToken>` |
| `Accept` | `*/*` (o `application/pdf`, `text/csv`, `application/json`, `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` según corresponda) |

#### Parámetros (query string)
| Parámetro | ¿Obligatorio? | Tipo | Valores aceptados | Ejemplo | Qué filtra |
|---|---|---|---|---|---|
| `formato` | ✅ SÍ | string | `csv`, `json`, `xlsx`, `pdf` | `formato=csv` | Formato del archivo descargado |
| `tipo` | ✅ SÍ | string | `ventas`, `clientes`, `oportunidades`, `pipeline`, `prospectos` | `tipo=ventas` | Qué dataset se exporta |
| `desde` | ⚙️ OPCIONAL | fecha (ISO: `YYYY-MM-DD`) | `2026-01-01` | `desde=2026-01-01` | Fecha inicial creación del registro |
| `hasta` | ⚙️ OPCIONAL | fecha (ISO: `YYYY-MM-DD`) | `2026-08-26` | `hasta=2026-08-26` | Fecha final creación del registro |
| `ejecutivoId` | ⚙️ OPCIONAL | number (Long) | entero positivo | `ejecutivoId=5` | Filtra por responsable (usuario ejecutivo). **Nota**: Si el usuario logueado tiene rol `EJECUTIVO`, este parámetro es IGNORADO y siempre se usa su propio ID (seguridad aislamiento de datos). |
| `estado` | ⚙️ OPCIONAL | string | Para oportunidades: `ACTIVA`, `GANADA`, `PERDIDA`, `CONGELADA` | `estado=GANADA` | Filtra por estado. Aplica solo para `tipo=oportunidades`. Ignorado en otros tipos. |

---

### 8.2 Columnas que exporta cada tipo

| `tipo=` | Columnas del reporte |
|---|---|
| `ventas` | **Periodo, Ejecutivo, Meta, Venta Real, Forecast, Cumplimiento** |
| `clientes` | **Nombre, Empresa, Email, Teléfono, Ejecutivo, Industria, Zona** |
| `oportunidades` | **Nombre, Cliente, Etapa, Valor, Probabilidad, Estado, Fecha Cierre** |
| `pipeline` | **Etapa, Cantidad Oportunidades, Valor Total, Valor Ponderado** |
| `prospectos` | **Nombre, Empresa, Email, Etapa, Origen, Fecha Creación** |

> 💡 **Nota `pipeline`**: Ignora filtros `desde`, `hasta`, `estado`. Siempre muestra oportunidades con `estado = ACTIVA` (abiertas en el tablero). Usa `ejecutivoId` si está logueado ADMIN/GERENTE.

---

### 8.3 Response

```
HTTP 200 OK
Content-Type:          text/csv | application/json | application/vnd.openxmlformats-officedocument.spreadsheetml.sheet | application/pdf
Content-Disposition:   attachment; filename="<tipo>_YYYYMMDD.<formato>"   (ej: "ventas_20260826.csv")
Content-Length:        <bytes>
Body:                  <binario>
```

En caso de error **NO binario** (400/401/403), el backend responde JSON normal:
```json
{
  "status": 400,
  "error": "PARAMETRO_INVALIDO",
  "mensaje": "Formato no soportado: xyz. Use: csv, json, xlsx, pdf",
  "timestamp": "2026-08-26T10:30:00.123Z"
}
```

---

### 8.4 Los 5 ejemplos del requerimiento (listos para usar)

```bash
# 1) Exportar TODAS las ventas del año 2026 en CSV
GET /api/reportes/export?formato=csv&tipo=ventas&desde=2026-01-01&hasta=2026-12-31

# 2) Exportar SOLO clientes del ejecutivo #5 en CSV
GET /api/reportes/export?formato=csv&tipo=clientes&ejecutivoId=5

# 3) Exportar oportunidades GANADAS en PDF (requerimiento original)
GET /api/reportes/export?formato=pdf&tipo=oportunidades&estado=GANADA

# 4) Exportar PIPELINE global (oportunidades activas) en Excel (sin filtro fechas)
GET /api/reportes/export?formato=xlsx&tipo=pipeline

# 5) Exportar prospectos creados entre enero y agosto 2026 en JSON
GET /api/reportes/export?formato=json&tipo=prospectos&desde=2026-01-01&hasta=2026-08-26
```

---

### 8.5 Ejemplos de código desde el frontend (descarga binaria)

#### 🅰️ Fetch nativo
```ts
async function descargarReporte(params: URLSearchParams) {
  const token = localStorage.getItem("accessToken");
  const res = await fetch(
    `/api/reportes/export?${params.toString()}`,
    {
      method: "GET",
      headers: { Authorization: `Bearer ${token}` },
      // NO pongas 'Content-Type' en GET
    }
  );

  if (!res.ok) {
    const err = await res.json();
    console.error("Error exportando:", err);
    alert(`${err.error} → ${err.mensaje}`);
    return;
  }

  const disposition = res.headers.get("Content-Disposition");
  const match = disposition?.match(/filename="?([^"]+)"?/);
  const filename = match?.[1] ?? `reporte_${Date.now()}.bin`;

  const blob = await res.blob();
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  window.URL.revokeObjectURL(url);
}

// Uso:
descargarReporte(new URLSearchParams({
  formato: "csv",
  tipo: "ventas",
  desde: "2026-01-01",
  hasta: "2026-12-31",
}));
```

#### 🅱️ Axios (con responseType: 'blob' pero parseando errores JSON)
```ts
import axios, { AxiosError } from "axios";

export async function exportarReporte(params: Record<string, any>) {
  const token = localStorage.getItem("accessToken");
  try {
    const res = await axios.get("/api/reportes/export", {
      params,
      headers: { Authorization: `Bearer ${token}` },
      responseType: "blob",
    });

    const disposition = res.headers["content-disposition"];
    const match = /filename="?([^"]+)"?/.exec(disposition ?? "");
    const filename = match?.[1] ?? `reporte_${Date.now()}.bin`;

    const url = window.URL.createObjectURL(new Blob([res.data]));
    const a = document.createElement("a");
    a.href = url;
    a.download = filename;
    a.click();
    window.URL.revokeObjectURL(url);
  } catch (e: any) {
    const err: AxiosError = e;
    if (err.response?.data instanceof Blob) {
      const txt = await (err.response.data as Blob).text();
      let jsonErr: any = { error: "ERROR_DESC", mensaje: txt };
      try { jsonErr = JSON.parse(txt); } catch {}
      console.error("Error export:", jsonErr);
      alert(`${jsonErr.error}: ${jsonErr.mensaje}`);
    } else {
      alert("Error de red: " + (err.message || "desconocido"));
    }
  }
}

// Uso:
exportarReporte({
  formato: "pdf",
  tipo: "oportunidades",
  estado: "GANADA",
  desde: "2026-01-01",
});
```

---

### 8.6 Errores típicos en este endpoint

| HTTP | `error` | Causa | Qué revisar en frontend |
|---|---|---|---|
| 400 | `VALIDACION_FALLIDA` | Falta `formato` o `tipo` | Asegúrate enviar ambos query params, no null/undefined |
| 400 | `PARAMETRO_INVALIDO` | Formato fuera de `[csv,json,xlsx,pdf]` o tipo fuera de `[ventas,clientes,oportunidades,pipeline,prospectos]` | Select cerrado, no permitir valores libres |
| 400 | `PARAMETRO_INVALIDO` | `desde`/`hasta` en formato malo | Siempre `YYYY-MM-DD` (ISO) |
| 401 | `NO_AUTENTICADO` | Sin token / token vencido | Ejecutar flujo refresh token (Sección 3) |
| 403 | `ACCESO_DENEGADO` | Usuario sin login | |

---

### 8.7 Checklist frontend antes de enviar

- [ ] `GET` + query params, **no** `POST` y **nunca** body JSON.
- [ ] Parámetros `formato` y `tipo` **siempre** presentes y minúsculas (el backend normaliza pero buena práctica).
- [ ] Fechas en `YYYY-MM-DD` → JavaScript: `fecha.toISOString().slice(0, 10)`.
- [ ] `ejecutivoId` solo enviar cuando el login actual es **ADMIN** o **GERENTE**. Si es EJECUTIVO no es necesario; el backend sobreescribe.
- [ ] `estado` solo para `tipo=oportunidades`.
- [ ] `responseType: 'blob'` en Axios; o `res.blob()` en Fetch nativo.
- [ ] Los errores 4xx tienen body JSON; hay que **leer el Blob como texto** antes de parsear.
- [ ] **Siempre** `revokeObjectURL` después del click para liberar memoria.
