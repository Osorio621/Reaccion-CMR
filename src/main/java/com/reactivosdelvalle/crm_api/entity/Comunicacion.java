package com.reactivosdelvalle.crm_api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "comunicaciones")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comunicacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "entidad_tipo", nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private TipoEntidadComunicacion entidadTipo;

    @Column(name = "entidad_id", nullable = false)
    private Long entidadId;

    @Column(name = "tipo", nullable = false, length = 50)
    private String tipo;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "resultado", length = 300)
    private String resultado;

    @Column(name = "ejecutivo_id")
    private Long ejecutivoId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
