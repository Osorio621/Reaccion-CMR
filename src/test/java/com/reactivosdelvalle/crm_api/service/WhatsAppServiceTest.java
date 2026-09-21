package com.reactivosdelvalle.crm_api.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WhatsAppService — normalización de teléfonos")
class WhatsAppServiceTest {

    private final WhatsAppService service = new WhatsAppService();

    @ParameterizedTest(name = "'{0}' → '{1}'")
    @CsvSource({
            "3001234567,   +573001234567",   // móvil sin prefijo
            "573001234567, +573001234567",   // con prefijo 57 sin +
            "+573001234567,+573001234567",   // ya en E.164
            "00573001234567,+573001234567",  // con 0057
            "300 123 4567,  +573001234567",  // con espacios
            "300-123-4567,  +573001234567",  // con guiones
    })
    void normalizarFormatos(String entrada, String esperado) {
        assertThat(service.normalizarTelefono(entrada.trim()))
                .isEqualTo(esperado.trim());
    }

    @Test
    @DisplayName("null retorna null")
    void nulo_retornaNull() {
        assertThat(service.normalizarTelefono(null)).isNull();
    }

    @Test
    @DisplayName("cadena vacía retorna null")
    void vacio_retornaNull() {
        assertThat(service.normalizarTelefono("   ")).isNull();
    }

    @Test
    @DisplayName("formato no reconocido retorna null")
    void formatoDesconocido_retornaNull() {
        assertThat(service.normalizarTelefono("12345")).isNull();
    }
}
