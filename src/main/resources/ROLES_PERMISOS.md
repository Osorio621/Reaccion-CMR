# Roles y Permisos — CRM Reactivos del Valle

> **Stack:** Spring Security 6 · JWT · `@PreAuthorize` · Row-Level Filtering

---

## Tres capas de seguridad

```
┌─────────────────────────────────────────────────────────────────┐
│  CAPA 1 — AUTENTICACIÓN                                         │
│  ¿Quién eres? → JWT válido + usuario activo                     │
├─────────────────────────────────────────────────────────────────┤
│  CAPA 2 — AUTORIZACIÓN DE ACCIÓN (por ROL)                      │
│  ¿Puedes hacer esta operación? → @PreAuthorize por endpoint     │
├─────────────────────────────────────────────────────────────────┤
│  CAPA 3 — FILTRO DE DATOS (por PROPIEDAD)                       │
│  ¿Sobre qué registros? → ejecutivo_id = yo, o gerente/admin     │
└─────────────────────────────────────────────────────────────────┘
```

La seguridad real vive **siempre en el backend**. El frontend solo oculta opciones de UI como mejora de experiencia.

---

## Roles del sistema

| Rol | Código | Descripción |
|---|---|---|
| **Administrador** | `ADMIN` | Control total: usuarios, catálogos, auditoría, respaldos |
| **Gerente** | `GERENTE` | Ve y gestiona a todo el equipo; edita metas |
| **Ejecutivo** | `EJECUTIVO` | Solo ve y opera sus propios registros |

---

## JWT — Estructura del token

El token que el backend emite al hacer login contiene:

```json
{
  "sub": "42",
  "email": "juan.perez@reactivosdelvalle.com",
  "nombre": "Juan Pérez",
  "rol": "EJECUTIVO",
  "iat": 1724095200,
  "exp": 1724098800
}
```

- **`sub`** → ID del usuario (se usa para filtrar datos propios).
- **`rol`** → Se convierte en `GrantedAuthority` en Spring Security.
- **Access token:** 1 hora · **Refresh token:** 7 días (en cookie httpOnly).

---

## Matriz de Permisos por Módulo

### Clientes

| Operación | EJECUTIVO | GERENTE | ADMIN |
|---|---|---|---|
| Listar (ver) | Solo los suyos | Todos | Todos |
| Ver detalle | Solo los suyos | Todos | Todos |
| Crear | ✅ | ✅ | ✅ |
| Editar | Solo los suyos | Todos | Todos |
| Desactivar (borrado lógico) | ❌ | ✅ | ✅ |
| Reasignar ejecutivo | ❌ | ✅ | ✅ |

### Prospectos

| Operación | EJECUTIVO | GERENTE | ADMIN |
|---|---|---|---|
| Listar | Solo los suyos | Todos | Todos |
| Crear | ✅ | ✅ | ✅ |
| Editar | Solo los suyos | Todos | Todos |
| Convertir a cliente | Solo los suyos | Todos | Todos |
| Desactivar | ❌ | ✅ | ✅ |

### Oportunidades / Pipeline

| Operación | EJECUTIVO | GERENTE | ADMIN |
|---|---|---|---|
| Ver pipeline | Solo las suyas | Todo el equipo | Todo |
| Crear oportunidad | ✅ | ✅ | ✅ |
| Editar (campos) | Solo las suyas | Todos | Todos |
| Cambiar etapa | Solo las suyas | Todos | Todos |
| Cerrar como ganada/perdida | Solo las suyas | Todos | Todos |
| Reasignar ejecutivo | ❌ | ✅ | ✅ |

### Visitas

| Operación | EJECUTIVO | GERENTE | ADMIN |
|---|---|---|---|
| Ver visitas | Solo las suyas | Todas | Todas |
| Registrar visita | ✅ | ✅ | ✅ |
| Editar visita propia | Solo las suyas | Todas | Todas |
| Eliminar (lógico) | ❌ | ✅ | ✅ |

### Seguimientos

| Operación | EJECUTIVO | GERENTE | ADMIN |
|---|---|---|---|
| Ver seguimientos | Solo los suyos | Todos | Todos |
| Crear seguimiento | ✅ | ✅ | ✅ |
| Cerrar seguimiento | Solo los suyos | Todos | Todos |
| Ver bandeja de vencidos del equipo | ❌ | ✅ | ✅ |

### Ventas

| Operación | EJECUTIVO | GERENTE | ADMIN |
|---|---|---|---|
| Ver su propio registro mensual | ✅ | ✅ | ✅ |
| Ver registros de otros ejecutivos | ❌ | ✅ | ✅ |
| Registrar / editar su meta y forecast | ✅ | ✅ | ✅ |
| Editar meta de otro ejecutivo | ❌ | ✅ | ✅ |

