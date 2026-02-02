package com.calculosjuridicos.service;

import com.calculosjuridicos.entity.TabelaIndice;
import com.calculosjuridicos.entity.ValorIndice;
import com.calculosjuridicos.exception.BusinessException;
import com.calculosjuridicos.exception.ResourceNotFoundException;
import com.calculosjuridicos.repository.TabelaIndiceRepository;
import com.calculosjuridicos.repository.ValorIndiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndiceService {

    private final TabelaIndiceRepository tabelaIndiceRepository;
    private final ValorIndiceRepository valorIndiceRepository;

    private static final DateTimeFormatter[] DATE_FORMATS = {
        DateTimeFormatter.ofPattern("yyyy-MM"),
        DateTimeFormatter.ofPattern("MM/yyyy"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd")
    };

    @Transactional(readOnly = true)
    public List<TabelaIndice> listarTabelas() {
        return tabelaIndiceRepository.findAll();
    }

    @Transactional(readOnly = true)
    public TabelaIndice buscarTabelaPorId(Long id) {
        return tabelaIndiceRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("TabelaIndice", "id", id));
    }

    @Transactional(readOnly = true)
    public List<ValorIndice> listarValores(Long tabelaIndiceId, YearMonth de, YearMonth ate) {
        LocalDate dataInicial = de != null ? de.atDay(1) : LocalDate.of(1990, 1, 1);
        LocalDate dataFinal = ate != null ? ate.atEndOfMonth() : LocalDate.now();

        return valorIndiceRepository.findByTabelaIndiceIdAndPeriodo(tabelaIndiceId, dataInicial, dataFinal);
    }

    @Transactional
    public ImportResult importarCSV(Long tabelaIndiceId, MultipartFile file) {
        TabelaIndice tabela = tabelaIndiceRepository.findById(tabelaIndiceId)
            .orElseThrow(() -> new ResourceNotFoundException("TabelaIndice", "id", tabelaIndiceId));

        int importados = 0;
        int atualizados = 0;
        List<String> erros = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String linha;
            int numeroLinha = 0;
            boolean primeiraLinha = true;

            while ((linha = reader.readLine()) != null) {
                numeroLinha++;

                // Pular linha de cabeçalho
                if (primeiraLinha) {
                    primeiraLinha = false;
                    if (linha.toLowerCase().contains("competencia") || linha.toLowerCase().contains("data")) {
                        continue;
                    }
                }

                // Pular linhas vazias
                if (linha.trim().isEmpty()) {
                    continue;
                }

                try {
                    String[] partes = linha.split("[,;\\t]");
                    if (partes.length < 2) {
                        erros.add("Linha " + numeroLinha + ": formato inválido");
                        continue;
                    }

                    LocalDate competencia = parseCompetencia(partes[0].trim());
                    BigDecimal valor = parseValor(partes[1].trim());

                    Optional<ValorIndice> existente = valorIndiceRepository
                        .findByTabelaIndiceIdAndCompetencia(tabelaIndiceId, competencia);

                    if (existente.isPresent()) {
                        ValorIndice vi = existente.get();
                        vi.setValor(valor);
                        vi.setFonte(ValorIndice.FonteValor.CSV_IMPORT);
                        valorIndiceRepository.save(vi);
                        atualizados++;
                    } else {
                        ValorIndice novoValor = ValorIndice.builder()
                            .tabelaIndice(tabela)
                            .competencia(competencia)
                            .valor(valor)
                            .fonte(ValorIndice.FonteValor.CSV_IMPORT)
                            .build();
                        valorIndiceRepository.save(novoValor);
                        importados++;
                    }

                } catch (Exception e) {
                    erros.add("Linha " + numeroLinha + ": " + e.getMessage());
                }
            }

        } catch (Exception e) {
            throw new BusinessException("Erro ao processar arquivo CSV: " + e.getMessage());
        }

        log.info("Importação concluída para tabela {}: {} novos, {} atualizados, {} erros",
            tabela.getNome(), importados, atualizados, erros.size());

        return new ImportResult(importados, atualizados, erros);
    }

    private LocalDate parseCompetencia(String valor) {
        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                if (formatter.toString().contains("yyyy-MM-dd")) {
                    return LocalDate.parse(valor, formatter);
                } else {
                    YearMonth ym = YearMonth.parse(valor, formatter);
                    return ym.atDay(1);
                }
            } catch (DateTimeParseException ignored) {
                // Tentar próximo formato
            }
        }
        throw new BusinessException("Formato de data inválido: " + valor);
    }

    private BigDecimal parseValor(String valor) {
        // Remover pontos de milhar e trocar vírgula por ponto
        String valorLimpo = valor
            .replace(".", "")
            .replace(",", ".")
            .trim();

        try {
            return new BigDecimal(valorLimpo);
        } catch (NumberFormatException e) {
            throw new BusinessException("Valor numérico inválido: " + valor);
        }
    }

    public record ImportResult(int registrosImportados, int registrosAtualizados, List<String> erros) {}
}
