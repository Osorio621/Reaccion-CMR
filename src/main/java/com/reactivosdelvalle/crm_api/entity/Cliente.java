package com.reactivosdelvalle.crm_api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "clientes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Column(name = "razon_social", length = 200)
    private String razonSocial;

    @Column(name = "rfc", length = 20)
    private String rfc;

    @Column(name = "tipo_id")
    private Long tipoId;

    @Column(name = "industria_id")
    private Long industriaId;

    @Column(name = "zona_id")
    private Long zonaId;

    @Column(name = "ejecutivo_id", nullable = false)
    private Long ejecutivoId;

    @Column(name = "telefono_principal", length = 20)
    private String telefonoPrincipal;

    @Column(name = "email_principal", length = 150)
    private String emailPrincipal;

    @Column(name = "sitio_web", length = 300)
    private String sitioWeb;

    @Column(name = "direccion", length = 300)
    private String direccion;

    @Column(name = "ciudad", length = 100)
    private String ciudad;

    @Column(name = "estado_region", length = 100)
    private String estadoRegion;

    @Column(name = "notas", columnDefinition = "TEXT")
    private String notas;

    @Column(name = "fecha_primera_compra")
    private LocalDate fechaPrimeraCompra;

    @Builder.Default
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by_id")
    private Long createdById;

    @Column(name = "updated_by_id")
    private Long updatedById;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (activo == null) {
            activo = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}