### Dashboard

| Vista | EJECUTIVO | GERENTE | ADMIN |
|---|---|---|---|
| KPIs propios (pipeline, visitas, vencidos) | ✅ | ✅ | ✅ |
| KPIs de todo el equipo | ❌ | ✅ | ✅ |
| Alerta de inactividad de otros ejecutivos | ❌ | ✅ | ✅ |

### Administración

| Operación | EJECUTIVO | GERENTE | ADMIN |
|---|---|---|---|
| Gestionar catálogos | ❌ | ❌ | ✅ |
| Crear / desactivar usuarios | ❌ | ❌ | ✅ |
| Ver auditoría | ❌ | Parcial (su equipo) | Completa |
| Disparar respaldo manual | ❌ | ❌ | ✅ |
| Descargar respaldo | ❌ | ❌ | ✅ |

---

## Implementación Backend (Spring Security)

### Configuración base

```java
// SecurityConfig.java
@Configuration
@EnableMethodSecurity          // habilita @PreAuthorize en controllers
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

### Constantes de roles

```java
// Roles.java
public final class Roles {
    public static final String ADMIN    = "ADMIN";
    public static final String GERENTE  = "GERENTE";
    public static final String EJECUTIVO = "EJECUTIVO";

    // Expresiones reutilizables para @PreAuthorize
    public static final String ADMIN_O_GERENTE  = "hasAnyRole('ADMIN','GERENTE')";
    public static final String CUALQUIER_ROL    = "isAuthenticated()";
}
```

### Controllers con `@PreAuthorize`

```java
// ClienteController.java
@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    // Cualquier usuario autenticado puede listar
    // El servicio filtra por rol internamente
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Page<ClienteResponse> listar(Pageable pageable,
                                         Authentication auth) {
        return clienteService.listar(pageable, auth);
    }

    // Cualquiera puede crear
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ClienteResponse crear(@Valid @RequestBody ClienteRequest req,
                                  Authentication auth) {
        return clienteService.crear(req, auth);
    }

    // Editar: el servicio verifica si es suyo o si es gerente/admin
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ClienteResponse editar(@PathVariable Long id,
                                   @Valid @RequestBody ClienteRequest req,
                                   Authentication auth) {
        return clienteService.editar(id, req, auth);
    }

    // Desactivar: solo gerente o admin
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public void desactivar(@PathVariable Long id) {
        clienteService.desactivar(id);
    }

    // Reasignar: solo gerente o admin
    @PatchMapping("/{id}/reasignar")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ClienteResponse reasignar(@PathVariable Long id,
                                      @RequestParam Long nuevoEjecutivoId) {
        return clienteService.reasignar(id, nuevoEjecutivoId);
    }
}
```

### Servicio con filtro de datos por propiedad (Capa 3)

```java
// ClienteService.java
@Service
public class ClienteService {

    public Page<ClienteResponse> listar(Pageable pageable, Authentication auth) {
        UsuarioPrincipal usuario = (UsuarioPrincipal) auth.getPrincipal();

        // EJECUTIVO solo ve los suyos
        if (usuario.getRol() == Rol.EJECUTIVO) {
            return clienteRepo
                .findByEjecutivoIdAndActivoTrue(usuario.getId(), pageable)
                .map(mapper::toResponse);
        }

        // GERENTE y ADMIN ven todos
        return clienteRepo
            .findByActivoTrue(pageable)
            .map(mapper::toResponse);
    }

    public ClienteResponse editar(Long id, ClienteRequest req, Authentication auth) {
        Cliente cliente = clienteRepo.findById(id)
            .orElseThrow(() -> new AppException("Cliente no encontrado", 404));

        UsuarioPrincipal usuario = (UsuarioPrincipal) auth.getPrincipal();

        // EJECUTIVO solo puede editar sus propios clientes
        if (usuario.getRol() == Rol.EJECUTIVO
                && !cliente.getEjecutivoId().equals(usuario.getId())) {
            throw new AppException("No tienes permiso para editar este cliente", 403);
        }

        // aplica cambios
        mapper.updateFromRequest(req, cliente);
        return mapper.toResponse(clienteRepo.save(cliente));
    }
}
```

### Helper para resolver el usuario actual

```java
// SecurityUtils.java
@Component
public class SecurityUtils {

