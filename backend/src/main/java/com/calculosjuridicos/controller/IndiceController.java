package com.calculosjuridicos.controller;

import com.calculosjuridicos.entity.TabelaIndice;
import com.calculosjuridicos.entity.ValorIndice;
import com.calculosjuridicos.service.IndiceService;
import com.calculosjuridicos.service.IndicesSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/indices")
@RequiredArgsConstructor
@Tag(name = "Índices Monetários", description = "Endpoints para gestão de índices de correção monetária")
public class IndiceController {

    private final IndiceService indiceService;
    private final IndicesSyncService syncService;

    @GetMapping
    @Operation(summary = "Listar tabelas de índices disponíveis")
    public ResponseEntity<List<TabelaIndiceResponse>> listarTabelas() {
        List<TabelaIndice> tabelas = indiceService.listarTabelas();
        List<TabelaIndiceResponse> response = tabelas.stream()
            .map(this::toTabelaResponse)
            .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar tabela de índices por ID")
    public ResponseEntity<TabelaIndiceResponse> buscarTabela(@PathVariable Long id) {
        TabelaIndice tabela = indiceService.buscarTabelaPorId(id);
        return ResponseEntity.ok(toTabelaResponse(tabela));
    }

    @GetMapping("/{id}/valores")
    @Operation(summary = "Listar valores de um índice por período")
    public ResponseEntity<List<ValorIndiceResponse>> listarValores(
            @PathVariable Long id,
            @RequestParam(required = false) YearMonth de,
            @RequestParam(required = false) YearMonth ate) {
        List<ValorIndice> valores = indiceService.listarValores(id, de, ate);
        List<ValorIndiceResponse> response = valores.stream()
            .map(this::toValorResponse)
            .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/valores/import")
    @Operation(summary = "Importar valores de índice via CSV")
    public ResponseEntity<ImportResponse> importarCSV(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        IndiceService.ImportResult result = indiceService.importarCSV(id, file);
        return ResponseEntity.ok(new ImportResponse(
            result.registrosImportados(),
            result.registrosAtualizados(),
            result.erros()
        ));
    }

    @PostMapping("/{id}/sync")
    @Operation(summary = "Sincronizar índice com a API do Banco Central (BCB SGS)")
    public ResponseEntity<SyncResponse> sincronizar(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal) {

        if (dataFinal == null) {
            dataFinal = LocalDate.now();
        }
        if (dataInicial == null) {
            dataInicial = dataFinal.minusYears(5);
        }

        IndicesSyncService.SyncResult result = syncService.sincronizar(id, dataInicial, dataFinal);
        return ResponseEntity.ok(new SyncResponse(
            result.registrosImportados(),
            result.registrosAtualizados(),
            result.erros()
        ));
    }

    @PostMapping("/sync/todos")
    @Operation(summary = "Sincronizar todos os índices com o BCB SGS")
    public ResponseEntity<Map<String, SyncResponse>> sincronizarTodos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal) {

        if (dataFinal == null) {
            dataFinal = LocalDate.now();
        }
        if (dataInicial == null) {
            dataInicial = dataFinal.minusYears(5);
        }

        Map<String, IndicesSyncService.SyncResult> resultados = syncService.sincronizarTodos(dataInicial, dataFinal);
        Map<String, SyncResponse> response = new java.util.LinkedHashMap<>();
        resultados.forEach((nome, result) ->
            response.put(nome, new SyncResponse(
                result.registrosImportados(),
                result.registrosAtualizados(),
                result.erros()
            ))
        );
        return ResponseEntity.ok(response);
    }

    private TabelaIndiceResponse toTabelaResponse(TabelaIndice tabela) {
        return TabelaIndiceResponse.builder()
            .id(tabela.getId())
            .nome(tabela.getNome())
            .descricao(tabela.getDescricao())
            .codigoOficial(tabela.getCodigoOficial())
            .fonteApi(tabela.getFonteApi() != null ? tabela.getFonteApi().name() : null)
            .build();
    }

    private ValorIndiceResponse toValorResponse(ValorIndice valor) {
        return ValorIndiceResponse.builder()
            .id(valor.getId())
            .competencia(valor.getCompetencia())
            .valor(valor.getValor())
            .fonte(valor.getFonte() != null ? valor.getFonte().name() : null)
            .build();
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TabelaIndiceResponse {
        private Long id;
        private String nome;
        private String descricao;
        private String codigoOficial;
        private String fonteApi;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ValorIndiceResponse {
        private Long id;
        private LocalDate competencia;
        private BigDecimal valor;
        private String fonte;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ImportResponse {
        private int registrosImportados;
        private int registrosAtualizados;
        private List<String> erros;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SyncResponse {
        private int registrosImportados;
        private int registrosAtualizados;
        private List<String> erros;
    }
}
