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

    /**
     * Mapeamento dos nomes das tabelas para as séries BCB SGS.
     * Todas as séries retornam variação mensal (%).
     */
    private static final Map<String, String> SERIE_BCB_MAP = Map.of(
        "IPCA_E", "10764",
        "INPC", "188",
        "IGPM", "189",
        "TR", "226",
        "SELIC", "4390"
    );

    /**
     * Sincroniza os índices de uma tabela específica com o BCB SGS.
     */
    @Transactional
    public SyncResult sincronizar(Long tabelaIndiceId, LocalDate dataInicial, LocalDate dataFinal) {
        TabelaIndice tabela = tabelaIndiceRepository.findById(tabelaIndiceId)
            .orElseThrow(() -> new BusinessException("Tabela de índice não encontrada: " + tabelaIndiceId));

        String serieId = SERIE_BCB_MAP.get(tabela.getNome());
        if (serieId == null) {
            throw new BusinessException("Série BCB não configurada para o índice: " + tabela.getNome());
        }

        log.info("Iniciando sincronização do índice {} (série BCB {}), período: {} a {}",
            tabela.getNome(), serieId, dataInicial, dataFinal);

        List<BcbDataPoint> dados = fetchBcbData(serieId, dataInicial, dataFinal);

        if (dados.isEmpty()) {
            log.warn("Nenhum dado retornado do BCB para série {}", serieId);
            return new SyncResult(0, 0, List.of("Nenhum dado retornado pela API do BCB"));
        }

        // Buscar o último índice acumulado existente antes do período
        BigDecimal indiceAcumulado = obterUltimoIndiceAcumulado(tabelaIndiceId, dataInicial);

        int importados = 0;
        int atualizados = 0;
        List<String> erros = new ArrayList<>();

        for (BcbDataPoint ponto : dados) {
            try {
                LocalDate competencia = ponto.competencia();
                BigDecimal variacao = ponto.valor();

                // Calcular índice acumulado: indice_anterior * (1 + variacao/100)
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
                String msg = "Erro ao processar ponto: " + ponto + " - " + e.getMessage();
                log.warn(msg);
                erros.add(msg);
            }
        }

        log.info("Sincronização concluída para {}: {} novos, {} atualizados, {} erros",
            tabela.getNome(), importados, atualizados, erros.size());

        return new SyncResult(importados, atualizados, erros);
    }

    /**
     * Sincroniza todos os índices configurados.
     */
    @Transactional
    public Map<String, SyncResult> sincronizarTodos(LocalDate dataInicial, LocalDate dataFinal) {
        Map<String, SyncResult> resultados = new LinkedHashMap<>();
        List<TabelaIndice> tabelas = tabelaIndiceRepository.findAll();

        for (TabelaIndice tabela : tabelas) {
            if (SERIE_BCB_MAP.containsKey(tabela.getNome())) {
                try {
                    SyncResult result = sincronizar(tabela.getId(), dataInicial, dataFinal);
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
     * Sincronização agendada - executa no dia 15 de cada mês às 6h.
     */
    @Scheduled(cron = "${indices.sync.cron:0 0 6 15 * ?}")
    public void sincronizacaoAgendada() {
        if (!syncEnabled) {
            log.debug("Sincronização agendada desabilitada");
            return;
        }

        log.info("Executando sincronização agendada de índices");
        LocalDate dataFinal = LocalDate.now();
        LocalDate dataInicial = dataFinal.minusMonths(3);
        sincronizarTodos(dataInicial, dataFinal);
    }

    /**
     * Busca dados da API BCB SGS.
     * URL: https://api.bcb.gov.br/dados/serie/bcdata.sgs.{serie}/dados?formato=json&dataInicial=dd/MM/yyyy&dataFinal=dd/MM/yyyy
     */
    private List<BcbDataPoint> fetchBcbData(String serieId, LocalDate dataInicial, LocalDate dataFinal) {
        String url = String.format(
            "%s/dados/serie/bcdata.sgs.%s/dados?formato=json&dataInicial=%s&dataFinal=%s",
            bcbBaseUrl,
            serieId,
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
                    // Normalizar para primeiro dia do mês (competência)
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

    /**
     * Obtém o último índice acumulado antes da data informada.
     * Se não existir, inicia com base 1000 (padrão para índices acumulados).
     */
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
