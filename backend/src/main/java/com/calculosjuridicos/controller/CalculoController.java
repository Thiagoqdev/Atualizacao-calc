package com.calculosjuridicos.controller;

import com.calculosjuridicos.dto.request.CalculoRequest;
import com.calculosjuridicos.dto.response.ResultadoCalculoResponse;
import com.calculosjuridicos.entity.Calculo;
import com.calculosjuridicos.entity.ResultadoCalculo;
import com.calculosjuridicos.entity.Usuario;
import com.calculosjuridicos.service.CalculoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Cálculos", description = "Endpoints para gestão e execução de cálculos")
public class CalculoController {

    private final CalculoService calculoService;

    @PostMapping("/calculos/preview")
    @Operation(summary = "Preview de cálculo sem persistir")
    public ResponseEntity<ResultadoCalculoResponse> preview(@Valid @RequestBody CalculoRequest request) {
        ResultadoCalculoResponse response = calculoService.preview(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/processos/{processoId}/calculos")
    @Operation(summary = "Criar novo cálculo em um processo")
    public ResponseEntity<CalculoResponse> criar(
            @PathVariable Long processoId,
            @Valid @RequestBody CalculoRequest request,
            @AuthenticationPrincipal Usuario usuario) {
        Calculo calculo = calculoService.criar(processoId, request, usuario.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(calculo));
    }

    @GetMapping("/processos/{processoId}/calculos")
    @Operation(summary = "Listar cálculos de um processo")
    public ResponseEntity<Page<CalculoResponse>> listarPorProcesso(
            @PathVariable Long processoId,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<Calculo> calculos = calculoService.listarPorProcesso(processoId, pageable);
        return ResponseEntity.ok(calculos.map(this::toResponse));
    }

    @GetMapping("/calculos")
    @Operation(summary = "Listar cálculos do usuário")
    public ResponseEntity<Page<CalculoResponse>> listarMeusCalculos(
            @AuthenticationPrincipal Usuario usuario,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<Calculo> calculos = calculoService.listarPorUsuario(usuario.getId(), pageable);
        return ResponseEntity.ok(calculos.map(this::toResponse));
    }

    @GetMapping("/calculos/{id}")
    @Operation(summary = "Buscar cálculo por ID")
    public ResponseEntity<CalculoResponse> buscarPorId(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {
        Calculo calculo = calculoService.buscarPorId(id, usuario.getId());
        return ResponseEntity.ok(toResponse(calculo));
    }

    @PostMapping("/calculos/{id}/executar")
    @Operation(summary = "Executar cálculo e persistir resultado")
    public ResponseEntity<ResultadoCalculoResponse> executar(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {
        ResultadoCalculoResponse response = calculoService.executar(id, usuario.getId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/calculos/{id}")
    @Operation(summary = "Excluir cálculo")
    public ResponseEntity<Void> excluir(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {
        calculoService.excluir(id, usuario.getId());
        return ResponseEntity.noContent().build();
    }

    private CalculoResponse toResponse(Calculo calculo) {
        List<CalculoResponse.ParcelaResponse> parcelas = null;
        if (calculo.getParcelas() != null && !calculo.getParcelas().isEmpty()) {
            parcelas = calculo.getParcelas().stream()
                .map(p -> CalculoResponse.ParcelaResponse.builder()
                    .id(p.getId())
                    .descricao(p.getDescricao())
                    .valorOriginal(p.getValorOriginal())
                    .dataVencimento(p.getDataVencimento())
                    .tabelaIndiceId(p.getTabelaIndice() != null ? p.getTabelaIndice().getId() : null)
                    .tabelaIndiceNome(p.getTabelaIndice() != null ? p.getTabelaIndice().getNome() : null)
                    .build())
                .toList();
        }

        // Mapear resultado se existir
        CalculoResponse.ResultadoResponse resultadoResponse = null;
        ResultadoCalculo resultado = calculo.getResultado();
        if (resultado != null) {
            resultadoResponse = CalculoResponse.ResultadoResponse.builder()
                .valorCorrigido(resultado.getValorCorrigido())
                .valorJuros(resultado.getValorJuros())
                .valorMulta(resultado.getValorMulta())
                .valorHonorarios(resultado.getValorHonorarios())
                .valorTotal(resultado.getValorTotal())
                .dataCalculo(resultado.getDataCalculo())
                .detalhamento(resultado.getDetalhamentoJson())
                .build();
        }

        return CalculoResponse.builder()
            .id(calculo.getId())
            .processoId(calculo.getProcesso() != null ? calculo.getProcesso().getId() : null)
            .titulo(calculo.getTitulo())
            .tipoCalculo(calculo.getTipoCalculo())
            .valorPrincipal(calculo.getValorPrincipal())
            .dataInicial(calculo.getDataInicial())
            .dataFinal(calculo.getDataFinal())
            .tabelaIndiceId(calculo.getTabelaIndice() != null ? calculo.getTabelaIndice().getId() : null)
            .tabelaIndiceNome(calculo.getTabelaIndice() != null ? calculo.getTabelaIndice().getNome() : null)
            .tipoJuros(calculo.getTipoJuros())
            .taxaJuros(calculo.getTaxaJuros())
            .periodicidadeJuros(calculo.getPeriodicidadeJuros())
            .multaPercentual(calculo.getMultaPercentual())
            .honorariosPercentual(calculo.getHonorariosPercentual())
            .jurosSobreCorrigido(calculo.getJurosSobreCorrigido())
            .status(calculo.getStatus())
            .dataCriacao(calculo.getDataCriacao())
            .dataAtualizacao(calculo.getDataAtualizacao())
            .parcelas(parcelas)
            .resultado(resultadoResponse)
            .build();
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CalculoResponse {
        private Long id;
        private Long processoId;
        private String titulo;
        private com.calculosjuridicos.entity.TipoCalculo tipoCalculo;
        private java.math.BigDecimal valorPrincipal;
        private java.time.LocalDate dataInicial;
        private java.time.LocalDate dataFinal;
        private Long tabelaIndiceId;
        private String tabelaIndiceNome;
        private com.calculosjuridicos.entity.TipoJuros tipoJuros;
        private java.math.BigDecimal taxaJuros;
        private com.calculosjuridicos.entity.PeriodicidadeJuros periodicidadeJuros;
        private java.math.BigDecimal multaPercentual;
        private java.math.BigDecimal honorariosPercentual;
        private Boolean jurosSobreCorrigido;
        private com.calculosjuridicos.entity.StatusCalculo status;
        private java.time.LocalDateTime dataCriacao;
        private java.time.LocalDateTime dataAtualizacao;
        private List<ParcelaResponse> parcelas;
        private ResultadoResponse resultado;

        @lombok.Data
        @lombok.Builder
        @lombok.NoArgsConstructor
        @lombok.AllArgsConstructor
        public static class ParcelaResponse {
            private Long id;
            private String descricao;
            private java.math.BigDecimal valorOriginal;
            private java.time.LocalDate dataVencimento;
            private Long tabelaIndiceId;
            private String tabelaIndiceNome;
        }

        @lombok.Data
        @lombok.Builder
        @lombok.NoArgsConstructor
        @lombok.AllArgsConstructor
        public static class ResultadoResponse {
            private java.math.BigDecimal valorCorrigido;
            private java.math.BigDecimal valorJuros;
            private java.math.BigDecimal valorMulta;
            private java.math.BigDecimal valorHonorarios;
            private java.math.BigDecimal valorTotal;
            private java.time.LocalDateTime dataCalculo;
            private String detalhamento;
        }
    }
}
