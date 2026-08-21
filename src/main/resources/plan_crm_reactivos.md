# Plan de Desarrollo — CRM Reactivos del Valle

> **Stack definitivo:** Java 21 + Spring Boot 3 (Maven) · React 18 + Vite · PostgreSQL 16 · Docker

---

## 1. Visión y Alcance

Sistema web **separado en backend y frontend** para centralizar clientes, prospectos, oportunidades, visitas, seguimientos, ventas e indicadores. Uso diario por más de 10 ejecutivos, con control por rol y reglas de negocio obligatorias desde el primer día.

### Alcance inicial (MVP+)

| Módulo | Descripción |
|---|---|
| Clientes y Prospectos | CRUD completo con borrado lógico |
| Oportunidades / Pipeline | Regla de oro, kanban por etapas, valor ponderado automático |
| Visitas | Registro con validación de 24 h, 7 campos obligatorios |
| Seguimientos | Bandeja de vencidos, días calculados automáticamente |
| Ventas mensuales | Meta, venta real y forecast por ejecutivo |
| Dashboard gerencial | Indicadores clave para reunión semanal |
| Administración | Usuarios, catálogos, auditoría, respaldos |

### Fuera de alcance (Fase 1)
- Facturación, inventario, integraciones contables, app móvil nativa.

---

## 2. Arquitectura General

```
┌─────────────────────────────────────────────────────────┐
│                    FRONTEND (React + Vite)               │
│  Puerto: 5173 (dev) / Nginx (prod)                      │
│  React 18 · React Router v6 · Axios · TanStack Query    │
│  Recharts · DnD Kit (kanban) · React Hook Form · Zod    │
└────────────────────┬────────────────────────────────────┘
                     │  HTTPS / REST API (JSON)
                     │  Autenticación: JWT (Bearer token)
┌────────────────────▼────────────────────────────────────┐
│                   BACKEND (Java + Spring Boot)           │
│  Puerto: 8080                                           │
│  Spring Boot 3.3 · Spring Security · Spring Data JPA    │
│  Hibernate · Flyway (migraciones) · MapStruct           │
│  SpringDoc OpenAPI (Swagger UI en /swagger-ui.html)     │
└────────────────────┬────────────────────────────────────┘
                     │  JDBC / JPA
┌────────────────────▼────────────────────────────────────┐
│              BASE DE DATOS (PostgreSQL 16)               │
│  Puerto: 5432                                           │
│  Esquema: crm_rv · Backup: pg_dump semanal con fecha    │
└─────────────────────────────────────────────────────────┘
```

### Principios de diseño

- **API REST** documentada con OpenAPI 3 / Swagger.
- **Cálculos en el backend**: valor ponderado, días vencidos, alertas de inactividad — nunca en el cliente.
- **Borrado lógico** en todas las entidades maestras (campo `activo / estado`).
- **Catálogos configurables** por el administrador (etapas, probabilidades, resultados de visita, tipos de cliente).
- **Auditoría** de cambios: quién · qué · cuándo · valor anterior / nuevo.

---

## 3. Stack Tecnológico Detallado

### 3.1 Backend — Java 21 + Spring Boot 3

| Librería / Herramienta | Versión | Propósito |
|---|---|---|
| Java | 21 (LTS) | Lenguaje principal |
| Spring Boot | 3.3.x | Framework principal |
| Spring Web MVC | incluida | Controladores REST |
| Spring Security | incluida | JWT, roles, CORS |
| Spring Data JPA | incluida | Repositorios, entidades |
| Hibernate | incluida | ORM para PostgreSQL |
| Flyway | incluida | Migraciones de base de datos |
| MapStruct | 1.5.x | Conversión Entidad ↔ DTO |
| Lombok | 1.18.x | Reducción de boilerplate |
| SpringDoc OpenAPI | 2.x | Swagger UI automático |
| jjwt (JJWT) | 0.12.x | Generación y validación de JWT |
| PostgreSQL Driver | 42.x | Conector JDBC |
| JUnit 5 + Mockito | incluida | Pruebas unitarias e integración |
| Maven | 3.9.x | Gestión de dependencias y build |

