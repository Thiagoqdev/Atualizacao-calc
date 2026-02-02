package com.calculosjuridicos.controller;

import com.calculosjuridicos.entity.Usuario;
import com.calculosjuridicos.service.RelatorioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/calculos")
@RequiredArgsConstructor
@Tag(name = "Relatórios", description = "Endpoints para geração de relatórios")
public class RelatorioController {

    private final RelatorioService relatorioService;

    @GetMapping("/{id}/relatorio")
    @Operation(summary = "Gerar relatório do cálculo em PDF ou Excel")
    public ResponseEntity<byte[]> gerarRelatorio(
            @PathVariable Long id,
            @RequestParam(defaultValue = "pdf") String formato,
            @RequestParam(defaultValue = "completo") String nivel,
            @AuthenticationPrincipal Usuario usuario) {

        byte[] conteudo;
        String contentType;
        String filename;

        if ("xlsx".equalsIgnoreCase(formato) || "excel".equalsIgnoreCase(formato)) {
            conteudo = relatorioService.gerarExcel(id, usuario.getId(), nivel);
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            filename = "calculo_" + id + ".xlsx";
        } else {
            conteudo = relatorioService.gerarPdf(id, usuario.getId(), nivel);
            contentType = MediaType.APPLICATION_PDF_VALUE;
            filename = "calculo_" + id + ".pdf";
        }

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .body(conteudo);
    }
}
