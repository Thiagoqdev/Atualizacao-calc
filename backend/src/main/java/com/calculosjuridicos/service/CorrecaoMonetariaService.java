package com.calculosjuridicos.service;

import com.calculosjuridicos.entity.ValorIndice;
import com.calculosjuridicos.exception.BusinessException;
import com.calculosjuridicos.repository.ValorIndiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CorrecaoMonetariaService {

    private final ValorIndiceRepository valorIndiceRepository;

    private static final int PRECISION = 10;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    /**
     * Calcula a correção monetária de um valor entre duas datas.
     *
     * @param valorOriginal   Valor a ser corrigido
     * @param dataInicial     Data inicial (mês do valor original)
     * @param dataFinal       Data final (mês de atualização)
     * @param tabelaIndiceId  ID da tabela de índices a ser usada
     * @return Valor corrigido monetariamente
     */
    public BigDecimal calcular(BigDecimal valorOriginal,
                               LocalDate dataInicial,
                               LocalDate dataFinal,
                               Long tabelaIndiceId) {

        if (valorOriginal == null || valorOriginal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        if (dataInicial.isAfter(dataFinal)) {
            throw new BusinessException("Data inicial não pode ser posterior à data final");
        }

        // Buscar índice do mês anterior à data inicial
        LocalDate competenciaInicial = dataInicial.withDayOfMonth(1).minusMonths(1);
        ValorIndice indiceInicial = valorIndiceRepository
            .findByTabelaIndiceIdAndCompetenciaLessThanEqual(tabelaIndiceId, competenciaInicial)
            .orElseThrow(() -> new BusinessException(
                "Índice não encontrado para a competência: " + competenciaInicial
            ));

        // Buscar índice do mês da data final
        LocalDate competenciaFinal = dataFinal.withDayOfMonth(1);
        ValorIndice indiceFinal = valorIndiceRepository
            .findByTabelaIndiceIdAndCompetenciaLessThanEqual(tabelaIndiceId, competenciaFinal)
            .orElseThrow(() -> new BusinessException(
                "Índice não encontrado para a competência: " + competenciaFinal
            ));

        return calcularComIndices(valorOriginal, indiceInicial.getValor(), indiceFinal.getValor());
    }

    /**
     * Calcula a correção monetária dados os índices inicial e final.
     */
    public BigDecimal calcularComIndices(BigDecimal valorOriginal,
                                         BigDecimal indiceInicial,
                                         BigDecimal indiceFinal) {

        if (indiceInicial.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessException("Índice inicial não pode ser zero");
        }

        // Fator de correção = Índice Final / Índice Inicial
        BigDecimal fatorCorrecao = indiceFinal.divide(indiceInicial, PRECISION, ROUNDING);

        // Valor corrigido = Valor Original × Fator de Correção
        return valorOriginal.multiply(fatorCorrecao).setScale(2, ROUNDING);
    }

    /**
     * Calcula o fator de correção entre duas datas.
     */
    public BigDecimal calcularFatorCorrecao(LocalDate dataInicial,
                                            LocalDate dataFinal,
                                            Long tabelaIndiceId) {

        LocalDate competenciaInicial = dataInicial.withDayOfMonth(1).minusMonths(1);
        ValorIndice indiceInicial = valorIndiceRepository
            .findByTabelaIndiceIdAndCompetenciaLessThanEqual(tabelaIndiceId, competenciaInicial)
            .orElseThrow(() -> new BusinessException(
                "Índice não encontrado para a competência: " + competenciaInicial
            ));

        LocalDate competenciaFinal = dataFinal.withDayOfMonth(1);
        ValorIndice indiceFinal = valorIndiceRepository
            .findByTabelaIndiceIdAndCompetenciaLessThanEqual(tabelaIndiceId, competenciaFinal)
            .orElseThrow(() -> new BusinessException(
                "Índice não encontrado para a competência: " + competenciaFinal
            ));

        return indiceFinal.getValor().divide(indiceInicial.getValor(), PRECISION, ROUNDING);
    }

    /**
     * Obtém a lista de índices entre duas datas para detalhamento.
     */
    public List<ValorIndice> obterIndicesNoPeriodo(Long tabelaIndiceId,
                                                   LocalDate dataInicial,
                                                   LocalDate dataFinal) {
        LocalDate competenciaInicial = dataInicial.withDayOfMonth(1);
        LocalDate competenciaFinal = dataFinal.withDayOfMonth(1);

        return valorIndiceRepository.findByTabelaIndiceIdAndPeriodo(
            tabelaIndiceId, competenciaInicial, competenciaFinal
        );
    }
}