    /**
     * Obtiene el usuario autenticado del contexto de Spring Security.
     */
    public UsuarioPrincipal getUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (UsuarioPrincipal) auth.getPrincipal();
    }

    public boolean esAdmin() {
        return getUsuarioActual().getRol() == Rol.ADMIN;
    }

    public boolean esGerenteOAdmin() {
        Rol rol = getUsuarioActual().getRol();
        return rol == Rol.GERENTE || rol == Rol.ADMIN;
    }

    public boolean esPropietario(Long ejecutivoId) {
        return getUsuarioActual().getId().equals(ejecutivoId);
    }

    public boolean puedeAccederA(Long ejecutivoId) {
        return esGerenteOAdmin() || esPropietario(ejecutivoId);
    }
}
```

### Ejemplo: Oportunidades (Regla de Oro + filtro)

```java
// OportunidadController.java
@RestController
@RequestMapping("/api/oportunidades")
public class OportunidadController {

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<OportunidadResponse> listar(Authentication auth) {
        return oportunidadService.listar(auth);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public OportunidadResponse crear(@Valid @RequestBody OportunidadRequest req,
                                      Authentication auth) {
        // @Valid aplica la Regla de Oro a nivel de DTO
        return oportunidadService.crear(req, auth);
    }

    @PatchMapping("/{id}/etapa")
    @PreAuthorize("isAuthenticated()")
    public OportunidadResponse cambiarEtapa(@PathVariable Long id,
                                             @RequestBody CambioEtapaRequest req,
                                             Authentication auth) {
        return oportunidadService.cambiarEtapa(id, req, auth);
    }

    @PatchMapping("/{id}/reasignar")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")   // solo gerente/admin
    public OportunidadResponse reasignar(@PathVariable Long id,
                                          @RequestParam Long nuevoEjecutivoId) {
        return oportunidadService.reasignar(id, nuevoEjecutivoId);
    }
}
```

### DTO con validación de Regla de Oro

```java
// OportunidadRequest.java
public class OportunidadRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotNull(message = "El cliente es obligatorio")
    private Long clienteId;

    @NotNull(message = "La etapa es obligatoria")
    private Long etapaId;

    @NotNull(message = "El valor es obligatorio")
    @DecimalMin(value = "0.01", message = "El valor debe ser mayor a cero")
    private BigDecimal valor;

    @NotNull(message = "La probabilidad es obligatoria")
    @Min(0) @Max(100)
    private Integer probabilidad;

    @NotNull(message = "La fecha estimada de cierre es obligatoria")
    @Future(message = "La fecha de cierre debe ser futura")
    private LocalDate fechaEstimadaCierre;

    @NotBlank(message = "La próxima acción es obligatoria")
    private String proximaAccion;

    @NotNull(message = "La fecha de próxima acción es obligatoria")
    private LocalDate fechaProximaAccion;

    // campos opcionales
    private Long prospectoId;
    private String descripcion;
    private String competencia;
}
```

---

## Flujo completo de una petición

```
1. Frontend envía: GET /api/oportunidades
   Authorization: Bearer eyJhbGci...

2. JwtFilter intercepta la petición:
   → Extrae y valida el token
   → Carga el usuario desde la BD (verifica activo = true)
   → Coloca UsuarioPrincipal en SecurityContext

3. Spring Security evalúa @PreAuthorize("isAuthenticated()")
   → OK (token válido)

4. OportunidadController.listar(auth) delega a OportunidadService

5. OportunidadService evalúa el rol:
   if EJECUTIVO  → WHERE ejecutivo_id = {mi id} AND activo = true
   if GERENTE    → WHERE activo = true
   if ADMIN      → WHERE activo = true  (mismo que gerente en este caso)

6. Devuelve solo los datos autorizados
```

---

## Implementación Frontend (React)

### Store de autenticación (Zustand)

```js
// store/authStore.js
import { create } from 'zustand';

export const useAuthStore = create((set) => ({
  usuario: null,
  token: null,
  rol: null,

  login: (data) => set({
    usuario: data.usuario,
    token: data.accessToken,
    rol: data.usuario.rol,          // 'ADMIN' | 'GERENTE' | 'EJECUTIVO'
  }),

  logout: () => set({ usuario: null, token: null, rol: null }),
}));
```

### Hook de permisos

```js
// hooks/usePermisos.js
import { useAuthStore } from '../store/authStore';

export function usePermisos() {
  const rol = useAuthStore((s) => s.rol);

  return {
    esAdmin:          rol === 'ADMIN',
    esGerente:        rol === 'GERENTE',
    esEjecutivo:      rol === 'EJECUTIVO',
    esGerenteOAdmin:  rol === 'ADMIN' || rol === 'GERENTE',
    puedeVerEquipo:   rol === 'ADMIN' || rol === 'GERENTE',
    puedeGestionarCatalogos: rol === 'ADMIN',
    puedeDesactivar:  rol === 'ADMIN' || rol === 'GERENTE',
    puedeReasignar:   rol === 'ADMIN' || rol === 'GERENTE',
  };
}
```

### Componente Guard de ruta

```jsx
// components/auth/RolGuard.jsx
import { Navigate } from 'react-router-dom';
import { usePermisos } from '../../hooks/usePermisos';