### 3.2 Frontend — React 18 + Vite

| Librería | Versión | Propósito |
|---|---|---|
| React | 18.x | UI declarativa |
| Vite | 5.x | Bundler / dev server |
| React Router | 6.x | Navegación SPA |
| Axios | 1.x | Cliente HTTP (API calls) |
| TanStack Query (React Query) | 5.x | Cache, refetch, estados de carga |
| React Hook Form | 7.x | Manejo de formularios |
| Zod | 3.x | Validación de esquemas en cliente |
| Recharts | 2.x | Gráficas del dashboard |
| DnD Kit | 6.x | Tablero Kanban drag & drop |
| date-fns | 3.x | Cálculo de fechas en cliente |
| Lucide React | última | Iconografía |
| Zustand | 4.x | Estado global (usuario, sesión) |

### 3.3 Infraestructura

| Herramienta | Propósito |
|---|---|
| Docker + Docker Compose | Contenedores para backend, frontend, PostgreSQL |
| Nginx | Proxy inverso / serving del build de React |
| pg_dump (cron) | Backup semanal automático con fecha |
| GitHub / GitLab | Control de versiones + CI/CD básico |
| IntelliJ IDEA | IDE recomendado para backend |
| VS Code | IDE recomendado para frontend |

---

## 4. Estructura de Carpetas

### 4.1 Backend (`crm-reactivos-backend/`)

```
crm-reactivos-backend/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/reactivosdelvalle/crm/
│   │   │   ├── CrmApplication.java
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── ClienteController.java
│   │   │   │   ├── ProspectoController.java
│   │   │   │   ├── OportunidadController.java
│   │   │   │   ├── VisitaController.java
│   │   │   │   ├── SeguimientoController.java
│   │   │   │   ├── VentaController.java
│   │   │   │   ├── DashboardController.java
│   │   │   │   └── AdminController.java
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   ├── entity/
│   │   │   ├── dto/
│   │   │   │   ├── request/
│   │   │   │   └── response/
│   │   │   ├── mapper/
│   │   │   ├── security/
│   │   │   ├── exception/
│   │   │   └── scheduler/
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── db/migration/
│   └── test/
└── Dockerfile
```

### 4.2 Frontend (`crm-reactivos-frontend/`)

```
crm-reactivos-frontend/
├── vite.config.js
├── package.json
├── index.html
├── .env.development
├── .env.production
├── public/
└── src/
    ├── main.jsx
    ├── App.jsx
    ├── api/
    │   ├── axiosClient.js
    │   ├── auth.api.js
    │   ├── clientes.api.js
    │   ├── oportunidades.api.js
    │   ├── visitas.api.js
    │   ├── seguimientos.api.js
    │   ├── ventas.api.js
    │   └── dashboard.api.js
    ├── store/
    │   └── authStore.js
    ├── pages/
    │   ├── Login/
    │   ├── Dashboard/
    │   ├── Pipeline/
    │   ├── Clientes/
    │   ├── Prospectos/
    │   ├── Oportunidades/
    │   ├── Visitas/
    │   ├── Seguimientos/
    │   ├── Ventas/
    │   ├── Reportes/
    │   └── Admin/
    ├── components/
    │   ├── layout/
    │   ├── ui/
    │   ├── kanban/
    │   └── charts/
    ├── hooks/
    └── utils/
```

---

## 5. Modelo de Datos

### 5.1 Entidades principales

