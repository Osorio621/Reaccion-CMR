package com.reactivosdelvalle.crm_api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "oportunidad_etapas_hist")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OportunidadEtapaHist {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "oportunidad_id", nullable = false)
    private Long oportunidadId;

    @Column(name = "etapa_anterior_id")
    private Long etapaAnteriorId;

    @Column(name = "etapa_nueva_id", nullable = false)
    private Long etapaNuevaId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "notas", columnDefinition = "TEXT")
    private String notas;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}