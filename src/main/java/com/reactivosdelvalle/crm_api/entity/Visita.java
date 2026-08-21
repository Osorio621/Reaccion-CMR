package com.reactivosdelvalle.crm_api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "visitas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Visita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_entidad", nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private TipoEntidadVisita tipoEntidad;

    @Column(name = "cliente_id")
    private Long clienteId;

    @Column(name = "prospecto_id")
    private Long prospectoId;

    @Column(name = "oportunidad_id")
    private Long oportunidadId;

    @Column(name = "ejecutivo_id", nullable = false)
    private Long ejecutivoId;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "objetivo", nullable = false, columnDefinition = "TEXT")
    private String objetivo;

    @Column(name = "necesidad_detectada", nullable = false, columnDefinition = "TEXT")
    private String necesidadDetectada;

    @Column(name = "competencia_mencionada", nullable = false)
    private String competenciaMencionada;

    @Column(name = "resultado_id", nullable = false)
    private Long resultadoId;

    @Column(name = "oportunidad_generada", nullable = false)
    private Boolean oportunidadGenerada;

    @Column(name = "compromiso", nullable = false, columnDefinition = "TEXT")
    private String compromiso;

    @Column(name = "notas_adicionales", columnDefinition = "TEXT")
    private String notasAdicionales;

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
