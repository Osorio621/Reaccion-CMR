package com.reactivosdelvalle.crm_api.dto.response;

import com.reactivosdelvalle.crm_api.entity.RolUsuario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioResponse {
    private Long id;
    private String nombre;
    private String apellido;
    private String email;
    private RolUsuario rol;
    private String telefono;
    private String fotoUrl;
    private Boolean activo;
}
