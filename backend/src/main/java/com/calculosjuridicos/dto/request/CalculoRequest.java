package com.calculosjuridicos.dto.request;

import com.calculosjuridicos.entity.PeriodicidadeJuros;
import com.calculosjuridicos.entity.TipoCalculo;
import com.calculosjuridicos.entity.TipoJuros;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalculoRequest {

    @Builder.Default
    private TipoCalculo tipoCalculo = TipoCalculo.PADRAO;

    @Size(max = 255, message = "Título deve ter no máximo 255 caracteres")
    private String titulo;

    @NotNull(message = "Valor principal é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor principal deve ser maior que zero")
    private BigDecimal valorPrincipal;

    @NotNull(message = "Data inicial é obrigatória")
    private LocalDate dataInicial;

    @NotNull(message = "Data final é obrigatória")
    private LocalDate dataFinal;

    private Long tabelaIndiceId;

    @Builder.Default
    private TipoJuros tipoJuros = TipoJuros.SIMPLES;

    @DecimalMin(value = "0", message = "Taxa de juros não pode ser negativa")
    @DecimalMax(value = "100", message = "Taxa de juros não pode ser maior que 100%")
    private BigDecimal taxaJuros;

    @Builder.Default
    private PeriodicidadeJuros periodicidadeJuros = PeriodicidadeJuros.MENSAL;

    @DecimalMin(value = "0", message = "Multa não pode ser negativa")
    @DecimalMax(value = "100", message = "Multa não pode ser maior que 100%")
    @Builder.Default
    private BigDecimal multaPercentual = BigDecimal.ZERO;

    @DecimalMin(value = "0", message = "Honorários não pode ser negativo")
    @DecimalMax(value = "100", message = "Honorários não pode ser maior que 100%")
    @Builder.Default
    private BigDecimal honorariosPercentual = BigDecimal.ZERO;

    @Builder.Default
    private Boolean jurosSobreCorrigido = true;

    private List<ParcelaRequest> parcelas;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParcelaRequest {

        @Size(max = 255, message = "Descrição deve ter no máximo 255 caracteres")
        private String descricao;

        @NotNull(message = "Valor original da parcela é obrigatório")
        @DecimalMin(value = "0.01", message = "Valor da parcela deve ser maior que zero")
        private BigDecimal valorOriginal;

        @NotNull(message = "Data de vencimento da parcela é obrigatória")
        private LocalDate dataVencimento;

        private Long tabelaIndiceId;
    }
}