| Entidad | Tabla | Campos obligatorios (regla de negocio) |
|---|---|---|
| **Usuario** | `usuarios` | nombre, email, contraseña_hash, rol, activo |
| **Cliente** | `clientes` | nombre, tipo_id, contacto, telefono, email, ejecutivo_id, activo |
| **Prospecto** | `prospectos` | nombre, responsable_id, etapa_id, proxima_accion, fecha_proxima_accion |
| **Oportunidad** | `oportunidades` | cliente_id, ejecutivo_id, valor, etapa_id, probabilidad, fecha_estimada_cierre, proxima_accion |
| **Visita** | `visitas` | prospecto_o_cliente, fecha, objetivo, necesidad, competencia, resultado_id, oportunidad_generada, compromiso |
| **Seguimiento** | `seguimientos` | oportunidad_id, fecha_programada, estado, notas |
| **Venta** | `ventas` | ejecutivo_id, anio, mes, meta, venta_real, forecast |
| **Catálogo** | `catalogos` | tipo, codigo, nombre, orden, activo |
| **Auditoría** | `auditoria` | tabla, registro_id, usuario_id, accion, campo, valor_anterior, valor_nuevo, timestamp |

### 5.2 Campos calculados (solo en backend)

```sql
-- Valor ponderado (Regla 7)
valor_ponderado = valor * (probabilidad / 100.0)

-- Días vencidos (Regla 8)
dias_vencidos = CURRENT_DATE - fecha_programada
  WHERE estado != 'COMPLETADO'

-- Alerta de inactividad (Regla 2)
SELECT ejecutivo_id FROM usuarios
WHERE ultima_actividad < NOW() - INTERVAL '24 hours'
```

### 5.3 Constraints de integridad

- `UNIQUE(ejecutivo_id, anio, mes)` en tabla `ventas` (una fila por ejecutivo/mes).
- `CHECK(fecha >= CURRENT_DATE - INTERVAL '1 day')` en tabla `visitas` (máximo 24 h atrás).
- Todas las tablas maestras: columna `activo BOOLEAN DEFAULT TRUE` (borrado lógico).

---

## 6. API REST — Endpoints principales

```
# Autenticación
POST   /api/auth/login
POST   /api/auth/refresh
POST   /api/auth/logout

# Clientes
GET    /api/clientes
POST   /api/clientes
GET    /api/clientes/{id}
PUT    /api/clientes/{id}
DELETE /api/clientes/{id}          ← borrado lógico

# Prospectos
GET    /api/prospectos
POST   /api/prospectos
PUT    /api/prospectos/{id}

# Oportunidades
GET    /api/oportunidades
POST   /api/oportunidades          ← valida Regla de Oro
PUT    /api/oportunidades/{id}
PATCH  /api/oportunidades/{id}/etapa

# Pipeline
GET    /api/pipeline               ← agrupadas por etapa + valor ponderado

# Visitas
GET    /api/visitas
POST   /api/visitas                ← valida fecha máx. 24 h atrás
PUT    /api/visitas/{id}

# Seguimientos
GET    /api/seguimientos
GET    /api/seguimientos/vencidos  ← con dias_vencidos calculado
POST   /api/seguimientos
PATCH  /api/seguimientos/{id}/cerrar

# Ventas
GET    /api/ventas
POST   /api/ventas                 ← valida unicidad ejecutivo+mes+año
PUT    /api/ventas/{id}

# Dashboard
GET    /api/dashboard/resumen      ← todos los KPIs en una sola llamada

# Administración
GET    /api/admin/catalogos
POST   /api/admin/catalogos
GET    /api/admin/usuarios
POST   /api/admin/usuarios
GET    /api/admin/auditoria
POST   /api/admin/backup           ← dispara pg_dump manualmente
```

---

## 7. Módulos y Pantallas (Frontend)

| # | Pantalla | Componentes clave |
|---|---|---|
| 1 | **Login** | Formulario JWT, redirect por rol |
| 2 | **Dashboard** | Tarjetas KPI, Recharts, tabla de vencidos, alerta inactividad |
| 3 | **Pipeline (Kanban)** | DnD Kit, columnas por etapa, valor ponderado por columna |
| 4 | **Clientes** | Tabla paginada, búsqueda, ficha con historial completo |
| 5 | **Prospectos** | Lista con alertas de acción vencida, filtros |
| 6 | **Oportunidades** | CRUD con validación Regla de Oro, historial de etapas |
| 7 | **Visitas** | Formulario 7 campos obligatorios, validación de fecha ≤ 24 h |
| 8 | **Seguimientos** | Bandeja de vencidos con badge de días, botón "Cerrar" |
| 9 | **Ventas** | Captura mensual, gráfica meta vs real vs forecast |
| 10 | **Reportes** | Exportación CSV/JSON, resumen reunión semanal |
| 11 | **Admin** | Gestión de usuarios, catálogos, auditoría, respaldos |

