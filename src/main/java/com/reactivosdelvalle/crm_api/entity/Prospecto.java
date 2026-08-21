package com.reactivosdelvalle.crm_api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "prospectos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prospecto {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Column(name = "empresa", length = 200)
    private String empresa;

    @Column(name = "tipo_id")
    private Long tipoId;

    @Column(name = "industria_id")
    private Long industriaId;

    @Column(name = "zona_id")
    private Long zonaId;

    @Column(name = "responsable_id", nullable = false)
    private Long responsableId;

    @Column(name = "etapa_id", nullable = false)
    private Long etapaId;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "sitio_web", length = 300)
    private String sitioWeb;

    @Column(name = "notas", columnDefinition = "TEXT")
    private String notas;

    @Column(name = "proxima_accion", nullable = false, columnDefinition = "TEXT")
    private String proximaAccion;

    @Column(name = "fecha_proxima_accion", nullable = false)
    private LocalDate fechaProximaAccion;

    @Builder.Default
    @Column(name = "convertido", nullable = false)
    private Boolean convertido = false;

    @Column(name = "cliente_id")
    private Long clienteId;

    @Builder.Default
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by_id")
    private Long createdById;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (convertido == null) {
            convertido = false;
        }
        if (activo == null) {
            activo = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}