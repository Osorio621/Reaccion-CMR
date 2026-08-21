package com.reactivosdelvalle.crm_api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "oportunidades")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Oportunidad {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Column(name = "prospecto_id")
    private Long prospectoId;

    @Column(name = "ejecutivo_id", nullable = false)
    private Long ejecutivoId;

    @Column(name = "etapa_id", nullable = false)
    private Long etapaId;

    @Column(name = "valor", nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Column(name = "probabilidad", nullable = false)
    private Integer probabilidad;

    @Column(name = "valor_ponderado", precision = 15, scale = 2, insertable = false, updatable = false)
    private BigDecimal valorPonderado;

    @Column(name = "fecha_estimada_cierre", nullable = false)
    private LocalDate fechaEstimadaCierre;

    @Column(name = "proxima_accion", nullable = false, columnDefinition = "TEXT")
    private String proximaAccion;

    @Column(name = "fecha_proxima_accion", nullable = false)
    private LocalDate fechaProximaAccion;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "competencia", length = 300)
    private String competencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Builder.Default
    private EstadoOportunidad estado = EstadoOportunidad.ACTIVA;

    @Column(name = "motivo_perdida", columnDefinition = "TEXT")
    private String motivoPerdida;

    @Column(name = "fecha_cierre_real")
    private LocalDate fechaCierreReal;

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
        if (estado == null) {
            estado = EstadoOportunidad.ACTIVA;
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