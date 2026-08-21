package com.reactivosdelvalle.crm_api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ventas", uniqueConstraints = {
    @UniqueConstraint(name = "uk_venta_ejecutivo_periodo", columnNames = {"ejecutivo_id", "anio", "mes"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ejecutivo_id", nullable = false)
    private Long ejecutivoId;

    @Column(name = "updated_by_id")
    private Long updatedById;

    @Column(name = "anio", nullable = false)
    private Integer anio;

    @Column(name = "mes", nullable = false)
    private Integer mes;

    @Column(name = "meta", nullable = false)
    private BigDecimal meta;

    @Column(name = "venta_real", nullable = false)
    @Builder.Default
    private BigDecimal ventaReal = BigDecimal.ZERO;

    @Column(name = "forecast", nullable = false)
    @Builder.Default
    private BigDecimal forecast = BigDecimal.ZERO;

    @Column(name = "notas", columnDefinition = "TEXT")
    private String notas;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (ventaReal == null) {
            ventaReal = BigDecimal.ZERO;
        }
        if (forecast == null) {
            forecast = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
