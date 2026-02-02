package com.calculosjuridicos.service;

import com.calculosjuridicos.entity.PeriodicidadeJuros;
import com.calculosjuridicos.entity.TipoJuros;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
public class JurosService {

    private static final int PRECISION = 10;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final MathContext MC = new MathContext(PRECISION, ROUNDING);

    private static final BigDecimal DIAS_MES = new BigDecimal("30");
    private static final BigDecimal MESES_ANO = new BigDecimal("12");
    private static final BigDecimal CEM = new BigDecimal("100");

    /**
     * Calcula juros de acordo com o tipo (simples ou composto).
     *
     * @param principal       Valor base para cálculo dos juros
     * @param taxaPercentual  Taxa de juros em percentual (ex: 1.0 = 1%)
     * @param tipoJuros       SIMPLES ou COMPOSTO
     * @param dataInicial     Data inicial do período
     * @param dataFinal       Data final do período
     * @param periodicidade   DIARIO, MENSAL ou ANUAL
     * @return Valor dos juros calculados
     */
    public BigDecimal calcular(BigDecimal principal,
                               BigDecimal taxaPercentual,
                               TipoJuros tipoJuros,
                               LocalDate dataInicial,
                               LocalDate dataFinal,
                               PeriodicidadeJuros periodicidade) {

        if (principal == null || principal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        if (taxaPercentual == null || taxaPercentual.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        if (tipoJuros == TipoJuros.SIMPLES) {
            return calcularJurosSimples(principal, taxaPercentual, dataInicial, dataFinal, periodicidade);
        } else {
            return calcularJurosCompostos(principal, taxaPercentual, dataInicial, dataFinal, periodicidade);
        }
    }

    /**
     * Calcula juros simples: J = P × i × t
     */
    public BigDecimal calcularJurosSimples(BigDecimal principal,
                                           BigDecimal taxaPercentual,
                                           LocalDate dataInicial,
                                           LocalDate dataFinal,
                                           PeriodicidadeJuros periodicidade) {

        BigDecimal taxa = taxaPercentual.divide(CEM, PRECISION, ROUNDING);
        long periodos = calcularPeriodos(dataInicial, dataFinal, periodicidade);
        BigDecimal taxaAjustada = ajustarTaxaParaPeriodicidade(taxa, periodicidade);

        // J = P × i × t
        return principal
            .multiply(taxaAjustada)
            .multiply(new BigDecimal(periodos))
            .setScale(2, ROUNDING);
    }

    /**
     * Calcula juros compostos: J = P × [(1 + i)^t - 1]
     */
    public BigDecimal calcularJurosCompostos(BigDecimal principal,
                                             BigDecimal taxaPercentual,
                                             LocalDate dataInicial,
                                             LocalDate dataFinal,
                                             PeriodicidadeJuros periodicidade) {

        BigDecimal taxa = taxaPercentual.divide(CEM, PRECISION, ROUNDING);
        long periodos = calcularPeriodos(dataInicial, dataFinal, periodicidade);
        BigDecimal taxaAjustada = ajustarTaxaParaPeriodicidade(taxa, periodicidade);

        if (periodos <= 0) {
            return BigDecimal.ZERO;
        }

        // M = P × (1 + i)^n
        BigDecimal fator = BigDecimal.ONE.add(taxaAjustada);
        BigDecimal montante;

        if (periodos <= Integer.MAX_VALUE) {
            montante = principal.multiply(fator.pow((int) periodos, MC));
        } else {
            // Para períodos muito grandes, usar logaritmo
            double fatorDouble = Math.pow(fator.doubleValue(), periodos);
            montante = principal.multiply(new BigDecimal(fatorDouble, MC));
        }

        // J = M - P
        return montante.subtract(principal).setScale(2, ROUNDING);
    }

    /**
     * Calcula juros pro rata die (proporcionais por dia).
     *
     * @param principal      Valor base
     * @param taxaMensal     Taxa mensal em percentual (ex: 1.0 = 1% a.m.)
     * @param dias           Número de dias
     * @return Valor dos juros proporcionais
     */
    public BigDecimal calcularProRataDie(BigDecimal principal,
                                         BigDecimal taxaMensal,
                                         int dias) {

        if (principal == null || principal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        if (taxaMensal == null || taxaMensal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // Taxa diária = Taxa mensal / 30
        BigDecimal taxaDiaria = taxaMensal
            .divide(CEM, PRECISION, ROUNDING)
            .divide(DIAS_MES, PRECISION, ROUNDING);

        // Juros = Principal × Taxa diária × Dias
        return principal
            .multiply(taxaDiaria)
            .multiply(new BigDecimal(dias))
            .setScale(2, ROUNDING);
    }

    /**
     * Calcula o número de períodos entre duas datas.
     */
    private long calcularPeriodos(LocalDate dataInicial,
                                  LocalDate dataFinal,
                                  PeriodicidadeJuros periodicidade) {

        return switch (periodicidade) {
            case DIARIO -> ChronoUnit.DAYS.between(dataInicial, dataFinal);
            case MENSAL -> ChronoUnit.MONTHS.between(dataInicial, dataFinal);
            case ANUAL -> ChronoUnit.YEARS.between(dataInicial, dataFinal);
        };
    }

    /**
     * Ajusta a taxa mensal para a periodicidade especificada.
     * A taxa de entrada é sempre mensal.
     */
    private BigDecimal ajustarTaxaParaPeriodicidade(BigDecimal taxaMensal,
                                                    PeriodicidadeJuros periodicidade) {

        return switch (periodicidade) {
            case DIARIO -> taxaMensal.divide(DIAS_MES, PRECISION, ROUNDING);
            case MENSAL -> taxaMensal;
            case ANUAL -> taxaMensal.multiply(MESES_ANO);
        };
    }
}
