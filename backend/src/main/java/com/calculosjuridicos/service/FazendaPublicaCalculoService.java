package com.calculosjuridicos.service;

import com.calculosjuridicos.dto.request.CalculoRequest;
import com.calculosjuridicos.dto.response.ResultadoCalculoResponse;
import com.calculosjuridicos.dto.response.ResultadoCalculoResponse.DetalhamentoMensalResponse;
import com.calculosjuridicos.entity.TabelaIndice;
import com.calculosjuridicos.entity.ValorIndice;
import com.calculosjuridicos.exception.BusinessException;
import com.calculosjuridicos.repository.TabelaIndiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Serviço especializado para cálculos de condenação da Fazenda Pública Federal.
 *
 * Aplica automaticamente os índices de correção monetária e juros moratórios
 * conforme a legislação federal vigente em cada período:
 *
 * Correção Monetária:
 * - 01/1984 a 02/1991: INPC (substituto consolidado para índices extintos ORTN/OTN/IPC)
 * - 03/1991 a 12/1991: INPC (Lei 8.177/91)
 * - 01/1992 a 12/2000: IPCA-E (transição pós-Plano Real, Manual JF)
 * - 01/2001 a 06/2009: IPCA-E (Manual JF, Res. CJF 242/2001)
 * - 07/2009 a 08/12/2021: IPCA-E (Lei 11.960/2009, Tema 810 STF)
 * - 09/12/2021 a 09/2025: SELIC unificada (EC 113/2021, art. 3º)
 * - 10/2025 em diante: IPCA + juros 2% a.a., limitado à SELIC (EC 136/2025)
 *
 * Juros Moratórios (simplificado):
 * - 01/1984 a 06/2009: 1% a.m. simples (Art. 406 CC/2002, Art. 161 §1º CTN)
 * - 07/2009 a 08/12/2021: 0,5% a.m. simples (Lei 11.960/2009 - poupança)
 * - 09/12/2021 a 09/2025: SELIC unificada (engloba correção + juros)
 * - 10/2025+: 2% a.a. simples, limitado à SELIC (EC 136/2025)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FazendaPublicaCalculoService {

    private final CorrecaoMonetariaService correcaoService;
    private final TabelaIndiceRepository tabelaIndiceRepository;

    private static final BigDecimal CEM = new BigDecimal("100");
    private static final int PRECISION = 10;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final DateTimeFormatter COMPETENCIA_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    // Marcos legislativos
    private static final LocalDate MARCO_INPC_IPCAE = LocalDate.of(1992, 1, 1);   // Transição INPC → IPCA-E
    private static final LocalDate MARCO_LEI_11960 = LocalDate.of(2009, 7, 1);     // Lei 11.960/2009
    private static final LocalDate MARCO_EC_113 = LocalDate.of(2021, 12, 9);       // EC 113/2021
    private static final LocalDate MARCO_EC_136 = LocalDate.of(2025, 10, 1);       // EC 136/2025

    // Taxas de juros
    private static final BigDecimal TAXA_1_PERCENT_MENSAL = new BigDecimal("1.0");
    private static final BigDecimal TAXA_POUPANCA_MENSAL = new BigDecimal("0.5");
    private static final BigDecimal TAXA_2_AA = new BigDecimal("2.0");

    /**
     * Executa o cálculo completo de condenação da Fazenda Pública.
     */
    public ResultadoCalculoResponse calcular(CalculoRequest request) {
        LocalDate dataInicial = request.getDataInicial();
        LocalDate dataFinal = request.getDataFinal();
        BigDecimal valorOriginal = request.getValorPrincipal();

        // Buscar IDs dos índices no banco
        Long inpcId = buscarIdIndice(TabelaIndice.INPC);
        Long ipcaeId = buscarIdIndice(TabelaIndice.IPCA_E);
        Long selicId = buscarIdIndice(TabelaIndice.SELIC);

        // Gerar detalhamento mensal com índices variáveis
        List<DetalhamentoMensalResponse> detalhamento = gerarDetalhamentoMensal(
                valorOriginal, dataInicial, dataFinal,
                inpcId, ipcaeId, selicId
        );

        // Extrair totais do último mês do detalhamento
        BigDecimal totalCorrigido = valorOriginal;
        BigDecimal totalJuros = BigDecimal.ZERO;

        if (!detalhamento.isEmpty()) {
            DetalhamentoMensalResponse ultimo = detalhamento.get(detalhamento.size() - 1);
            totalCorrigido = ultimo.getValorCorrigidoParcial();
            totalJuros = ultimo.getJurosParcial();
        }

        // Subtotal (corrigido + juros)
        BigDecimal subtotal = totalCorrigido.add(totalJuros);

        // Multa
        BigDecimal valorMulta = BigDecimal.ZERO;
        if (request.getMultaPercentual() != null && request.getMultaPercentual().compareTo(BigDecimal.ZERO) > 0) {
            valorMulta = subtotal.multiply(request.getMultaPercentual())
                    .divide(CEM, 2, ROUNDING);
        }

        // Honorários
        BigDecimal baseHonorarios = subtotal.add(valorMulta);
        BigDecimal valorHonorarios = BigDecimal.ZERO;
        if (request.getHonorariosPercentual() != null && request.getHonorariosPercentual().compareTo(BigDecimal.ZERO) > 0) {
            valorHonorarios = baseHonorarios.multiply(request.getHonorariosPercentual())
                    .divide(CEM, 2, ROUNDING);
        }

        BigDecimal valorTotal = subtotal.add(valorMulta).add(valorHonorarios);

        // Fator de correção geral
        BigDecimal fatorCorrecao = BigDecimal.ONE;
        if (valorOriginal.compareTo(BigDecimal.ZERO) > 0) {
            fatorCorrecao = totalCorrigido.divide(valorOriginal, 6, ROUNDING);
        }

        // Variação total do período
        BigDecimal variacaoTotalPeriodo = null;
        if (fatorCorrecao.compareTo(BigDecimal.ONE) > 0) {
            variacaoTotalPeriodo = fatorCorrecao.subtract(BigDecimal.ONE)
                    .multiply(CEM).setScale(4, ROUNDING);
        }

        return ResultadoCalculoResponse.builder()
                .valorOriginal(valorOriginal)
                .valorCorrigido(totalCorrigido)
                .valorJuros(totalJuros)
                .valorMulta(valorMulta)
                .valorHonorarios(valorHonorarios)
                .valorTotal(valorTotal)
                .fatorCorrecao(fatorCorrecao)
                .variacaoTotalPeriodo(variacaoTotalPeriodo)
                .dataCalculo(LocalDateTime.now())
                .parcelas(List.of())
                .detalhamento(detalhamento)
                .build();
    }

    /**
     * Gera o detalhamento mensal com índices que mudam conforme a legislação.
     * Cobre o período de 1984 ao presente.
     */
    private List<DetalhamentoMensalResponse> gerarDetalhamentoMensal(
            BigDecimal valorOriginal,
            LocalDate dataInicial,
            LocalDate dataFinal,
            Long inpcId,
            Long ipcaeId,
            Long selicId) {

        List<DetalhamentoMensalResponse> detalhamento = new ArrayList<>();

        // Iterar mês a mês
        LocalDate competencia = dataInicial.withDayOfMonth(1);
        LocalDate competenciaFinal = dataFinal.withDayOfMonth(1);

        BigDecimal valorCorrigidoAcumulado = valorOriginal;
        BigDecimal jurosAcumulados = BigDecimal.ZERO;
        BigDecimal indiceAnteriorInpc = null;
        BigDecimal indiceAnteriorIpcae = null;
        BigDecimal indiceAnteriorSelic = null;

        while (!competencia.isAfter(competenciaFinal)) {
            LocalDate dataRef = competencia;
            String nomeIndice;
            BigDecimal valorCorrigidoMes;
            BigDecimal jurosMes;
            BigDecimal variacaoPercentual = null;
            BigDecimal indiceValor = null;
            BigDecimal fatorAcumulado = BigDecimal.ONE;

            // Determinar qual regime aplicar neste mês
            if (!dataRef.isBefore(MARCO_EC_136.withDayOfMonth(1))) {
                // ═══ EC 136/2025: IPCA + juros 2% a.a. (limitado à SELIC) ═══
                nomeIndice = "IPCA";
                BigDecimal[] resultadoIpca = calcularMesIndice(valorCorrigidoAcumulado,
                        competencia, ipcaeId, indiceAnteriorIpcae);
                valorCorrigidoMes = resultadoIpca[0];
                variacaoPercentual = resultadoIpca[1];
                indiceValor = resultadoIpca[2];
                indiceAnteriorIpcae = indiceValor;

                // Juros: 2% a.a. simples sobre o principal
                long meses = ChronoUnit.MONTHS.between(dataInicial.withDayOfMonth(1), competencia) + 1;
                jurosMes = valorOriginal.multiply(TAXA_2_AA)
                        .divide(CEM, PRECISION, ROUNDING)
                        .multiply(new BigDecimal(meses))
                        .divide(new BigDecimal("12"), 2, ROUNDING);

                // Limitar: se IPCA + juros > SELIC, usar SELIC
                BigDecimal totalIpcaJuros = valorCorrigidoMes.add(jurosMes);
                BigDecimal totalSelic = calcularValorSelic(valorOriginal, dataInicial, competencia, selicId);
                if (totalSelic != null && totalIpcaJuros.compareTo(totalSelic) > 0) {
                    valorCorrigidoMes = totalSelic;
                    jurosMes = BigDecimal.ZERO;
                    nomeIndice = "SELIC (Teto)";
                }

            } else if (!dataRef.isBefore(MARCO_EC_113.withDayOfMonth(1))) {
                // ═══ EC 113/2021: SELIC unificada (correção + juros) ═══
                nomeIndice = "SELIC";
                BigDecimal totalSelic = calcularValorSelic(valorOriginal, dataInicial, competencia, selicId);
                if (totalSelic != null) {
                    valorCorrigidoMes = totalSelic;
                } else {
                    valorCorrigidoMes = valorCorrigidoAcumulado;
                }
                jurosMes = BigDecimal.ZERO; // SELIC já inclui juros

                // Buscar índice SELIC para variação
                try {
                    List<ValorIndice> selicIndices = correcaoService.obterIndicesNoPeriodo(selicId, competencia, competencia);
                    if (!selicIndices.isEmpty()) {
                        indiceValor = selicIndices.get(0).getValor();
                        if (indiceAnteriorSelic != null && indiceAnteriorSelic.compareTo(BigDecimal.ZERO) > 0) {
                            variacaoPercentual = indiceValor.subtract(indiceAnteriorSelic)
                                    .divide(indiceAnteriorSelic, 6, ROUNDING)
                                    .multiply(CEM).setScale(4, ROUNDING);
                        }
                        indiceAnteriorSelic = indiceValor;
                    }
                } catch (Exception e) {
                    log.warn("Erro ao buscar índice SELIC para {}: {}", competencia, e.getMessage());
                }

            } else if (!dataRef.isBefore(MARCO_INPC_IPCAE.withDayOfMonth(1))) {
                // ═══ 01/1992 a 08/12/2021: IPCA-E para correção + juros separados ═══
                nomeIndice = "IPCA-E";
                BigDecimal[] resultadoIpca = calcularMesIndice(valorCorrigidoAcumulado,
                        competencia, ipcaeId, indiceAnteriorIpcae);
                valorCorrigidoMes = resultadoIpca[0];
                variacaoPercentual = resultadoIpca[1];
                indiceValor = resultadoIpca[2];
                indiceAnteriorIpcae = indiceValor;

                // Calcular juros simplificados
                jurosMes = calcularJurosSimplificado(valorOriginal, valorCorrigidoMes,
                        dataInicial, competencia);

            } else {
                // ═══ Antes de 01/1992: INPC para correção + juros separados ═══
                nomeIndice = "INPC";
                BigDecimal[] resultadoInpc = calcularMesIndice(valorCorrigidoAcumulado,
                        competencia, inpcId, indiceAnteriorInpc);
                valorCorrigidoMes = resultadoInpc[0];
                variacaoPercentual = resultadoInpc[1];
                indiceValor = resultadoInpc[2];
                indiceAnteriorInpc = indiceValor;

                // Calcular juros simplificados
                jurosMes = calcularJurosSimplificado(valorOriginal, valorCorrigidoMes,
                        dataInicial, competencia);
            }

            // Fator acumulado
            if (valorOriginal.compareTo(BigDecimal.ZERO) > 0) {
                fatorAcumulado = valorCorrigidoMes.divide(valorOriginal, 6, ROUNDING);
            }

            valorCorrigidoAcumulado = valorCorrigidoMes;
            jurosAcumulados = jurosMes;

            detalhamento.add(DetalhamentoMensalResponse.builder()
                    .competencia(competencia.format(COMPETENCIA_FORMAT))
                    .nomeIndice(nomeIndice)
                    .indice(indiceValor)
                    .fatorAcumulado(fatorAcumulado)
                    .variacaoPercentual(variacaoPercentual)
                    .valorCorrigidoParcial(valorCorrigidoMes.setScale(2, ROUNDING))
                    .jurosParcial(jurosMes.setScale(2, ROUNDING))
                    .subtotalParcial(valorCorrigidoMes.add(jurosMes).setScale(2, ROUNDING))
                    .build());

            competencia = competencia.plusMonths(1);
        }

        return detalhamento;
    }

    /**
     * Calcula valor corrigido por um índice genérico (IPCA-E ou INPC) para um mês específico.
     * Retorna array: [valorCorrigido, variacaoPercentual, indiceValor]
     */
    private BigDecimal[] calcularMesIndice(BigDecimal valorAcumuladoAnterior,
                                            LocalDate competencia, Long indiceId,
                                            BigDecimal indiceAnterior) {
        BigDecimal valorCorrigido = valorAcumuladoAnterior;
        BigDecimal variacaoPercentual = null;
        BigDecimal indiceValor = null;

        try {
            List<ValorIndice> indices = correcaoService.obterIndicesNoPeriodo(indiceId, competencia, competencia);
            if (!indices.isEmpty()) {
                indiceValor = indices.get(0).getValor();

                if (indiceAnterior != null && indiceAnterior.compareTo(BigDecimal.ZERO) > 0) {
                    // Variação mensal
                    BigDecimal fatorMensal = indiceValor.divide(indiceAnterior, PRECISION, ROUNDING);
                    valorCorrigido = valorAcumuladoAnterior.multiply(fatorMensal).setScale(2, ROUNDING);
                    variacaoPercentual = fatorMensal.subtract(BigDecimal.ONE)
                            .multiply(CEM).setScale(4, ROUNDING);
                }
            }
        } catch (Exception e) {
            log.warn("Erro ao buscar índice para {}: {}", competencia, e.getMessage());
        }

        return new BigDecimal[]{valorCorrigido, variacaoPercentual, indiceValor};
    }

    /**
     * Calcula juros moratórios simplificados (sem diferenciação por natureza).
     *
     * - Antes de 07/2009: 1% a.m. simples (Art. 406 CC/2002)
     * - 07/2009 a 08/12/2021: 0,5% a.m. simples (Lei 11.960/2009 - poupança)
     * (EC 113+ e EC 136+ são tratados separadamente no fluxo principal)
     */
    private BigDecimal calcularJurosSimplificado(BigDecimal valorOriginal, BigDecimal valorCorrigido,
                                                   LocalDate dataInicial, LocalDate competencia) {
        long meses = ChronoUnit.MONTHS.between(dataInicial.withDayOfMonth(1), competencia) + 1;
        if (meses <= 0) return BigDecimal.ZERO;

        BigDecimal taxa;
        if (competencia.isBefore(MARCO_LEI_11960)) {
            // Antes de 07/2009: 1% a.m. simples
            taxa = TAXA_1_PERCENT_MENSAL;
        } else {
            // 07/2009 a 08/12/2021: 0,5% a.m. simples (poupança)
            taxa = TAXA_POUPANCA_MENSAL;
        }

        return valorCorrigido.multiply(taxa)
                .divide(CEM, PRECISION, ROUNDING)
                .multiply(new BigDecimal(meses))
                .setScale(2, ROUNDING);
    }

    /**
     * Calcula valor atualizado pela SELIC (taxa unificada).
     */
    private BigDecimal calcularValorSelic(BigDecimal valorOriginal, LocalDate dataInicial,
                                           LocalDate competencia, Long selicId) {
        try {
            return correcaoService.calcular(valorOriginal, dataInicial, competencia.plusMonths(1).minusDays(1), selicId);
        } catch (Exception e) {
            log.warn("Erro ao calcular SELIC para {}: {}", competencia, e.getMessage());
            return null;
        }
    }

    /**
     * Busca o ID de um índice pelo nome.
     */
    private Long buscarIdIndice(String nome) {
        TabelaIndice tabela = tabelaIndiceRepository.findByNome(nome)
                .orElseThrow(() -> new BusinessException(
                        "Índice '" + nome + "' não encontrado. Verifique se os índices estão cadastrados no sistema."));
        return tabela.getId();
    }
}
