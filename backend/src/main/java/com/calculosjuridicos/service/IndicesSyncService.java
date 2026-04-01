package com.calculosjuridicos.service;

import com.calculosjuridicos.entity.TabelaIndice;
import com.calculosjuridicos.entity.ValorIndice;
import com.calculosjuridicos.exception.BusinessException;
import com.calculosjuridicos.repository.TabelaIndiceRepository;
import com.calculosjuridicos.repository.ValorIndiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndicesSyncService {

    private final TabelaIndiceRepository tabelaIndiceRepository;
    private final ValorIndiceRepository valorIndiceRepository;
    private final WebClient.Builder webClientBuilder;

    @Value("${indices.bcb.base-url:https://api.bcb.gov.br}")
    private String bcbBaseUrl;

    @Value("${indices.bcb.timeout:30000}")
    private int timeout;

    @Value("${indices.sync.enabled:true}")
    private boolean syncEnabled;

    private static final DateTimeFormatter BCB_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final BigDecimal CEM = new BigDecimal("100");
    private static final int SCALE = 8;
    private static final LocalDate DATA_HISTORICO_INICIO = LocalDate.of(2000, 1, 1);

    private static final Map<String, String> SERIE_BCB_MAP = Map.of(
        "IPCA_E", "10764",
        "INPC", "188",
        "IGPM", "189",
        "TR", "226",
        "SELIC", "4390"
    );

    /**
     * Sincronização inteligente de um índice.
     * - Se não há dados ou faltam dados históricos: faz sync completo desde 2000 (apaga e recria cadeia contínua).
     * - Se dados já estão completos desde 2000: apenas busca meses novos/atualizados.
     */
    @Transactional
    public SyncResult sincronizarIncremental(Long tabelaIndiceId, LocalDate dataInicial, LocalDate dataFinal) {
        TabelaIndice tabela = tabelaIndiceRepository.findById(tabelaIndiceId)
            .orElseThrow(() -> new BusinessException("Tabela de índice não encontrada: " + tabelaIndiceId));

        Optional<LocalDate> primeiraCompetencia = valorIndiceRepository.findMinCompetenciaByTabelaIndiceId(tabelaIndiceId);
        Optional<LocalDate> ultimaCompetencia = valorIndiceRepository.findMaxCompetenciaByTabelaIndiceId(tabelaIndiceId);

        // Se não há dados OU o primeiro registro é posterior ao dataInicial esperado,
        // reconstruir toda a cadeia do zero para garantir continuidade
        boolean precisaReconstruir = primeiraCompetencia.isEmpty()
            || primeiraCompetencia.get().isAfter(dataInicial.withDayOfMonth(1).plusMonths(1));

        if (precisaReconstruir) {
            log.info("Índice {} precisa de sync completo desde {}. Reconstruindo cadeia...",
                tabela.getNome(), dataInicial);
            // Apagar todos os registros e reconstruir
            valorIndiceRepository.deleteByTabelaIndiceIdAndCompetenciaBetween(
                tabelaIndiceId, LocalDate.of(1900, 1, 1), LocalDate.of(2100, 12, 31));
            return sincronizarCompleto(tabelaIndiceId, tabela, dataInicial, dataFinal);
        }

        // Dados já existem desde ~2000: apenas atualizar os meses mais recentes
        LocalDate ultimaData = ultimaCompetencia.get();
        LocalDate buscarDesde = ultimaData.minusMonths(1); // sobrepor 1 mês para garantir
        log.info("Índice {} atualizado até {}. Buscando novos dados desde {}...",
            tabela.getNome(), ultimaData, buscarDesde);
        return sincronizarPeriodo(tabelaIndiceId, tabela, buscarDesde, dataFinal);
    }

    /**
     * Sync completo: busca todos os dados desde dataInicial em chunks de 5 anos,
     * calcula cadeia acumulada contínua e persiste.
     */
    private SyncResult sincronizarCompleto(Long tabelaIndiceId, TabelaIndice tabela,
                                            LocalDate dataInicial, LocalDate dataFinal) {
        String serieId = SERIE_BCB_MAP.get(tabela.getNome());

        // Buscar TODOS os dados do BCB em chunks de 5 anos
        List<BcbDataPoint> todosOsDados = new ArrayList<>();
        LocalDate chunkInicio = dataInicial;
        List<String> erros = new ArrayList<>();

        while (chunkInicio.isBefore(dataFinal)) {
            LocalDate chunkFim = chunkInicio.plusYears(5);
            if (chunkFim.isAfter(dataFinal)) chunkFim = dataFinal;
            try {
                List<BcbDataPoint> chunk = fetchBcbData(serieId, chunkInicio, chunkFim);
                todosOsDados.addAll(chunk);
            } catch (Exception ex) {
                log.warn("Falha no chunk {}-{} para {}: {}", chunkInicio, chunkFim, tabela.getNome(), ex.getMessage());
                erros.add("Erro no período " + chunkInicio + " a " + chunkFim);
            }
            chunkInicio = chunkFim.plusDays(1);
        }

        // Deduplicar por competência (manter último valor para cada mês)
        LinkedHashMap<LocalDate, BigDecimal> porCompetencia = new LinkedHashMap<>();
        for (BcbDataPoint ponto : todosOsDados) {
            porCompetencia.put(ponto.competencia(), ponto.valor());
        }

        // Calcular cadeia acumulada contínua
        BigDecimal indiceAcumulado = new BigDecimal("1000.00000000");
        int importados = 0;

        for (Map.Entry<LocalDate, BigDecimal> entry : porCompetencia.entrySet()) {
            LocalDate competencia = entry.getKey();
            BigDecimal variacao = entry.getValue();

            BigDecimal fator = BigDecimal.ONE.add(variacao.divide(CEM, SCALE, RoundingMode.HALF_UP));
            indiceAcumulado = indiceAcumulado.multiply(fator).setScale(SCALE, RoundingMode.HALF_UP);

            ValorIndice novoValor = ValorIndice.builder()
                .tabelaIndice(tabela)
                .competencia(competencia)
                .valor(indiceAcumulado)
                .fonte(ValorIndice.FonteValor.API_BCB)
                .build();
            valorIndiceRepository.save(novoValor);
            importados++;
        }

        log.info("Sync completo {} concluído: {} registros criados em cadeia contínua",
            tabela.getNome(), importados);
        return new SyncResult(importados, 0, erros);
    }

    /**
     * Sync parcial: busca dados de um período e atualiza/cria registros.
     * Usa o último índice acumulado existente como base.
     */
    private SyncResult sincronizarPeriodo(Long tabelaIndiceId, TabelaIndice tabela,
                                           LocalDate dataInicial, LocalDate dataFinal) {
        String serieId = SERIE_BCB_MAP.get(tabela.getNome());
        List<BcbDataPoint> dados = fetchBcbData(serieId, dataInicial, dataFinal);

        if (dados.isEmpty()) {
            return new SyncResult(0, 0, List.of());
        }

        // Deduplicar por competência
        LinkedHashMap<LocalDate, BigDecimal> porCompetencia = new LinkedHashMap<>();
        for (BcbDataPoint ponto : dados) {
            porCompetencia.put(ponto.competencia(), ponto.valor());
        }

        BigDecimal indiceAcumulado = obterUltimoIndiceAcumulado(tabelaIndiceId, dataInicial);

        int importados = 0, atualizados = 0;
        List<String> erros = new ArrayList<>();

        for (Map.Entry<LocalDate, BigDecimal> entry : porCompetencia.entrySet()) {
            try {
                LocalDate competencia = entry.getKey();
                BigDecimal variacao = entry.getValue();

                BigDecimal fator = BigDecimal.ONE.add(variacao.divide(CEM, SCALE, RoundingMode.HALF_UP));
                indiceAcumulado = indiceAcumulado.multiply(fator).setScale(SCALE, RoundingMode.HALF_UP);

                Optional<ValorIndice> existente = valorIndiceRepository
                    .findByTabelaIndiceIdAndCompetencia(tabelaIndiceId, competencia);

                if (existente.isPresent()) {
                    ValorIndice vi = existente.get();
                    vi.setValor(indiceAcumulado);
                    vi.setFonte(ValorIndice.FonteValor.API_BCB);
                    valorIndiceRepository.save(vi);
                    atualizados++;
                } else {
                    ValorIndice novoValor = ValorIndice.builder()
                        .tabelaIndice(tabela)
                        .competencia(competencia)
                        .valor(indiceAcumulado)
                        .fonte(ValorIndice.FonteValor.API_BCB)
                        .build();
                    valorIndiceRepository.save(novoValor);
                    importados++;
                }
            } catch (Exception e) {
                erros.add("Erro ao processar " + entry.getKey() + ": " + e.getMessage());
            }
        }

        return new SyncResult(importados, atualizados, erros);
    }

    /**
     * Sincronização incremental de todos os índices.
     */
    @Transactional
    public Map<String, SyncResult> sincronizarTodosIncremental(LocalDate dataInicial, LocalDate dataFinal) {
        Map<String, SyncResult> resultados = new LinkedHashMap<>();
        List<TabelaIndice> tabelas = tabelaIndiceRepository.findAll();

        for (TabelaIndice tabela : tabelas) {
            if (SERIE_BCB_MAP.containsKey(tabela.getNome())) {
                try {
                    SyncResult result = sincronizarIncremental(tabela.getId(), dataInicial, dataFinal);
                    resultados.put(tabela.getNome(), result);
                } catch (Exception e) {
                    log.error("Erro ao sincronizar índice {}: {}", tabela.getNome(), e.getMessage());
                    resultados.put(tabela.getNome(),
                        new SyncResult(0, 0, List.of("Erro: " + e.getMessage())));
                }
            }
        }

        return resultados;
    }

    /**
     * Sincroniza dados históricos completos desde 2000 para todos os índices.
     */
    @Transactional
    public Map<String, SyncResult> sincronizarHistorico() {
        log.info("Iniciando sincronização histórica completa desde {}", DATA_HISTORICO_INICIO);
        return sincronizarTodosIncremental(DATA_HISTORICO_INICIO, LocalDate.now());
    }

    /**
     * Verifica e sincroniza dados históricos (método legado, mantido para compatibilidade).
     */
    public void verificarDadosHistoricos() {
        if (!syncEnabled) return;
        sincronizarTodosIncremental(DATA_HISTORICO_INICIO, LocalDate.now());
    }

    /**
     * Sincroniza todos os índices para um período (método legado).
     */
    @Transactional
    public Map<String, SyncResult> sincronizarTodos(LocalDate dataInicial, LocalDate dataFinal) {
        return sincronizarTodosIncremental(dataInicial, dataFinal);
    }

    /**
     * Sincroniza os índices de uma tabela específica (método legado usado pelo controller individual).
     */
    @Transactional
    public SyncResult sincronizar(Long tabelaIndiceId, LocalDate dataInicial, LocalDate dataFinal) {
        TabelaIndice tabela = tabelaIndiceRepository.findById(tabelaIndiceId)
            .orElseThrow(() -> new BusinessException("Tabela de índice não encontrada: " + tabelaIndiceId));
        return sincronizarPeriodo(tabelaIndiceId, tabela, dataInicial, dataFinal);
    }

    /**
     * Sincronização agendada - executa no dia 15 de cada mês às 6h.
     */
    @Scheduled(cron = "${indices.sync.cron:0 0 6 15 * ?}")
    public void sincronizacaoAgendada() {
        if (!syncEnabled) return;
        log.info("Executando sincronização agendada de índices");
        LocalDate dataFinal = LocalDate.now();
        sincronizarTodosIncremental(DATA_HISTORICO_INICIO, dataFinal);
    }

    private List<BcbDataPoint> fetchBcbData(String serieId, LocalDate dataInicial, LocalDate dataFinal) {
        String url = String.format(
            "%s/dados/serie/bcdata.sgs.%s/dados?formato=json&dataInicial=%s&dataFinal=%s",
            bcbBaseUrl, serieId,
            dataInicial.format(BCB_DATE_FORMAT),
            dataFinal.format(BCB_DATE_FORMAT)
        );

        log.debug("Fetching BCB data from: {}", url);

        try {
            WebClient client = webClientBuilder.build();
            List<Map<String, String>> response = client.get()
                .uri(url)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, String>>>() {})
                .block(Duration.ofMillis(timeout));

            if (response == null || response.isEmpty()) {
                return List.of();
            }

            return response.stream()
                .map(item -> {
                    LocalDate data = LocalDate.parse(item.get("data"), BCB_DATE_FORMAT);
                    LocalDate competencia = data.withDayOfMonth(1);
                    BigDecimal valor = new BigDecimal(item.get("valor"));
                    return new BcbDataPoint(competencia, valor);
                })
                .toList();

        } catch (Exception e) {
            log.error("Erro ao buscar dados do BCB (série {}): {}", serieId, e.getMessage());
            throw new BusinessException("Erro ao acessar API do Banco Central: " + e.getMessage());
        }
    }

    private BigDecimal obterUltimoIndiceAcumulado(Long tabelaIndiceId, LocalDate dataInicial) {
        return valorIndiceRepository
            .findFirstByTabelaIndiceIdAndCompetenciaLessThanEqualOrderByCompetenciaDesc(
                tabelaIndiceId, dataInicial.minusDays(1))
            .map(ValorIndice::getValor)
            .orElse(new BigDecimal("1000.00000000"));
    }

    public record BcbDataPoint(LocalDate competencia, BigDecimal valor) {}

    public record SyncResult(int registrosImportados, int registrosAtualizados, List<String> erros) {}
}