---

## 8. Roles y Permisos

| Acción | Ejecutivo | Gerente | Admin |
|---|---|---|---|
| Ver sus propios datos | ✅ | ✅ | ✅ |
| Ver datos de otros ejecutivos | ❌ | ✅ | ✅ |
| Crear / editar visitas y seguimientos propios | ✅ | ✅ | ✅ |
| Editar ventas y metas propias | ✅ | ✅ (equipo) | ✅ (todos) |
| Gestionar catálogos | ❌ | Parcial | ✅ |
| Gestionar usuarios | ❌ | ❌ | ✅ |
| Ver auditoría | ❌ | Parcial | ✅ |
| Ejecutar respaldo manual | ❌ | ❌ | ✅ |

> Los permisos se validan **en el backend** con Spring Security + `@PreAuthorize`. El frontend solo oculta opciones de UI; la seguridad real vive en la API.

---

## 9. Indicadores del Dashboard

| KPI | Cálculo |
|---|---|
| Valor total del pipeline | `SUM(valor)` en oportunidades activas |
| Valor ponderado del pipeline | `SUM(valor × probabilidad / 100)` |
| Tasa de conversión | `ventas_cerradas / oportunidades_creadas` |
| Días promedio por etapa | promedio de tiempo entre cambios de etapa |
| Oportunidades que cierran este mes | fecha_estimada_cierre en los próximos 30 días |
| Seguimientos vencidos | `COUNT` donde `dias_vencidos > 0` |
| Visitas últimos 7 días por ejecutivo | agrupación por `ejecutivo_id` |
| Cumplimiento mensual | `venta_real / meta × 100` por ejecutivo |
| Alerta de inactividad | ejecutivos sin actividad en > 24 h |

---

## 10. Reglas de Negocio → Implementación

| # | Regla | Implementación |
|---|---|---|
| 1 | Centralizar todo | Una sola API y base de datos |
| 2 | Actualización diaria | `ultima_actividad` visible; alerta automática si > 24 h |
| 3 | Regla de oro | Validación en `OportunidadService` → 400 si falta algún campo |
| 4 | Reunión semanal | Dashboard con período de la última semana |
| 5 | Prospectos completos | `@NotNull` en responsable, etapa, próxima acción |
| 6 | Visitas completas | 7 campos `@NotBlank`; `@Past(max=1day)` en fecha |
| 7 | Valor ponderado | Calculado en `PipelineService`, expuesto en API |
| 8 | Seguimientos vencidos | `CURRENT_DATE - fecha_programada` en query JPA |
| 9 | Ventas mensuales | `UNIQUE(ejecutivo_id, anio, mes)` + validación en service |
| 10 | Calidad del dato | Borrado lógico, catálogos, auditoría con `@EntityListeners` |
| 11 | Respaldo semanal | `@Scheduled(cron = "0 0 1 * * SUN")` → `pg_dump` con fecha |
| 12 | Evolución | `GET /api/reportes/export?formato=csv\|json` desde Fase 1 |

---

## 11. Flujo de Autenticación (JWT)

```
Frontend                          Backend
   │  POST /api/auth/login           │
   │  { email, password }  ────────► │  Valida credenciales
   │                                  │  Genera JWT (access 1h + refresh 7d)
   │ ◄── { accessToken, refreshToken, usuario, rol }
   │
   │  GET /api/dashboard/resumen     │
   │  Authorization: Bearer <token> ►│  JwtFilter valida
   │ ◄──── { kpis, pipeline, ... }   │  @PreAuthorize por rol
```

### Manejo de errores estándar

