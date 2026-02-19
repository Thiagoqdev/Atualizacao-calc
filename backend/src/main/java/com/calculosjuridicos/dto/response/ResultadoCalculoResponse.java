package com.calculosjuridicos.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResultadoCalculoResponse {

    private Long calculoId;
    private BigDecimal valorOriginal;
    private BigDecimal valorCorrigido;
    private BigDecimal valorJuros;
    private BigDecimal valorMulta;
    private BigDecimal valorHonorarios;
    private BigDecimal valorTotal;
    private BigDecimal fatorCorrecao;
    private LocalDateTime dataCalculo;

    private List<ResultadoParcelaResponse> parcelas;
    private List<DetalhamentoMensalResponse> detalhamento;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResultadoParcelaResponse {
        private String descricao;
        private BigDecimal valorOriginal;
        private LocalDate dataVencimento;
        private BigDecimal valorCorrigido;
        private BigDecimal valorJuros;
        private BigDecimal subtotal;
        private int mesesJuros;
        private String indiceNome;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetalhamentoMensalResponse {
        private String competencia;
        private BigDecimal indice;
        private BigDecimal fatorAcumulado;
        private BigDecimal valorCorrigidoParcial;
        private BigDecimal jurosParcial;
        private BigDecimal subtotalParcial;
    }
}