/**
 * Bloquea el acceso a rutas según el rol.
 * Si el usuario no cumple, redirige a /403.
 */
export function RolGuard({ roles, children }) {
  const { esAdmin, esGerente, esEjecutivo } = usePermisos();

  const roleMap = { ADMIN: esAdmin, GERENTE: esGerente, EJECUTIVO: esEjecutivo };
  const tieneAcceso = roles.some(r => roleMap[r]);

  if (!tieneAcceso) {
    return <Navigate to="/403" replace />;
  }
  return children;
}
```

### Uso en el Router

```jsx
// App.jsx
<Routes>
  <Route path="/login"    element={<Login />} />

  {/* Cualquier usuario autenticado */}
  <Route path="/dashboard"     element={<RequireAuth><Dashboard /></RequireAuth>} />
  <Route path="/clientes"      element={<RequireAuth><Clientes /></RequireAuth>} />
  <Route path="/oportunidades" element={<RequireAuth><Oportunidades /></RequireAuth>} />

  {/* Solo gerente o admin */}
  <Route path="/reportes" element={
    <RolGuard roles={['ADMIN','GERENTE']}>
      <Reportes />
    </RolGuard>
  } />

  {/* Solo admin */}
  <Route path="/admin" element={
    <RolGuard roles={['ADMIN']}>
      <Admin />
    </RolGuard>
  } />

  <Route path="/403" element={<NoAutorizado />} />
</Routes>
```

### Mostrar/ocultar botones según rol

```jsx
// pages/Clientes/ClienteDetalle.jsx
import { usePermisos } from '../../hooks/usePermisos';

export function ClienteDetalle({ cliente }) {
  const { puedeDesactivar, puedeReasignar } = usePermisos();

  return (
    <div>
      <h1>{cliente.nombre}</h1>

      {/* Botón editar: visible para todos */}
      <Button>Editar</Button>

      {/* Solo gerente / admin */}
      {puedeReasignar && (
        <Button variant="secondary">Reasignar ejecutivo</Button>
      )}

      {puedeDesactivar && (
        <Button variant="danger">Desactivar cliente</Button>
      )}
    </div>
  );
}
```

---

## Errores de autorización — Respuestas estándar

```json
// 401 — No autenticado (token ausente o expirado)
{
  "status": 401,
  "error": "NO_AUTENTICADO",
  "mensaje": "Token inválido o expirado. Por favor inicie sesión nuevamente."
}

// 403 — Autenticado pero sin permiso
{
  "status": 403,
  "error": "ACCESO_DENEGADO",
  "mensaje": "No tienes permiso para realizar esta operación."
}

// 403 — Datos de otro ejecutivo
{
  "status": 403,
  "error": "ACCESO_DENEGADO",
  "mensaje": "Este registro pertenece a otro ejecutivo."
}
```

---

## Resumen visual de las 3 capas

```
PETICIÓN HTTP
     │
     ▼
┌──────────────────────────────────┐
│ CAPA 1 — JwtFilter               │  ¿Token válido y usuario activo?
│                                  │  NO → 401 Unauthorized
└──────────────┬───────────────────┘
               │ SÍ
               ▼
┌──────────────────────────────────┐
│ CAPA 2 — @PreAuthorize           │  ¿El ROL puede hacer esta acción?
│                                  │  NO → 403 Forbidden
└──────────────┬───────────────────┘
               │ SÍ
               ▼
┌──────────────────────────────────┐
│ CAPA 3 — Service (filtro de      │  EJECUTIVO → WHERE ejecutivo_id = yo
│          datos por propiedad)    │  GERENTE/ADMIN → sin filtro
└──────────────┬───────────────────┘
               │
               ▼
         RESPUESTA 200
         (solo datos autorizados)
```

---

## Tabla de referencia rápida

| Escenario | Capa que actúa | Código de respuesta |
|---|---|---|
| Token ausente o expirado | Capa 1 (JwtFilter) | 401 |
| Token válido, usuario inactivo | Capa 1 (JwtFilter) | 401 |
| Ejecutivo intenta acceder a `/api/admin` | Capa 2 (@PreAuthorize) | 403 |
| Ejecutivo edita cliente de otro ejecutivo | Capa 3 (Service) | 403 |
| Ejecutivo consulta sus propios datos | Capas 1 + 3 | 200 (filtrado) |
| Gerente consulta todo el equipo | Capas 1 + 2 | 200 (sin filtro) |

---

*Última actualización: 2026-08-19 | Versión: 1.0 | CRM Reactivos del Valle*
