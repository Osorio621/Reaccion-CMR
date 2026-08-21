package com.reactivosdelvalle.crm_api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "seguimientos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seguimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "oportunidad_id", nullable = false)
    private Long oportunidadId;

    @Column(name = "ejecutivo_id", nullable = false)
    private Long ejecutivoId;

    @Column(name = "tipo", nullable = false)
    private String tipo;

    @Column(name = "fecha_programada", nullable = false)
    private LocalDate fechaProgramada;

    @Column(name = "fecha_realizada")
    private LocalDate fechaRealizada;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Builder.Default
    private EstadoSeguimiento estado = EstadoSeguimiento.PENDIENTE;

    @Column(name = "notas", columnDefinition = "TEXT")
    private String notas;

    @Column(name = "proxima_accion", columnDefinition = "TEXT")
    private String proximaAccion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Transient
    public long getDiasVencidos() {
        if (estado != EstadoSeguimiento.COMPLETADO && estado != EstadoSeguimiento.CANCELADO 
                && fechaProgramada != null && fechaProgramada.isBefore(LocalDate.now())) {
            return ChronoUnit.DAYS.between(fechaProgramada, LocalDate.now());
        }
        return 0;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (estado == null) {
            estado = EstadoSeguimiento.PENDIENTE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
