package com.reactivosdelvalle.crm_api.mapper;

import com.reactivosdelvalle.crm_api.dto.response.VentaResponse;
import com.reactivosdelvalle.crm_api.entity.Venta;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * El mapa de nombres de usuarios se resuelve por lotes en el servicio
 * para evitar el problema N+1.
 */
@Mapper(componentModel = "spring")
public interface VentaMapper {

    @Mapping(target = "id", source = "venta.id")
    @Mapping(target = "ejecutivoId", source = "venta.ejecutivoId")
    @Mapping(target = "ejecutivoNombre", expression = "java(usuariosNombres.get(venta.getEjecutivoId()))")
    @Mapping(target = "anio", source = "venta.anio")
    @Mapping(target = "mes", source = "venta.mes")
    @Mapping(target = "meta", source = "venta.meta")
    @Mapping(target = "ventaReal", source = "venta.ventaReal")
    @Mapping(target = "forecast", source = "venta.forecast")
    @Mapping(target = "porcentajeCumplimiento", expression = "java(VentaMapper.calcularPorcentaje(venta))")
    @Mapping(target = "notas", source = "venta.notas")
    @Mapping(target = "createdAt", source = "venta.createdAt")
    @Mapping(target = "updatedAt", source = "venta.updatedAt")
    @Mapping(target = "updatedById", source = "venta.updatedById")
    @Mapping(target = "updatedByNombre", expression = "java(usuariosNombres.get(venta.getUpdatedById()))")
    VentaResponse toResponse(Venta venta, Map<Long, String> usuariosNombres);

    static BigDecimal calcularPorcentaje(Venta venta) {
        if (venta.getMeta() == null || venta.getMeta().compareTo(BigDecimal.ZERO) <= 0 || venta.getVentaReal() == null) {
            return BigDecimal.ZERO;
        }
        return venta.getVentaReal()
                .divide(venta.getMeta(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
}
