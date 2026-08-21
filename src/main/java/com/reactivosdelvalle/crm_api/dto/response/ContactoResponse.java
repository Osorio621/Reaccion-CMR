package com.reactivosdelvalle.crm_api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ContactoResponse {

    private Long id;
    private Long clienteId;
    private String nombre;
    private String cargo;
    private String telefono;
    private String email;
    private Boolean esPrincipal;
    private Boolean activo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}