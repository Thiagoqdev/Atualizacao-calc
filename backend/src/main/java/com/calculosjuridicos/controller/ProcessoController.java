package com.calculosjuridicos.controller;

import com.calculosjuridicos.dto.request.ProcessoRequest;
import com.calculosjuridicos.entity.Processo;
import com.calculosjuridicos.entity.TipoAcao;
import com.calculosjuridicos.entity.Usuario;
import com.calculosjuridicos.service.ProcessoService;
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

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/processos")
@RequiredArgsConstructor
@Tag(name = "Processos", description = "Endpoints para gestão de processos jurídicos")
public class ProcessoController {

    private final ProcessoService processoService;

    @PostMapping
    @Operation(summary = "Criar novo processo")
    public ResponseEntity<ProcessoResponse> criar(
            @Valid @RequestBody ProcessoRequest request,
            @AuthenticationPrincipal Usuario usuario) {
        Processo processo = processoService.criar(request, usuario.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(processo));
    }

    @GetMapping
    @Operation(summary = "Listar processos do usuário")
    public ResponseEntity<Page<ProcessoResponse>> listar(
            @AuthenticationPrincipal Usuario usuario,
            @RequestParam(required = false) String numeroProcesso,
            @RequestParam(required = false) TipoAcao tipoAcao,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<Processo> processos = processoService.listarPorUsuario(
            usuario.getId(), numeroProcesso, tipoAcao, pageable
        );
        return ResponseEntity.ok(processos.map(this::toResponse));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar processo por ID")
    public ResponseEntity<ProcessoResponse> buscarPorId(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {
        Processo processo = processoService.buscarPorId(id, usuario.getId());
        return ResponseEntity.ok(toResponse(processo));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar processo")
    public ResponseEntity<ProcessoResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody ProcessoRequest request,
            @AuthenticationPrincipal Usuario usuario) {
        Processo processo = processoService.atualizar(id, request, usuario.getId());
        return ResponseEntity.ok(toResponse(processo));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir processo")
    public ResponseEntity<Void> excluir(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {
        processoService.excluir(id, usuario.getId());
        return ResponseEntity.noContent().build();
    }

    private ProcessoResponse toResponse(Processo processo) {
        return ProcessoResponse.builder()
            .id(processo.getId())
            .numeroProcesso(processo.getNumeroProcesso())
            .descricao(processo.getDescricao())
            .varaTribunal(processo.getVaraTribunal())
            .tipoAcao(processo.getTipoAcao())
            .dataCriacao(processo.getDataCriacao())
            .quantidadeCalculos(processo.getCalculos() != null ? processo.getCalculos().size() : 0)
            .build();
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ProcessoResponse {
        private Long id;
        private String numeroProcesso;
        private String descricao;
        private String varaTribunal;
        private TipoAcao tipoAcao;
        private LocalDateTime dataCriacao;
        private int quantidadeCalculos;
    }
}
