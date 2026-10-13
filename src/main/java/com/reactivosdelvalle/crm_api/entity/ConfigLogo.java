package com.reactivosdelvalle.crm_api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "config_logo")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigLogo {

    @Id
    private Long id;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "ruta_almacenada", length = 500)
    private String rutaAlmacenada;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