```json
{
  "status": 400,
  "error": "VALIDACION_FALLIDA",
  "mensaje": "La oportunidad debe tener valor, etapa, probabilidad, fecha estimada de cierre y próxima acción.",
  "timestamp": "2026-08-19T15:00:00Z",
  "campos": {
    "probabilidad": "Campo obligatorio",
    "fechaEstimadaCierre": "Campo obligatorio"
  }
}
```

---

## 12. Roadmap de Desarrollo (~20 semanas)

| Fase | Entregable | Duración |
|---|---|---|
| **F0. Definición** | Catálogos con equipo comercial (etapas + probabilidades) | 1–2 sem |
| **F1. Fundación técnica** | Docker Compose, Login JWT, roles, Swagger UI | 2 sem |
| **F2. Clientes + Prospectos** | CRUD completo, borrado lógico, catálogos, Flyway | 2 sem |
| **F3. Oportunidades + Pipeline** | Regla de oro, kanban drag & drop, valor ponderado | 3 sem |
| **F4. Visitas + Seguimientos** | Formularios con validaciones de negocio, bandeja vencidos | 2 sem |
| **F5. Ventas + Dashboard** | Metas mensuales, gráficas, todos los KPIs | 3 sem |
| **F6. Calidad + Respaldo** | Auditoría, backup cron, exportación CSV/JSON | 2 sem |
| **F7. Capacitación + Piloto** | 3 ejecutivos en operación, ajustes de UX | 3 sem |
| **F8. Rollout completo** | Los 10+ ejecutivos en operación diaria | 1 sem |
| **F9. Evolución** | Evaluación trimestral de migración a CRM profesional | Continua |

---

## 13. Calidad del Dato y Respaldo

- **Nunca borrar**: borrado lógico (`activo = false`) + entrada en `auditoria`.
- **Auditoría automática**: `@EntityListeners(AuditListener.class)` en todas las entidades.
- **Catálogos**: todos los campos categóricos vienen de la tabla `catalogos`; sin texto libre en campos clave.
- **Backup semanal**: cron cada domingo 01:00 AM → `pg_dump -Fc crm_rv > respaldo_CRM_YYYY-MM-DD.dump.gz`.
- **Retención**: 6 meses; verificación de restauración trimestral en entorno separado.
- **Exportación**: `GET /api/reportes/export?entidad=clientes&formato=csv` disponible desde F6.

---

## 14. Definición de Listo (DoD) por Fase

- [ ] Validaciones de reglas de negocio implementadas en el **backend**.
- [ ] Cobertura de pruebas > 70 % en la capa de servicio.
- [ ] Dashboard con datos reales (no mock).
- [ ] Swagger actualizado para todos los endpoints de la fase.
- [ ] Respaldo automático verificado.
- [ ] Prueba funcional con al menos un usuario final por rol.
- [ ] Aprobación del gerente comercial.

---

## 15. Convenciones del Equipo

### Backend (Java)
- Paquetes: `com.reactivosdelvalle.crm.<capa>`
- Nomenclatura: `PascalCase` clases · `camelCase` métodos · `UPPER_SNAKE_CASE` constantes.
- DTOs separados de entidades; **nunca exponer entidades JPA directamente**.
- Toda la lógica de negocio en la capa `Service`; los `Controller` solo delegan.
- Manejo de excepciones centralizado en `GlobalExceptionHandler`.

### Frontend (React)
- Componentes en `PascalCase` (archivos `.jsx`).
- Hooks personalizados: prefijo `use` → `useOportunidades.js`.
- Llamadas a API **solo desde `src/api/`**; nunca `fetch` directo en componentes.
- Estilos: CSS Modules (`Componente.module.css`) o variables CSS globales.
- No usar `localStorage` para el token; usar Zustand (memory) o cookie `httpOnly`.

### Git
- Ramas: `main` (producción) · `develop` · `feature/<nombre>` · `fix/<nombre>`
- Commits convencionales: `feat:` · `fix:` · `docs:` · `refactor:` · `test:`
- Pull Request obligatorio para fusionar en `develop` o `main`.

---

*Última actualización: 2026-08-19 | Versión: 2.0 | Stack: Java 21 + Spring Boot 3 · React 18 + Vite · PostgreSQL 16*
