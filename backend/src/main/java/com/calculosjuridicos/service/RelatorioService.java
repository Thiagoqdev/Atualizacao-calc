package com.calculosjuridicos.service;

import com.calculosjuridicos.dto.request.CalculoRequest;
import com.calculosjuridicos.dto.response.ResultadoCalculoResponse;
import com.calculosjuridicos.entity.Calculo;
import com.calculosjuridicos.entity.ResultadoCalculo;
import com.calculosjuridicos.entity.TipoCalculo;
import com.calculosjuridicos.exception.BusinessException;
import com.calculosjuridicos.exception.ResourceNotFoundException;
import com.calculosjuridicos.repository.CalculoRepository;
import com.calculosjuridicos.repository.ResultadoCalculoRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class RelatorioService {

    private final CalculoRepository calculoRepository;
    private final ResultadoCalculoRepository resultadoCalculoRepository;
    private final ObjectMapper objectMapper;

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Cores modernas
    private static final Color HEADER_BG = new Color(44, 62, 80);       // #2C3E50
    private static final Color HEADER_BG_DARK = new Color(52, 73, 94);  // #34495E
    private static final Color ZEBRA_BG = new Color(248, 249, 250);     // #F8F9FA
    private static final Color BORDER_COLOR = new Color(222, 226, 230); // #DEE2E6
    private static final Color TOTAL_BG = new Color(214, 234, 216);     // verde claro
    private static final Color SUBTOTAL_BG = new Color(236, 240, 241);  // #ECF0F1
    private static final Color GREEN_ACCENT = new Color(39, 174, 96);   // #27AE60

    // =====================================================================
    //  GERADORES PRINCIPAIS
    // =====================================================================

    @Transactional(readOnly = true)
    public byte[] gerarPdf(Long calculoId, Long usuarioId, String nivel) {
        Calculo calculo = calculoRepository.findByIdAndUsuarioId(calculoId, usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Calculo", "id", calculoId));

        ResultadoCalculo resultado = resultadoCalculoRepository.findByCalculoId(calculoId)
            .orElseThrow(() -> new BusinessException("Cálculo ainda não foi executado"));

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, baos);
            document.open();

            adicionarCabecalhoPdf(document, calculo);

            if (calculo.getTipoCalculo() != null && calculo.getTipoCalculo() == TipoCalculo.FAZENDA_PUBLICA) {
                adicionarNotaLegislativaPdf(document);
            }

            if (calculo.getProcesso() != null) {
                adicionarDadosProcessoPdf(document, calculo);
            }

            adicionarParametrosPdf(document, calculo);
            adicionarResultadoPdf(document, resultado);

            if ("completo".equals(nivel) && resultado.getDetalhamentoJson() != null) {
                adicionarDetalhamentoPdf(document, calculo, resultado);
            }

            adicionarRodapePdf(document);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Erro ao gerar PDF: ", e);
            throw new BusinessException("Erro ao gerar relatório PDF");
        }
    }

    @Transactional(readOnly = true)
    public byte[] gerarExcel(Long calculoId, Long usuarioId, String nivel) {
        Calculo calculo = calculoRepository.findByIdAndUsuarioId(calculoId, usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Calculo", "id", calculoId));

        ResultadoCalculo resultado = resultadoCalculoRepository.findByCalculoId(calculoId)
            .orElseThrow(() -> new BusinessException("Cálculo ainda não foi executado"));

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            // Fundamentação Legal (aba separada para Fazenda Pública)
            if (calculo.getTipoCalculo() != null && calculo.getTipoCalculo() == TipoCalculo.FAZENDA_PUBLICA) {
                Sheet legalSheet = workbook.createSheet("Fundamentação Legal");
                criarAbaFundamentacaoLegal(workbook, legalSheet);
            }

            Sheet resumoSheet = workbook.createSheet("Resumo");
            criarAbaResumo(workbook, resumoSheet, calculo, resultado);

            Sheet parametrosSheet = workbook.createSheet("Parâmetros");
            criarAbaParametros(workbook, parametrosSheet, calculo);

            if ("completo".equals(nivel) && resultado.getDetalhamentoJson() != null) {
                Sheet detalhamentoSheet = workbook.createSheet("Evolução Mensal");
                criarAbaDetalhamento(workbook, detalhamentoSheet, calculo, resultado);
            }

            workbook.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Erro ao gerar Excel: ", e);
            throw new BusinessException("Erro ao gerar relatório Excel");
        }
    }

    /**
     * Gera relatório PDF a partir de um preview (sem persistir).
     */
    public byte[] gerarPdfPreview(CalculoRequest request, ResultadoCalculoResponse resultado) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, baos);
            document.open();

            adicionarCabecalhoPreviewPdf(document, request);
            if (request.getTipoCalculo() == TipoCalculo.FAZENDA_PUBLICA) {
                adicionarNotaLegislativaPdf(document);
            }
            adicionarParametrosPreviewPdf(document, request);
            adicionarResultadoPreviewPdf(document, resultado);

            if (resultado.getDetalhamento() != null && !resultado.getDetalhamento().isEmpty()) {
                adicionarDetalhamentoPreviewPdf(document, request, resultado);
            }

            adicionarRodapePdf(document);
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Erro ao gerar PDF preview: ", e);
            throw new BusinessException("Erro ao gerar relatório PDF");
        }
    }

    /**
     * Gera relatório Excel a partir de um preview (sem persistir).
     */
    public byte[] gerarExcelPreview(CalculoRequest request, ResultadoCalculoResponse resultado) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            // Fundamentação Legal (aba separada para Fazenda Pública)
            if (request.getTipoCalculo() != null && request.getTipoCalculo() == TipoCalculo.FAZENDA_PUBLICA) {
                Sheet legalSheet = workbook.createSheet("Fundamentação Legal");
                criarAbaFundamentacaoLegal(workbook, legalSheet);
            }

            Sheet resumoSheet = workbook.createSheet("Resumo");
            criarAbaResumoPreview(workbook, resumoSheet, request, resultado);

            if (resultado.getDetalhamento() != null && !resultado.getDetalhamento().isEmpty()) {
                Sheet detalhamentoSheet = workbook.createSheet("Evolução Mensal");
                criarAbaDetalhamentoPreview(workbook, detalhamentoSheet, resultado);
            }

            workbook.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Erro ao gerar Excel preview: ", e);
            throw new BusinessException("Erro ao gerar relatório Excel");
        }
    }

    /**
     * Gera relatório Word a partir de um preview (sem persistir).
     */
    public byte[] gerarWordPreview(CalculoRequest request, ResultadoCalculoResponse resultado) {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            // Título
            XWPFParagraph title = document.createParagraph();
            title.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = title.createRun();
            titleRun.setText("MEMORIAL DE CÁLCULO");
            titleRun.setBold(true);
            titleRun.setFontSize(18);
            titleRun.setColor("2C3E50");

            // Subtítulo
            XWPFParagraph subtitle = document.createParagraph();
            subtitle.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun subtitleRun = subtitle.createRun();
            subtitleRun.setText(request.getTitulo() != null ? request.getTitulo() : "Preview de Cálculo");
            subtitleRun.setFontSize(12);
            subtitleRun.setColor("808080");

            document.createParagraph();

            // Fundamentação Legal (antes dos parâmetros)
            if (request.getTipoCalculo() != null && request.getTipoCalculo() == TipoCalculo.FAZENDA_PUBLICA) {
                adicionarNotaLegislativaWord(document);
            }

            // Parâmetros
            adicionarSecaoWord(document, "PARÂMETROS DO CÁLCULO");
            boolean isFazenda = request.getTipoCalculo() != null && request.getTipoCalculo() == TipoCalculo.FAZENDA_PUBLICA;
            int paramRows = isFazenda ? 7 : 6;
            XWPFTable paramTable = document.createTable(paramRows, 2);
            setWordTableWidth(paramTable);
            int pRow = 0;
            if (isFazenda) {
                setWordTableRow(paramTable, pRow++, "Tipo de Cálculo:", "Condenação da Fazenda Pública");
            }
            setWordTableRow(paramTable, pRow++, "Valor Principal:", CURRENCY_FORMAT.format(request.getValorPrincipal()));
            setWordTableRow(paramTable, pRow++, "Data Inicial:", request.getDataInicial().format(DATE_FORMAT));
            setWordTableRow(paramTable, pRow++, "Data Final:", request.getDataFinal().format(DATE_FORMAT));
            if (isFazenda) {
                setWordTableRow(paramTable, pRow++, "Índice de Correção:", "Automático conforme legislação (INPC/IPCA-E/SELIC)");
                setWordTableRow(paramTable, pRow++, "Juros Moratórios:", "Conforme legislação vigente");
            } else {
                setWordTableRow(paramTable, pRow++, "Tipo de Juros:", request.getTipoJuros() != null ? request.getTipoJuros().toString() : "-");
                setWordTableRow(paramTable, pRow++, "Taxa de Juros:", request.getTaxaJuros() != null ? request.getTaxaJuros() + "%" : "-");
            }
            setWordTableRow(paramTable, pRow++, "Multa / Honorários:",
                (request.getMultaPercentual() != null ? request.getMultaPercentual() : "0") + "% / " +
                (request.getHonorariosPercentual() != null ? request.getHonorariosPercentual() : "0") + "%");
            document.createParagraph();

            // Resultado
            adicionarSecaoWord(document, "RESULTADO");
            XWPFTable resultTable = document.createTable(4, 2);
            setWordTableWidth(resultTable);
            setWordTableRow(resultTable, 0, "Valor Corrigido:", CURRENCY_FORMAT.format(resultado.getValorCorrigido()));
            setWordTableRow(resultTable, 1, "Juros:", CURRENCY_FORMAT.format(resultado.getValorJuros()));
            setWordTableRow(resultTable, 2, "Multa:", CURRENCY_FORMAT.format(resultado.getValorMulta()));
            setWordTableRow(resultTable, 3, "Honorários:", CURRENCY_FORMAT.format(resultado.getValorHonorarios()));

            XWPFParagraph totalParagraph = document.createParagraph();
            totalParagraph.setAlignment(ParagraphAlignment.RIGHT);
            XWPFRun totalRun = totalParagraph.createRun();
            totalRun.setText("VALOR TOTAL: " + CURRENCY_FORMAT.format(resultado.getValorTotal()));
            totalRun.setBold(true);
            totalRun.setFontSize(14);
            totalRun.setColor("27AE60");

            // Detalhamento mensal
            if (resultado.getDetalhamento() != null && !resultado.getDetalhamento().isEmpty()) {
                document.createParagraph();
                adicionarDetalhamentoPreviewWord(document, request, resultado);
            }

            // Rodapé
            document.createParagraph();
            XWPFParagraph footer = document.createParagraph();
            footer.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun footerRun = footer.createRun();
            footerRun.setText("Memorial gerado pelo Sistema de Cálculos Jurídicos em " +
                java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            footerRun.setFontSize(8);
            footerRun.setItalic(true);
            footerRun.setColor("808080");

            document.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Erro ao gerar Word preview: ", e);
            throw new BusinessException("Erro ao gerar relatório Word");
        }
    }

    @Transactional(readOnly = true)
    public byte[] gerarWord(Long calculoId, Long usuarioId, String nivel) {
        Calculo calculo = calculoRepository.findByIdAndUsuarioId(calculoId, usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Calculo", "id", calculoId));

        ResultadoCalculo resultado = resultadoCalculoRepository.findByCalculoId(calculoId)
            .orElseThrow(() -> new BusinessException("Cálculo ainda não foi executado"));

        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            // Título
            XWPFParagraph title = document.createParagraph();
            title.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = title.createRun();
            titleRun.setText("RELATÓRIO DE CÁLCULO");
            titleRun.setBold(true);
            titleRun.setFontSize(18);
            titleRun.setColor("2C3E50");

            // Subtítulo
            XWPFParagraph subtitle = document.createParagraph();
            subtitle.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun subtitleRun = subtitle.createRun();
            subtitleRun.setText(calculo.getTitulo());
            subtitleRun.setFontSize(12);
            subtitleRun.setColor("808080");

            document.createParagraph(); // espaço

            // Fundamentação Legal (antes de parâmetros)
            if (calculo.getTipoCalculo() != null && calculo.getTipoCalculo() == TipoCalculo.FAZENDA_PUBLICA) {
                adicionarNotaLegislativaWord(document);
            }

            // Dados do processo
            if (calculo.getProcesso() != null) {
                adicionarSecaoWord(document, "DADOS DO PROCESSO");
                XWPFTable processoTable = document.createTable(3, 2);
                setWordTableWidth(processoTable);
                setWordTableRow(processoTable, 0, "Número:", calculo.getProcesso().getNumeroProcesso() != null ?
                    calculo.getProcesso().getNumeroProcesso() : "-");
                setWordTableRow(processoTable, 1, "Tipo de Ação:", calculo.getProcesso().getTipoAcao().toString());
                setWordTableRow(processoTable, 2, "Vara/Tribunal:", calculo.getProcesso().getVaraTribunal() != null ?
                    calculo.getProcesso().getVaraTribunal() : "-");
                document.createParagraph();
            }

            // Parâmetros do cálculo
            boolean isFazendaWord = calculo.getTipoCalculo() != null && calculo.getTipoCalculo() == TipoCalculo.FAZENDA_PUBLICA;
            adicionarSecaoWord(document, "PARÂMETROS DO CÁLCULO");
            int wParamRows = isFazendaWord ? 7 : 8;
            XWPFTable paramTable = document.createTable(wParamRows, 2);
            setWordTableWidth(paramTable);
            int wp = 0;
            if (isFazendaWord) {
                setWordTableRow(paramTable, wp++, "Tipo de Cálculo:", "Condenação da Fazenda Pública");
            }
            setWordTableRow(paramTable, wp++, "Valor Principal:", CURRENCY_FORMAT.format(calculo.getValorPrincipal()));
            setWordTableRow(paramTable, wp++, "Data Inicial:", calculo.getDataInicial().format(DATE_FORMAT));
            setWordTableRow(paramTable, wp++, "Data Final:", calculo.getDataFinal().format(DATE_FORMAT));
            if (isFazendaWord) {
                setWordTableRow(paramTable, wp++, "Índice de Correção:", "Automático conforme legislação (INPC/IPCA-E/SELIC)");
                setWordTableRow(paramTable, wp++, "Juros Moratórios:", "Conforme legislação vigente");
            } else {
                setWordTableRow(paramTable, wp++, "Índice de Correção:", calculo.getTabelaIndice() != null ?
                    calculo.getTabelaIndice().getNome() : "Sem correção");
                setWordTableRow(paramTable, wp++, "Tipo de Juros:", calculo.getTipoJuros().toString());
                setWordTableRow(paramTable, wp++, "Taxa de Juros:", calculo.getTaxaJuros() != null ?
                    calculo.getTaxaJuros() + "% " + calculo.getPeriodicidadeJuros() : "-");
            }
            setWordTableRow(paramTable, wp++, "Multa:", calculo.getMultaPercentual() + "%");
            setWordTableRow(paramTable, wp++, "Honorários:", calculo.getHonorariosPercentual() + "%");
            document.createParagraph();

            // Resultado
            adicionarSecaoWord(document, "RESULTADO");
            XWPFTable resultTable = document.createTable(4, 2);
            setWordTableWidth(resultTable);
            setWordTableRow(resultTable, 0, "Valor Corrigido:", CURRENCY_FORMAT.format(resultado.getValorCorrigido()));
            setWordTableRow(resultTable, 1, "Juros:", CURRENCY_FORMAT.format(resultado.getValorJuros()));
            setWordTableRow(resultTable, 2, "Multa:", CURRENCY_FORMAT.format(resultado.getValorMulta()));
            setWordTableRow(resultTable, 3, "Honorários:", CURRENCY_FORMAT.format(resultado.getValorHonorarios()));

            XWPFParagraph totalParagraph = document.createParagraph();
            totalParagraph.setAlignment(ParagraphAlignment.RIGHT);
            XWPFRun totalRun = totalParagraph.createRun();
            totalRun.setText("VALOR TOTAL: " + CURRENCY_FORMAT.format(resultado.getValorTotal()));
            totalRun.setBold(true);
            totalRun.setFontSize(14);
            totalRun.setColor("27AE60");

            // Detalhamento mensal
            if ("completo".equals(nivel) && resultado.getDetalhamentoJson() != null) {
                document.createParagraph();
                adicionarDetalhamentoWord(document, calculo, resultado);
            }

            // Rodapé
            document.createParagraph();
            XWPFParagraph footer = document.createParagraph();
            footer.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun footerRun = footer.createRun();
            footerRun.setText("Relatório gerado pelo Sistema de Cálculos Jurídicos em " +
                java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            footerRun.setFontSize(8);
            footerRun.setItalic(true);
            footerRun.setColor("808080");

            document.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Erro ao gerar Word: ", e);
            throw new BusinessException("Erro ao gerar relatório Word");
        }
    }

    // =====================================================================
    //  WORD - DETALHAMENTO (SALVO)
    // =====================================================================

    private void adicionarDetalhamentoWord(XWPFDocument document, Calculo calculo, ResultadoCalculo resultado) {
        adicionarSecaoWord(document, "EVOLUÇÃO MENSAL");

        try {
            List<ResultadoCalculoResponse.DetalhamentoMensalResponse> detalhamento =
                objectMapper.readValue(resultado.getDetalhamentoJson(),
                    new TypeReference<List<ResultadoCalculoResponse.DetalhamentoMensalResponse>>() {});

            int extraRows = 2; // subtotal + total geral
            if (resultado.getValorMulta() != null && resultado.getValorMulta().compareTo(BigDecimal.ZERO) > 0) extraRows++;
            if (resultado.getValorHonorarios() != null && resultado.getValorHonorarios().compareTo(BigDecimal.ZERO) > 0) extraRows++;

            String[] headers = {"Competência", "Índice Aplicado", "Variação (%)", "Valor Corrigido", "Juros", "Subtotal"};
            XWPFTable table = document.createTable(detalhamento.size() + 1 + extraRows, headers.length);
            setWordTableWidth(table);

            // Cabeçalho com fundo escuro
            XWPFTableRow headerRow = table.getRow(0);
            for (int i = 0; i < headers.length; i++) {
                XWPFRun run = headerRow.getCell(i).getParagraphs().get(0).createRun();
                run.setText(headers[i]);
                run.setBold(true);
                run.setFontSize(8);
                run.setColor("FFFFFF");
                headerRow.getCell(i).setColor("2C3E50");
            }

            // Dados com zebra striping
            for (int i = 0; i < detalhamento.size(); i++) {
                ResultadoCalculoResponse.DetalhamentoMensalResponse det = detalhamento.get(i);
                XWPFTableRow dataRow = table.getRow(i + 1);

                // Zebra: linhas ímpares com fundo cinza claro
                if (i % 2 == 1) {
                    for (int c = 0; c < headers.length; c++) {
                        dataRow.getCell(c).setColor("F8F9FA");
                    }
                }

                setWordCellText(dataRow, 0, det.getCompetencia(), 8);
                setWordCellText(dataRow, 1, det.getNomeIndice() != null ? det.getNomeIndice() : "-", 8);
                setWordCellText(dataRow, 2, det.getVariacaoPercentual() != null ?
                    String.format("%.4f%%", det.getVariacaoPercentual()) : "-", 8);
                setWordCellText(dataRow, 3, CURRENCY_FORMAT.format(det.getValorCorrigidoParcial()), 8);
                setWordCellText(dataRow, 4, det.getJurosParcial() != null ?
                    CURRENCY_FORMAT.format(det.getJurosParcial()) : CURRENCY_FORMAT.format(BigDecimal.ZERO), 8);
                setWordCellText(dataRow, 5, CURRENCY_FORMAT.format(det.getSubtotalParcial()), 8);
            }

            // Linhas de totais
            int footerIdx = detalhamento.size() + 1;
            BigDecimal subtotal = resultado.getValorCorrigido().add(resultado.getValorJuros());
            setWordBoldCellText(table.getRow(footerIdx), 4, "SUBTOTAL", 8);
            setWordBoldCellText(table.getRow(footerIdx), 5, CURRENCY_FORMAT.format(subtotal), 8);
            footerIdx++;

            if (resultado.getValorMulta() != null && resultado.getValorMulta().compareTo(BigDecimal.ZERO) > 0) {
                setWordBoldCellText(table.getRow(footerIdx), 4, "Multa", 8);
                setWordBoldCellText(table.getRow(footerIdx), 5, CURRENCY_FORMAT.format(resultado.getValorMulta()), 8);
                footerIdx++;
            }

            if (resultado.getValorHonorarios() != null && resultado.getValorHonorarios().compareTo(BigDecimal.ZERO) > 0) {
                setWordBoldCellText(table.getRow(footerIdx), 4, "Honorários", 8);
                setWordBoldCellText(table.getRow(footerIdx), 5, CURRENCY_FORMAT.format(resultado.getValorHonorarios()), 8);
                footerIdx++;
            }

            XWPFTableRow totalRow = table.getRow(footerIdx);
            for (int c = 0; c < headers.length; c++) {
                totalRow.getCell(c).setColor("D6EAD8");
            }
            setWordBoldCellText(totalRow, 4, "TOTAL GERAL", 8);
            setWordBoldCellText(totalRow, 5, CURRENCY_FORMAT.format(resultado.getValorTotal()), 8);

        } catch (Exception e) {
            log.warn("Erro ao processar detalhamento Word: ", e);
        }
    }

    // =====================================================================
    //  WORD - DETALHAMENTO (PREVIEW)
    // =====================================================================

    private void adicionarDetalhamentoPreviewWord(XWPFDocument document, CalculoRequest request, ResultadoCalculoResponse resultado) {
        adicionarSecaoWord(document, "EVOLUÇÃO MENSAL");

        List<ResultadoCalculoResponse.DetalhamentoMensalResponse> detalhamento = resultado.getDetalhamento();
        int extraRows = 2; // subtotal + total geral
        if (resultado.getValorMulta() != null && resultado.getValorMulta().compareTo(BigDecimal.ZERO) > 0) extraRows++;
        if (resultado.getValorHonorarios() != null && resultado.getValorHonorarios().compareTo(BigDecimal.ZERO) > 0) extraRows++;

        String[] headers = {"Competência", "Índice Aplicado", "Variação (%)", "Valor Corrigido", "Juros", "Subtotal"};
        XWPFTable table = document.createTable(detalhamento.size() + 1 + extraRows, headers.length);
        setWordTableWidth(table);

        // Cabeçalho
        XWPFTableRow headerRow = table.getRow(0);
        for (int i = 0; i < headers.length; i++) {
            XWPFRun run = headerRow.getCell(i).getParagraphs().get(0).createRun();
            run.setText(headers[i]);
            run.setBold(true);
            run.setFontSize(8);
            run.setColor("FFFFFF");
            headerRow.getCell(i).setColor("2C3E50");
        }

        // Dados com zebra striping
        for (int i = 0; i < detalhamento.size(); i++) {
            ResultadoCalculoResponse.DetalhamentoMensalResponse det = detalhamento.get(i);
            XWPFTableRow dataRow = table.getRow(i + 1);

            if (i % 2 == 1) {
                for (int c = 0; c < headers.length; c++) {
                    dataRow.getCell(c).setColor("F8F9FA");
                }
            }

            setWordCellText(dataRow, 0, det.getCompetencia(), 8);
            setWordCellText(dataRow, 1, det.getNomeIndice() != null ? det.getNomeIndice() : "-", 8);
            setWordCellText(dataRow, 2, det.getVariacaoPercentual() != null ?
                String.format("%.4f%%", det.getVariacaoPercentual()) : "-", 8);
            setWordCellText(dataRow, 3, CURRENCY_FORMAT.format(det.getValorCorrigidoParcial()), 8);
            setWordCellText(dataRow, 4, det.getJurosParcial() != null ?
                CURRENCY_FORMAT.format(det.getJurosParcial()) : CURRENCY_FORMAT.format(BigDecimal.ZERO), 8);
            setWordCellText(dataRow, 5, CURRENCY_FORMAT.format(det.getSubtotalParcial()), 8);
        }

        // Linhas de totais
        int footerIdx = detalhamento.size() + 1;
        setWordBoldCellText(table.getRow(footerIdx), 4, "SUBTOTAL", 8);
        setWordBoldCellText(table.getRow(footerIdx), 5,
            CURRENCY_FORMAT.format(resultado.getValorCorrigido().add(resultado.getValorJuros())), 8);
        footerIdx++;

        if (resultado.getValorMulta() != null && resultado.getValorMulta().compareTo(BigDecimal.ZERO) > 0) {
            setWordBoldCellText(table.getRow(footerIdx), 4, "Multa", 8);
            setWordBoldCellText(table.getRow(footerIdx), 5, CURRENCY_FORMAT.format(resultado.getValorMulta()), 8);
            footerIdx++;
        }

        if (resultado.getValorHonorarios() != null && resultado.getValorHonorarios().compareTo(BigDecimal.ZERO) > 0) {
            setWordBoldCellText(table.getRow(footerIdx), 4, "Honorários", 8);
            setWordBoldCellText(table.getRow(footerIdx), 5, CURRENCY_FORMAT.format(resultado.getValorHonorarios()), 8);
            footerIdx++;
        }

        XWPFTableRow totalRow = table.getRow(footerIdx);
        for (int c = 0; c < headers.length; c++) {
            totalRow.getCell(c).setColor("D6EAD8");
        }
        setWordBoldCellText(totalRow, 4, "TOTAL GERAL", 8);
        setWordBoldCellText(totalRow, 5, CURRENCY_FORMAT.format(resultado.getValorTotal()), 8);
    }

    // =====================================================================
    //  WORD - MÉTODOS AUXILIARES
    // =====================================================================

    private void adicionarSecaoWord(XWPFDocument document, String titulo) {
        XWPFParagraph section = document.createParagraph();
        XWPFRun run = section.createRun();
        run.setText(titulo);
        run.setBold(true);
        run.setFontSize(12);
        run.setColor("2C3E50");
    }

    private void setWordTableWidth(XWPFTable table) {
        table.setWidth("100%");
    }

    private void setWordTableRow(XWPFTable table, int rowIndex, String label, String value) {
        XWPFTableRow row = table.getRow(rowIndex);
        XWPFRun labelRun = row.getCell(0).getParagraphs().get(0).createRun();
        labelRun.setText(label);
        labelRun.setBold(true);
        labelRun.setFontSize(10);
        XWPFRun valueRun = row.getCell(1).getParagraphs().get(0).createRun();
        valueRun.setText(value);
        valueRun.setFontSize(10);
    }

    private void setWordCellText(XWPFTableRow row, int cellIndex, String text, int fontSize) {
        XWPFRun run = row.getCell(cellIndex).getParagraphs().get(0).createRun();
        run.setText(text);
        run.setFontSize(fontSize);
    }

    private void setWordBoldCellText(XWPFTableRow row, int cellIndex, String text, int fontSize) {
        XWPFRun run = row.getCell(cellIndex).getParagraphs().get(0).createRun();
        run.setText(text);
        run.setFontSize(fontSize);
        run.setBold(true);
    }

    private void adicionarNotaLegislativaWord(XWPFDocument document) {
        document.createParagraph();

        // Título
        XWPFParagraph tituloP = document.createParagraph();
        tituloP.setAlignment(ParagraphAlignment.LEFT);
        XWPFRun tituloRun = tituloP.createRun();
        tituloRun.setText("FUNDAMENTAÇÃO LEGAL");
        tituloRun.setBold(true);
        tituloRun.setFontSize(9);
        tituloRun.setColor("2C3E50");

        // Correção Monetária
        XWPFParagraph correcaoTitleP = document.createParagraph();
        XWPFRun correcaoTitleRun = correcaoTitleP.createRun();
        correcaoTitleRun.setText("Correção Monetária:");
        correcaoTitleRun.setBold(true);
        correcaoTitleRun.setFontSize(7);
        correcaoTitleRun.setColor("34495E");

        XWPFParagraph correcaoP = document.createParagraph();
        correcaoP.setAlignment(ParagraphAlignment.LEFT);
        String[] correcaoItens = {
            "INPC (01/1984 a 12/1991) — Prática judicial consolidada, Lei 8.177/91",
            "IPCA-E (01/1992 a 08/12/2021) — Manual de Cálculos da Justiça Federal, Res. CJF 242/2001, Tema 810 STF",
            "SELIC taxa única (09/12/2021 a 09/2025) — EC 113/2021, art. 3º",
            "IPCA + 2% a.a. limitado à SELIC (10/2025 em diante) — EC 136/2025"
        };
        for (String item : correcaoItens) {
            XWPFRun run = correcaoP.createRun();
            run.setText("• " + item);
            run.setFontSize(7);
            run.setItalic(true);
            run.setColor("666666");
            run.addBreak();
        }

        // Juros Moratórios
        XWPFParagraph jurosTitleP = document.createParagraph();
        XWPFRun jurosTitleRun = jurosTitleP.createRun();
        jurosTitleRun.setText("Juros Moratórios:");
        jurosTitleRun.setBold(true);
        jurosTitleRun.setFontSize(7);
        jurosTitleRun.setColor("34495E");

        XWPFParagraph jurosP = document.createParagraph();
        jurosP.setAlignment(ParagraphAlignment.LEFT);
        String[] jurosItens = {
            "1% a.m. simples (até 06/2009) — Art. 406 CC/2002, Art. 161 §1º CTN",
            "0,5% a.m. poupança (07/2009 a 08/12/2021) — Lei 11.960/2009",
            "SELIC (engloba correção + juros) (09/12/2021 a 09/2025) — EC 113/2021",
            "2% a.a. limitado à SELIC (10/2025 em diante) — EC 136/2025"
        };
        for (String item : jurosItens) {
            XWPFRun run = jurosP.createRun();
            run.setText("• " + item);
            run.setFontSize(7);
            run.setItalic(true);
            run.setColor("666666");
            run.addBreak();
        }
    }

    // =====================================================================
    //  PDF - PREVIEW AUXILIARES
    // =====================================================================

    private void adicionarCabecalhoPreviewPdf(Document document, CalculoRequest request) throws DocumentException {
        com.lowagie.text.Font titleFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 18, com.lowagie.text.Font.BOLD, HEADER_BG);
        Paragraph title = new Paragraph("MEMORIAL DE CÁLCULO", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(10);
        document.add(title);

        com.lowagie.text.Font subtitleFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 12, com.lowagie.text.Font.NORMAL, Color.GRAY);
        Paragraph subtitle = new Paragraph(request.getTitulo() != null ? request.getTitulo() : "Preview de Cálculo", subtitleFont);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(20);
        document.add(subtitle);
        document.add(new Paragraph(" "));
    }

    private void adicionarParametrosPreviewPdf(Document document, CalculoRequest request) throws DocumentException {
        com.lowagie.text.Font sectionFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 12, com.lowagie.text.Font.BOLD, HEADER_BG);
        Paragraph section = new Paragraph("PARÂMETROS DO CÁLCULO", sectionFont);
        section.setSpacingBefore(10);
        section.setSpacingAfter(10);
        document.add(section);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 2});

        if (request.getTipoCalculo() != null && request.getTipoCalculo() == TipoCalculo.FAZENDA_PUBLICA) {
            addTableRow(table, "Tipo de Cálculo:", "Condenação da Fazenda Pública");
        }
        addTableRow(table, "Valor Principal:", CURRENCY_FORMAT.format(request.getValorPrincipal()));
        addTableRow(table, "Data Inicial:", request.getDataInicial().format(DATE_FORMAT));
        addTableRow(table, "Data Final:", request.getDataFinal().format(DATE_FORMAT));
        if (request.getTipoCalculo() != null && request.getTipoCalculo() == TipoCalculo.FAZENDA_PUBLICA) {
            addTableRow(table, "Índice de Correção:", "Automático conforme legislação (INPC/IPCA-E/SELIC)");
            addTableRow(table, "Juros Moratórios:", "Conforme legislação vigente");
        } else {
            addTableRow(table, "Tipo de Juros:", request.getTipoJuros() != null ? request.getTipoJuros().toString() : "-");
            addTableRow(table, "Taxa de Juros:", request.getTaxaJuros() != null ? request.getTaxaJuros() + "%" : "-");
        }
        addTableRow(table, "Multa:", (request.getMultaPercentual() != null ? request.getMultaPercentual() : "0") + "%");
        addTableRow(table, "Honorários:", (request.getHonorariosPercentual() != null ? request.getHonorariosPercentual() : "0") + "%");

        document.add(table);
        document.add(new Paragraph(" "));
    }

    private void adicionarResultadoPreviewPdf(Document document, ResultadoCalculoResponse resultado) throws DocumentException {
        com.lowagie.text.Font sectionFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 12, com.lowagie.text.Font.BOLD, HEADER_BG);
        Paragraph section = new Paragraph("RESULTADO", sectionFont);
        section.setSpacingBefore(10);
        section.setSpacingAfter(10);
        document.add(section);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 2});

        addTableRow(table, "Valor Corrigido:", CURRENCY_FORMAT.format(resultado.getValorCorrigido()));
        addTableRow(table, "Juros:", CURRENCY_FORMAT.format(resultado.getValorJuros()));
        addTableRow(table, "Multa:", CURRENCY_FORMAT.format(resultado.getValorMulta()));
        addTableRow(table, "Honorários:", CURRENCY_FORMAT.format(resultado.getValorHonorarios()));

        document.add(table);

        com.lowagie.text.Font totalFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 14, com.lowagie.text.Font.BOLD, GREEN_ACCENT);
        Paragraph total = new Paragraph("VALOR TOTAL: " + CURRENCY_FORMAT.format(resultado.getValorTotal()), totalFont);
        total.setAlignment(Element.ALIGN_RIGHT);
        total.setSpacingBefore(10);
        document.add(total);
        document.add(new Paragraph(" "));
    }

    // =====================================================================
    //  PDF - DETALHAMENTO (PREVIEW)
    // =====================================================================

    private void adicionarDetalhamentoPreviewPdf(Document document, CalculoRequest request, ResultadoCalculoResponse resultado) throws DocumentException {
        com.lowagie.text.Font sectionFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 12, com.lowagie.text.Font.BOLD, HEADER_BG);
        Paragraph section = new Paragraph("EVOLUÇÃO MENSAL", sectionFont);
        section.setSpacingBefore(15);
        section.setSpacingAfter(10);
        document.add(section);

        List<ResultadoCalculoResponse.DetalhamentoMensalResponse> detalhamento = resultado.getDetalhamento();

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.2f, 1.2f, 1f, 1.3f, 1f, 1.3f});

        addHeaderCell(table, "Competência");
        addHeaderCell(table, "Índice Aplicado");
        addHeaderCell(table, "Variação (%)");
        addHeaderCell(table, "Valor Corrigido");
        addHeaderCell(table, "Juros");
        addHeaderCell(table, "Subtotal");

        for (int i = 0; i < detalhamento.size(); i++) {
            ResultadoCalculoResponse.DetalhamentoMensalResponse det = detalhamento.get(i);
            Color bg = (i % 2 == 1) ? ZEBRA_BG : null;

            addCell(table, det.getCompetencia(), bg);
            addCell(table, det.getNomeIndice() != null ? det.getNomeIndice() : "-", bg);
            addCell(table, det.getVariacaoPercentual() != null ?
                String.format("%.4f%%", det.getVariacaoPercentual()) : "-", bg);
            addCell(table, CURRENCY_FORMAT.format(det.getValorCorrigidoParcial()), bg);
            addCell(table, det.getJurosParcial() != null ?
                CURRENCY_FORMAT.format(det.getJurosParcial()) : CURRENCY_FORMAT.format(BigDecimal.ZERO), bg);
            addCell(table, CURRENCY_FORMAT.format(det.getSubtotalParcial()), bg);
        }

        // Linhas de totais
        adicionarLinhaTotaisPdf(table, detalhamento, resultado);

        document.add(table);

    }

    private void adicionarLinhaTotaisPdf(PdfPTable table,
            List<ResultadoCalculoResponse.DetalhamentoMensalResponse> detalhamento,
            ResultadoCalculoResponse resultado) {
        com.lowagie.text.Font boldFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 8, com.lowagie.text.Font.BOLD);

        // Subtotal row — label spans 4 cols, then Juros + Subtotal values
        PdfPCell labelCell = new PdfPCell(new Phrase("SUBTOTAL", boldFont));
        labelCell.setColspan(4);
        labelCell.setPadding(5);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        labelCell.setBackgroundColor(SUBTOTAL_BG);
        labelCell.setBorderWidth(0.5f);
        labelCell.setBorderColor(BORDER_COLOR);
        table.addCell(labelCell);

        BigDecimal jurosTotal = resultado.getValorJuros() != null ? resultado.getValorJuros() : BigDecimal.ZERO;
        BigDecimal subtotal = resultado.getValorCorrigido().add(jurosTotal);
        addBoldCell(table, CURRENCY_FORMAT.format(jurosTotal));
        addBoldCell(table, CURRENCY_FORMAT.format(subtotal));

        // Multa
        if (resultado.getValorMulta() != null && resultado.getValorMulta().compareTo(BigDecimal.ZERO) > 0) {
            PdfPCell multaLabel = new PdfPCell(new Phrase("Multa", boldFont));
            multaLabel.setColspan(5);
            multaLabel.setPadding(5);
            multaLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
            multaLabel.setBackgroundColor(SUBTOTAL_BG);
            multaLabel.setBorderWidth(0.5f);
            multaLabel.setBorderColor(BORDER_COLOR);
            table.addCell(multaLabel);
            addBoldCell(table, CURRENCY_FORMAT.format(resultado.getValorMulta()));
        }

        // Honorários
        if (resultado.getValorHonorarios() != null && resultado.getValorHonorarios().compareTo(BigDecimal.ZERO) > 0) {
            PdfPCell honLabel = new PdfPCell(new Phrase("Honorários", boldFont));
            honLabel.setColspan(5);
            honLabel.setPadding(5);
            honLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
            honLabel.setBackgroundColor(SUBTOTAL_BG);
            honLabel.setBorderWidth(0.5f);
            honLabel.setBorderColor(BORDER_COLOR);
            table.addCell(honLabel);
            addBoldCell(table, CURRENCY_FORMAT.format(resultado.getValorHonorarios()));
        }

        // Total Geral
        com.lowagie.text.Font totalFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.BOLD, GREEN_ACCENT);
        PdfPCell totalLabel = new PdfPCell(new Phrase("TOTAL GERAL", totalFont));
        totalLabel.setColspan(5);
        totalLabel.setPadding(5);
        totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalLabel.setBackgroundColor(TOTAL_BG);
        totalLabel.setBorderWidth(0.5f);
        totalLabel.setBorderColor(BORDER_COLOR);
        table.addCell(totalLabel);

        PdfPCell totalCell = new PdfPCell(new Phrase(CURRENCY_FORMAT.format(resultado.getValorTotal()), totalFont));
        totalCell.setPadding(5);
        totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalCell.setBackgroundColor(TOTAL_BG);
        totalCell.setBorderWidth(0.5f);
        totalCell.setBorderColor(BORDER_COLOR);
        table.addCell(totalCell);
    }

    private void addBoldCell(PdfPTable table, String text) {
        com.lowagie.text.Font font = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 8, com.lowagie.text.Font.BOLD);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setBackgroundColor(SUBTOTAL_BG);
        cell.setBorderWidth(0.5f);
        cell.setBorderColor(BORDER_COLOR);
        table.addCell(cell);
    }

    // =====================================================================
    //  EXCEL - PREVIEW
    // =====================================================================

    private void criarAbaResumoPreview(Workbook workbook, Sheet sheet, CalculoRequest request, ResultadoCalculoResponse resultado) {
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);

        int rowNum = 0;
        org.apache.poi.ss.usermodel.Row titleRow = sheet.createRow(rowNum++);
        org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("MEMORIAL DE CÁLCULO - " + (request.getTitulo() != null ? request.getTitulo() : "Preview"));
        titleCell.setCellStyle(headerStyle);

        rowNum++;
        createResultRow(sheet, rowNum++, "Valor Principal", request.getValorPrincipal(), currencyStyle);
        createResultRow(sheet, rowNum++, "Valor Corrigido", resultado.getValorCorrigido(), currencyStyle);
        createResultRow(sheet, rowNum++, "Juros", resultado.getValorJuros(), currencyStyle);
        createResultRow(sheet, rowNum++, "Multa", resultado.getValorMulta(), currencyStyle);
        createResultRow(sheet, rowNum++, "Honorários", resultado.getValorHonorarios(), currencyStyle);
        rowNum++;
        org.apache.poi.ss.usermodel.Row totalRow = sheet.createRow(rowNum);
        totalRow.createCell(0).setCellValue("VALOR TOTAL");
        org.apache.poi.ss.usermodel.Cell totalValueCell = totalRow.createCell(1);
        totalValueCell.setCellValue(resultado.getValorTotal().doubleValue());
        totalValueCell.setCellStyle(currencyStyle);

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void criarAbaDetalhamentoPreview(Workbook workbook, Sheet sheet, ResultadoCalculoResponse resultado) {
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);
        CellStyle percentStyle = createPercentStyle(workbook);

        // Zebra styles
        CellStyle zebraStyle = createZebraStyle(workbook);
        CellStyle zebraCurrencyStyle = createZebraCurrencyStyle(workbook);
        CellStyle zebraPercentStyle = createZebraPercentStyle(workbook);
        CellStyle borderStyle = createBorderStyle(workbook);
        CellStyle borderCurrencyStyle = createBorderCurrencyStyle(workbook);
        CellStyle borderPercentStyle = createBorderPercentStyle(workbook);

        org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
        String[] headers = {"Competência", "Índice Aplicado", "Variação (%)", "Valor Corrigido", "Juros", "Subtotal"};
        for (int i = 0; i < headers.length; i++) {
            org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        List<ResultadoCalculoResponse.DetalhamentoMensalResponse> detalhamento = resultado.getDetalhamento();

        int rowNum = 1;
        for (int idx = 0; idx < detalhamento.size(); idx++) {
            ResultadoCalculoResponse.DetalhamentoMensalResponse det = detalhamento.get(idx);
            org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
            boolean isZebra = (idx % 2 == 1);

            org.apache.poi.ss.usermodel.Cell compCell = row.createCell(0);
            compCell.setCellValue(det.getCompetencia());
            compCell.setCellStyle(isZebra ? zebraStyle : borderStyle);

            org.apache.poi.ss.usermodel.Cell indiceCell = row.createCell(1);
            indiceCell.setCellValue(det.getNomeIndice() != null ? det.getNomeIndice() : "-");
            indiceCell.setCellStyle(isZebra ? zebraStyle : borderStyle);

            org.apache.poi.ss.usermodel.Cell variacaoCell = row.createCell(2);
            if (det.getVariacaoPercentual() != null) {
                variacaoCell.setCellValue(det.getVariacaoPercentual().doubleValue());
                variacaoCell.setCellStyle(isZebra ? zebraPercentStyle : borderPercentStyle);
            } else {
                variacaoCell.setCellValue("-");
                variacaoCell.setCellStyle(isZebra ? zebraStyle : borderStyle);
            }

            org.apache.poi.ss.usermodel.Cell corrigidoCell = row.createCell(3);
            corrigidoCell.setCellValue(det.getValorCorrigidoParcial() != null ? det.getValorCorrigidoParcial().doubleValue() : 0);
            corrigidoCell.setCellStyle(isZebra ? zebraCurrencyStyle : borderCurrencyStyle);

            org.apache.poi.ss.usermodel.Cell jurosCell = row.createCell(4);
            jurosCell.setCellValue(det.getJurosParcial() != null ? det.getJurosParcial().doubleValue() : 0);
            jurosCell.setCellStyle(isZebra ? zebraCurrencyStyle : borderCurrencyStyle);

            org.apache.poi.ss.usermodel.Cell subtotalCell = row.createCell(5);
            subtotalCell.setCellValue(det.getSubtotalParcial() != null ? det.getSubtotalParcial().doubleValue() : 0);
            subtotalCell.setCellStyle(isZebra ? zebraCurrencyStyle : borderCurrencyStyle);
        }

        // Linha de totais
        adicionarLinhaTotaisExcel(sheet, rowNum, resultado, currencyStyle, headerStyle);

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void adicionarLinhaTotaisExcel(Sheet sheet, int rowNum,
            ResultadoCalculoResponse resultado, CellStyle currencyStyle, CellStyle headerStyle) {
        // Subtotal
        rowNum++;
        org.apache.poi.ss.usermodel.Row subtotalRow = sheet.createRow(rowNum++);
        org.apache.poi.ss.usermodel.Cell stLabel = subtotalRow.createCell(4);
        stLabel.setCellValue("SUBTOTAL");
        stLabel.setCellStyle(headerStyle);
        org.apache.poi.ss.usermodel.Cell stValue = subtotalRow.createCell(5);
        stValue.setCellValue(resultado.getValorCorrigido().add(resultado.getValorJuros()).doubleValue());
        stValue.setCellStyle(currencyStyle);

        // Multa
        if (resultado.getValorMulta() != null && resultado.getValorMulta().compareTo(BigDecimal.ZERO) > 0) {
            org.apache.poi.ss.usermodel.Row multaRow = sheet.createRow(rowNum++);
            multaRow.createCell(4).setCellValue("Multa");
            org.apache.poi.ss.usermodel.Cell multaValue = multaRow.createCell(5);
            multaValue.setCellValue(resultado.getValorMulta().doubleValue());
            multaValue.setCellStyle(currencyStyle);
        }

        // Honorários
        if (resultado.getValorHonorarios() != null && resultado.getValorHonorarios().compareTo(BigDecimal.ZERO) > 0) {
            org.apache.poi.ss.usermodel.Row honRow = sheet.createRow(rowNum++);
            honRow.createCell(4).setCellValue("Honorários");
            org.apache.poi.ss.usermodel.Cell honValue = honRow.createCell(5);
            honValue.setCellValue(resultado.getValorHonorarios().doubleValue());
            honValue.setCellStyle(currencyStyle);
        }

        // Total Geral
        org.apache.poi.ss.usermodel.Row totalRow = sheet.createRow(rowNum);
        org.apache.poi.ss.usermodel.Cell totalLabel = totalRow.createCell(4);
        totalLabel.setCellValue("TOTAL GERAL");
        totalLabel.setCellStyle(headerStyle);
        org.apache.poi.ss.usermodel.Cell totalValue = totalRow.createCell(5);
        totalValue.setCellValue(resultado.getValorTotal().doubleValue());
        totalValue.setCellStyle(currencyStyle);
    }

    // =====================================================================
    //  PDF - SAVED AUXILIARES
    // =====================================================================

    private void adicionarCabecalhoPdf(Document document, Calculo calculo) throws DocumentException {
        com.lowagie.text.Font titleFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 18, com.lowagie.text.Font.BOLD, HEADER_BG);
        Paragraph title = new Paragraph("RELATÓRIO DE CÁLCULO", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(10);
        document.add(title);

        com.lowagie.text.Font subtitleFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 12, com.lowagie.text.Font.NORMAL, Color.GRAY);
        Paragraph subtitle = new Paragraph(calculo.getTitulo(), subtitleFont);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(20);
        document.add(subtitle);

        document.add(new Paragraph(" "));
    }

    private void adicionarDadosProcessoPdf(Document document, Calculo calculo) throws DocumentException {
        com.lowagie.text.Font sectionFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 12, com.lowagie.text.Font.BOLD, HEADER_BG);
        Paragraph section = new Paragraph("DADOS DO PROCESSO", sectionFont);
        section.setSpacingBefore(10);
        section.setSpacingAfter(10);
        document.add(section);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 2});

        addTableRow(table, "Número:", calculo.getProcesso().getNumeroProcesso() != null ?
            calculo.getProcesso().getNumeroProcesso() : "-");
        addTableRow(table, "Tipo de Ação:", calculo.getProcesso().getTipoAcao().toString());
        addTableRow(table, "Vara/Tribunal:", calculo.getProcesso().getVaraTribunal() != null ?
            calculo.getProcesso().getVaraTribunal() : "-");

        document.add(table);
        document.add(new Paragraph(" "));
    }

    private void adicionarParametrosPdf(Document document, Calculo calculo) throws DocumentException {
        com.lowagie.text.Font sectionFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 12, com.lowagie.text.Font.BOLD, HEADER_BG);
        Paragraph section = new Paragraph("PARÂMETROS DO CÁLCULO", sectionFont);
        section.setSpacingBefore(10);
        section.setSpacingAfter(10);
        document.add(section);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 2});

        if (calculo.getTipoCalculo() != null && calculo.getTipoCalculo() == TipoCalculo.FAZENDA_PUBLICA) {
            addTableRow(table, "Tipo de Cálculo:", "Condenação da Fazenda Pública");
        }
        addTableRow(table, "Valor Principal:", CURRENCY_FORMAT.format(calculo.getValorPrincipal()));
        addTableRow(table, "Data Inicial:", calculo.getDataInicial().format(DATE_FORMAT));
        addTableRow(table, "Data Final:", calculo.getDataFinal().format(DATE_FORMAT));
        if (calculo.getTipoCalculo() != null && calculo.getTipoCalculo() == TipoCalculo.FAZENDA_PUBLICA) {
            addTableRow(table, "Índice de Correção:", "Automático conforme legislação (INPC/IPCA-E/SELIC)");
            addTableRow(table, "Juros Moratórios:", "Conforme legislação vigente");
        } else {
            addTableRow(table, "Índice de Correção:", calculo.getTabelaIndice() != null ?
                calculo.getTabelaIndice().getNome() : "Sem correção");
            addTableRow(table, "Tipo de Juros:", calculo.getTipoJuros().toString());
            addTableRow(table, "Taxa de Juros:", calculo.getTaxaJuros() != null ?
                calculo.getTaxaJuros() + "% " + calculo.getPeriodicidadeJuros() : "-");
        }
        addTableRow(table, "Multa:", calculo.getMultaPercentual() + "%");
        addTableRow(table, "Honorários:", calculo.getHonorariosPercentual() + "%");

        document.add(table);
        document.add(new Paragraph(" "));
    }

    private void adicionarResultadoPdf(Document document, ResultadoCalculo resultado) throws DocumentException {
        com.lowagie.text.Font sectionFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 12, com.lowagie.text.Font.BOLD, HEADER_BG);
        Paragraph section = new Paragraph("RESULTADO", sectionFont);
        section.setSpacingBefore(10);
        section.setSpacingAfter(10);
        document.add(section);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 2});

        addTableRow(table, "Valor Corrigido:", CURRENCY_FORMAT.format(resultado.getValorCorrigido()));
        addTableRow(table, "Juros:", CURRENCY_FORMAT.format(resultado.getValorJuros()));
        addTableRow(table, "Multa:", CURRENCY_FORMAT.format(resultado.getValorMulta()));
        addTableRow(table, "Honorários:", CURRENCY_FORMAT.format(resultado.getValorHonorarios()));

        document.add(table);

        com.lowagie.text.Font totalFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 14, com.lowagie.text.Font.BOLD, GREEN_ACCENT);
        Paragraph total = new Paragraph("VALOR TOTAL: " + CURRENCY_FORMAT.format(resultado.getValorTotal()), totalFont);
        total.setAlignment(Element.ALIGN_RIGHT);
        total.setSpacingBefore(10);
        document.add(total);
        document.add(new Paragraph(" "));
    }

    // =====================================================================
    //  PDF - DETALHAMENTO (SALVO)
    // =====================================================================

    private void adicionarDetalhamentoPdf(Document document, Calculo calculo, ResultadoCalculo resultado) throws DocumentException {
        com.lowagie.text.Font sectionFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 12, com.lowagie.text.Font.BOLD, HEADER_BG);
        Paragraph section = new Paragraph("EVOLUÇÃO MENSAL", sectionFont);
        section.setSpacingBefore(15);
        section.setSpacingAfter(10);
        document.add(section);

        try {
            List<ResultadoCalculoResponse.DetalhamentoMensalResponse> detalhamento =
                objectMapper.readValue(resultado.getDetalhamentoJson(),
                    new TypeReference<List<ResultadoCalculoResponse.DetalhamentoMensalResponse>>() {});

            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.2f, 1.2f, 1f, 1.3f, 1f, 1.3f});

            addHeaderCell(table, "Competência");
            addHeaderCell(table, "Índice Aplicado");
            addHeaderCell(table, "Variação (%)");
            addHeaderCell(table, "Valor Corrigido");
            addHeaderCell(table, "Juros");
            addHeaderCell(table, "Subtotal");

            for (int i = 0; i < detalhamento.size(); i++) {
                ResultadoCalculoResponse.DetalhamentoMensalResponse det = detalhamento.get(i);
                Color bg = (i % 2 == 1) ? ZEBRA_BG : null;

                addCell(table, det.getCompetencia(), bg);
                addCell(table, det.getNomeIndice() != null ? det.getNomeIndice() : "-", bg);
                addCell(table, det.getVariacaoPercentual() != null ?
                    String.format("%.4f%%", det.getVariacaoPercentual()) : "-", bg);
                addCell(table, CURRENCY_FORMAT.format(det.getValorCorrigidoParcial()), bg);
                addCell(table, det.getJurosParcial() != null ?
                    CURRENCY_FORMAT.format(det.getJurosParcial()) : CURRENCY_FORMAT.format(BigDecimal.ZERO), bg);
                addCell(table, CURRENCY_FORMAT.format(det.getSubtotalParcial()), bg);
            }

            // Linhas de totais
            adicionarLinhaTotaisPdfSalvo(table, resultado);

            document.add(table);

        } catch (Exception e) {
            log.warn("Erro ao processar detalhamento: ", e);
        }
    }

    private void adicionarLinhaTotaisPdfSalvo(PdfPTable table, ResultadoCalculo resultado) {
        com.lowagie.text.Font boldFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 8, com.lowagie.text.Font.BOLD);

        // Subtotal row — label spans 4 cols, then Juros + Subtotal
        BigDecimal jurosTotal = resultado.getValorJuros() != null ? resultado.getValorJuros() : BigDecimal.ZERO;
        BigDecimal subtotal = resultado.getValorCorrigido().add(jurosTotal);

        PdfPCell labelCell = new PdfPCell(new Phrase("SUBTOTAL", boldFont));
        labelCell.setColspan(4);
        labelCell.setPadding(5);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        labelCell.setBackgroundColor(SUBTOTAL_BG);
        labelCell.setBorderWidth(0.5f);
        labelCell.setBorderColor(BORDER_COLOR);
        table.addCell(labelCell);

        addBoldCell(table, CURRENCY_FORMAT.format(jurosTotal));
        addBoldCell(table, CURRENCY_FORMAT.format(subtotal));

        // Multa
        if (resultado.getValorMulta() != null && resultado.getValorMulta().compareTo(BigDecimal.ZERO) > 0) {
            PdfPCell multaLabel = new PdfPCell(new Phrase("Multa", boldFont));
            multaLabel.setColspan(5);
            multaLabel.setPadding(5);
            multaLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
            multaLabel.setBackgroundColor(SUBTOTAL_BG);
            multaLabel.setBorderWidth(0.5f);
            multaLabel.setBorderColor(BORDER_COLOR);
            table.addCell(multaLabel);
            addBoldCell(table, CURRENCY_FORMAT.format(resultado.getValorMulta()));
        }

        // Honorários
        if (resultado.getValorHonorarios() != null && resultado.getValorHonorarios().compareTo(BigDecimal.ZERO) > 0) {
            PdfPCell honLabel = new PdfPCell(new Phrase("Honorários", boldFont));
            honLabel.setColspan(5);
            honLabel.setPadding(5);
            honLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
            honLabel.setBackgroundColor(SUBTOTAL_BG);
            honLabel.setBorderWidth(0.5f);
            honLabel.setBorderColor(BORDER_COLOR);
            table.addCell(honLabel);
            addBoldCell(table, CURRENCY_FORMAT.format(resultado.getValorHonorarios()));
        }

        // Total Geral
        com.lowagie.text.Font totalFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.BOLD, GREEN_ACCENT);
        PdfPCell totalLabel = new PdfPCell(new Phrase("TOTAL GERAL", totalFont));
        totalLabel.setColspan(5);
        totalLabel.setPadding(5);
        totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalLabel.setBackgroundColor(TOTAL_BG);
        totalLabel.setBorderWidth(0.5f);
        totalLabel.setBorderColor(BORDER_COLOR);
        table.addCell(totalLabel);

        PdfPCell totalCell = new PdfPCell(new Phrase(CURRENCY_FORMAT.format(resultado.getValorTotal()), totalFont));
        totalCell.setPadding(5);
        totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalCell.setBackgroundColor(TOTAL_BG);
        totalCell.setBorderWidth(0.5f);
        totalCell.setBorderColor(BORDER_COLOR);
        table.addCell(totalCell);
    }

    private void adicionarNotaLegislativaPdf(Document document) throws DocumentException {
        document.add(new Paragraph(" "));
        com.lowagie.text.Font notaTituloFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.BOLD, HEADER_BG);
        Paragraph notaTitulo = new Paragraph("FUNDAMENTAÇÃO LEGAL", notaTituloFont);
        notaTitulo.setSpacingBefore(5);
        notaTitulo.setSpacingAfter(5);
        document.add(notaTitulo);

        com.lowagie.text.Font notaSubFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 7, com.lowagie.text.Font.BOLD, HEADER_BG_DARK);
        Paragraph correcaoTitle = new Paragraph("Correção Monetária:", notaSubFont);
        correcaoTitle.setSpacingBefore(3);
        document.add(correcaoTitle);

        com.lowagie.text.Font notaFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 7, com.lowagie.text.Font.ITALIC, Color.GRAY);
        Paragraph correcao = new Paragraph(
            "• INPC (01/1984 a 12/1991) — Prática judicial consolidada, Lei 8.177/91\n" +
            "• IPCA-E (01/1992 a 08/12/2021) — Manual de Cálculos da Justiça Federal, Res. CJF 242/2001, Tema 810 STF\n" +
            "• SELIC taxa única (09/12/2021 a 09/2025) — EC 113/2021, art. 3º\n" +
            "• IPCA + 2% a.a. limitado à SELIC (10/2025 em diante) — EC 136/2025",
            notaFont
        );
        correcao.setSpacingBefore(2);
        document.add(correcao);

        Paragraph jurosTitle = new Paragraph("Juros Moratórios:", notaSubFont);
        jurosTitle.setSpacingBefore(5);
        document.add(jurosTitle);

        Paragraph juros = new Paragraph(
            "• 1% a.m. simples (até 06/2009) — Art. 406 CC/2002, Art. 161 §1º CTN\n" +
            "• 0,5% a.m. poupança (07/2009 a 08/12/2021) — Lei 11.960/2009\n" +
            "• SELIC (engloba correção + juros) (09/12/2021 a 09/2025) — EC 113/2021\n" +
            "• 2% a.a. limitado à SELIC (10/2025 em diante) — EC 136/2025",
            notaFont
        );
        juros.setSpacingBefore(2);
        document.add(juros);
        document.add(new Paragraph(" "));
    }

    private void adicionarRodapePdf(Document document) throws DocumentException {
        document.add(new Paragraph(" "));
        com.lowagie.text.Font footerFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 8, com.lowagie.text.Font.ITALIC, Color.GRAY);
        Paragraph footer = new Paragraph(
            "Relatório gerado pelo Sistema de Cálculos Jurídicos em " +
            java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
            footerFont
        );
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);
    }

    // =====================================================================
    //  PDF - HELPERS DE CÉLULA
    // =====================================================================

    private void addTableRow(PdfPTable table, String label, String value) {
        com.lowagie.text.Font labelFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.BOLD);
        com.lowagie.text.Font valueFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.NORMAL);

        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(5);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(5);
        table.addCell(valueCell);
    }

    private void addHeaderCell(PdfPTable table, String text) {
        com.lowagie.text.Font font = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 8, com.lowagie.text.Font.BOLD, Color.WHITE);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(HEADER_BG);
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBorderWidth(0.5f);
        cell.setBorderColor(HEADER_BG);
        table.addCell(cell);
    }

    private void addCell(PdfPTable table, String text) {
        addCell(table, text, null);
    }

    private void addCell(PdfPTable table, String text, Color bgColor) {
        com.lowagie.text.Font font = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 8, com.lowagie.text.Font.NORMAL);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setBorderWidth(0.5f);
        cell.setBorderColor(BORDER_COLOR);
        if (bgColor != null) {
            cell.setBackgroundColor(bgColor);
        }
        table.addCell(cell);
    }

    // =====================================================================
    //  EXCEL - SAVED
    // =====================================================================

    private void criarAbaResumo(Workbook workbook, Sheet sheet, Calculo calculo, ResultadoCalculo resultado) {
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);

        int rowNum = 0;

        org.apache.poi.ss.usermodel.Row titleRow = sheet.createRow(rowNum++);
        org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("RELATÓRIO DE CÁLCULO - " + calculo.getTitulo());
        titleCell.setCellStyle(headerStyle);

        rowNum++;

        createResultRow(sheet, rowNum++, "Valor Principal", calculo.getValorPrincipal(), currencyStyle);
        createResultRow(sheet, rowNum++, "Valor Corrigido", resultado.getValorCorrigido(), currencyStyle);
        createResultRow(sheet, rowNum++, "Juros", resultado.getValorJuros(), currencyStyle);
        createResultRow(sheet, rowNum++, "Multa", resultado.getValorMulta(), currencyStyle);
        createResultRow(sheet, rowNum++, "Honorários", resultado.getValorHonorarios(), currencyStyle);

        rowNum++;
        org.apache.poi.ss.usermodel.Row totalRow = sheet.createRow(rowNum);
        totalRow.createCell(0).setCellValue("VALOR TOTAL");
        org.apache.poi.ss.usermodel.Cell totalValueCell = totalRow.createCell(1);
        totalValueCell.setCellValue(resultado.getValorTotal().doubleValue());
        totalValueCell.setCellStyle(currencyStyle);

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void criarAbaParametros(Workbook workbook, Sheet sheet, Calculo calculo) {
        int rowNum = 0;
        boolean isFazenda = calculo.getTipoCalculo() != null && calculo.getTipoCalculo() == TipoCalculo.FAZENDA_PUBLICA;

        createParamRow(sheet, rowNum++, "Título", calculo.getTitulo());
        if (isFazenda) {
            createParamRow(sheet, rowNum++, "Tipo de Cálculo", "Condenação da Fazenda Pública");
        }
        createParamRow(sheet, rowNum++, "Data Inicial", calculo.getDataInicial().format(DATE_FORMAT));
        createParamRow(sheet, rowNum++, "Data Final", calculo.getDataFinal().format(DATE_FORMAT));
        if (isFazenda) {
            createParamRow(sheet, rowNum++, "Índice de Correção", "Automático conforme legislação (INPC/IPCA-E/SELIC)");
            createParamRow(sheet, rowNum++, "Juros Moratórios", "Conforme legislação vigente");
        } else {
            createParamRow(sheet, rowNum++, "Índice", calculo.getTabelaIndice() != null ?
                calculo.getTabelaIndice().getNome() : "Sem correção");
            createParamRow(sheet, rowNum++, "Tipo de Juros", calculo.getTipoJuros().toString());
            createParamRow(sheet, rowNum++, "Taxa de Juros", calculo.getTaxaJuros() != null ?
                calculo.getTaxaJuros() + "%" : "-");
            createParamRow(sheet, rowNum++, "Periodicidade", calculo.getPeriodicidadeJuros().toString());
        }
        createParamRow(sheet, rowNum++, "Multa", calculo.getMultaPercentual() + "%");
        createParamRow(sheet, rowNum++, "Honorários", calculo.getHonorariosPercentual() + "%");

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void criarAbaFundamentacaoLegal(Workbook workbook, Sheet sheet) {
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle boldStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        boldStyle.setFont(boldFont);

        int rowNum = 0;

        org.apache.poi.ss.usermodel.Row titleRow = sheet.createRow(rowNum++);
        org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("FUNDAMENTAÇÃO LEGAL");
        titleCell.setCellStyle(headerStyle);

        rowNum++;

        org.apache.poi.ss.usermodel.Row correcaoTitle = sheet.createRow(rowNum++);
        correcaoTitle.createCell(0).setCellValue("CORREÇÃO MONETÁRIA");
        correcaoTitle.getCell(0).setCellStyle(boldStyle);

        String[][] correcaoData = {
            {"01/1984 a 12/1991", "INPC", "Prática judicial consolidada, Lei 8.177/91"},
            {"01/1992 a 08/12/2021", "IPCA-E", "Manual de Cálculos da JF, Res. CJF 242/2001, Tema 810 STF"},
            {"09/12/2021 a 09/2025", "SELIC (taxa única)", "EC 113/2021, art. 3º"},
            {"10/2025 em diante", "IPCA + 2% a.a. limitado à SELIC", "EC 136/2025"}
        };

        // Cabeçalho tabela correção
        org.apache.poi.ss.usermodel.Row correcaoHeader = sheet.createRow(rowNum++);
        correcaoHeader.createCell(0).setCellValue("Período");
        correcaoHeader.getCell(0).setCellStyle(headerStyle);
        correcaoHeader.createCell(1).setCellValue("Índice");
        correcaoHeader.getCell(1).setCellStyle(headerStyle);
        correcaoHeader.createCell(2).setCellValue("Base Legal");
        correcaoHeader.getCell(2).setCellStyle(headerStyle);

        for (String[] row : correcaoData) {
            org.apache.poi.ss.usermodel.Row dataRow = sheet.createRow(rowNum++);
            dataRow.createCell(0).setCellValue(row[0]);
            dataRow.createCell(1).setCellValue(row[1]);
            dataRow.createCell(2).setCellValue(row[2]);
        }

        rowNum++;

        org.apache.poi.ss.usermodel.Row jurosTitle = sheet.createRow(rowNum++);
        jurosTitle.createCell(0).setCellValue("JUROS MORATÓRIOS");
        jurosTitle.getCell(0).setCellStyle(boldStyle);

        String[][] jurosData = {
            {"Até 06/2009", "1% a.m. simples", "Art. 406 CC/2002, Art. 161 §1º CTN"},
            {"07/2009 a 08/12/2021", "0,5% a.m. (poupança)", "Lei 11.960/2009"},
            {"09/12/2021 a 09/2025", "SELIC (engloba correção + juros)", "EC 113/2021"},
            {"10/2025 em diante", "2% a.a. limitado à SELIC", "EC 136/2025"}
        };

        org.apache.poi.ss.usermodel.Row jurosHeader = sheet.createRow(rowNum++);
        jurosHeader.createCell(0).setCellValue("Período");
        jurosHeader.getCell(0).setCellStyle(headerStyle);
        jurosHeader.createCell(1).setCellValue("Taxa");
        jurosHeader.getCell(1).setCellStyle(headerStyle);
        jurosHeader.createCell(2).setCellValue("Base Legal");
        jurosHeader.getCell(2).setCellStyle(headerStyle);

        for (String[] row : jurosData) {
            org.apache.poi.ss.usermodel.Row dataRow = sheet.createRow(rowNum++);
            dataRow.createCell(0).setCellValue(row[0]);
            dataRow.createCell(1).setCellValue(row[1]);
            dataRow.createCell(2).setCellValue(row[2]);
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
    }

    private void criarAbaDetalhamento(Workbook workbook, Sheet sheet, Calculo calculo, ResultadoCalculo resultado) {
        try {
            List<ResultadoCalculoResponse.DetalhamentoMensalResponse> detalhamento =
                objectMapper.readValue(resultado.getDetalhamentoJson(),
                    new TypeReference<List<ResultadoCalculoResponse.DetalhamentoMensalResponse>>() {});

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle percentStyle = createPercentStyle(workbook);

            // Zebra styles
            CellStyle zebraStyle = createZebraStyle(workbook);
            CellStyle zebraCurrencyStyle = createZebraCurrencyStyle(workbook);
            CellStyle zebraPercentStyle = createZebraPercentStyle(workbook);
            CellStyle borderStyle = createBorderStyle(workbook);
            CellStyle borderCurrencyStyle = createBorderCurrencyStyle(workbook);
            CellStyle borderPercentStyle = createBorderPercentStyle(workbook);

            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            String[] headers = {"Competência", "Índice Aplicado", "Variação (%)", "Valor Corrigido", "Juros", "Subtotal"};
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (int idx = 0; idx < detalhamento.size(); idx++) {
                ResultadoCalculoResponse.DetalhamentoMensalResponse det = detalhamento.get(idx);
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
                boolean isZebra = (idx % 2 == 1);

                org.apache.poi.ss.usermodel.Cell compCell = row.createCell(0);
                compCell.setCellValue(det.getCompetencia());
                compCell.setCellStyle(isZebra ? zebraStyle : borderStyle);

                org.apache.poi.ss.usermodel.Cell indiceCell = row.createCell(1);
                indiceCell.setCellValue(det.getNomeIndice() != null ? det.getNomeIndice() : "-");
                indiceCell.setCellStyle(isZebra ? zebraStyle : borderStyle);

                org.apache.poi.ss.usermodel.Cell variacaoCell = row.createCell(2);
                if (det.getVariacaoPercentual() != null) {
                    variacaoCell.setCellValue(det.getVariacaoPercentual().doubleValue());
                    variacaoCell.setCellStyle(isZebra ? zebraPercentStyle : borderPercentStyle);
                } else {
                    variacaoCell.setCellValue("-");
                    variacaoCell.setCellStyle(isZebra ? zebraStyle : borderStyle);
                }

                org.apache.poi.ss.usermodel.Cell corrigidoCell = row.createCell(3);
                corrigidoCell.setCellValue(det.getValorCorrigidoParcial() != null ? det.getValorCorrigidoParcial().doubleValue() : 0);
                corrigidoCell.setCellStyle(isZebra ? zebraCurrencyStyle : borderCurrencyStyle);

                org.apache.poi.ss.usermodel.Cell jurosCell = row.createCell(4);
                jurosCell.setCellValue(det.getJurosParcial() != null ? det.getJurosParcial().doubleValue() : 0);
                jurosCell.setCellStyle(isZebra ? zebraCurrencyStyle : borderCurrencyStyle);

                org.apache.poi.ss.usermodel.Cell subtotalCell = row.createCell(5);
                subtotalCell.setCellValue(det.getSubtotalParcial() != null ? det.getSubtotalParcial().doubleValue() : 0);
                subtotalCell.setCellStyle(isZebra ? zebraCurrencyStyle : borderCurrencyStyle);
            }

            // Linha de totais
            adicionarLinhaTotaisExcelSalvo(sheet, rowNum, resultado, currencyStyle, headerStyle);

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
        } catch (Exception e) {
            log.warn("Erro ao criar aba de detalhamento: ", e);
        }
    }

    private void adicionarLinhaTotaisExcelSalvo(Sheet sheet, int rowNum,
            ResultadoCalculo resultado, CellStyle currencyStyle, CellStyle headerStyle) {
        // Subtotal
        rowNum++;
        org.apache.poi.ss.usermodel.Row subtotalRow = sheet.createRow(rowNum++);
        org.apache.poi.ss.usermodel.Cell stLabel = subtotalRow.createCell(4);
        stLabel.setCellValue("SUBTOTAL");
        stLabel.setCellStyle(headerStyle);
        org.apache.poi.ss.usermodel.Cell stValue = subtotalRow.createCell(5);
        stValue.setCellValue(resultado.getValorCorrigido().add(resultado.getValorJuros()).doubleValue());
        stValue.setCellStyle(currencyStyle);

        // Multa
        if (resultado.getValorMulta() != null && resultado.getValorMulta().compareTo(BigDecimal.ZERO) > 0) {
            org.apache.poi.ss.usermodel.Row multaRow = sheet.createRow(rowNum++);
            multaRow.createCell(4).setCellValue("Multa");
            org.apache.poi.ss.usermodel.Cell multaValue = multaRow.createCell(5);
            multaValue.setCellValue(resultado.getValorMulta().doubleValue());
            multaValue.setCellStyle(currencyStyle);
        }

        // Honorários
        if (resultado.getValorHonorarios() != null && resultado.getValorHonorarios().compareTo(BigDecimal.ZERO) > 0) {
            org.apache.poi.ss.usermodel.Row honRow = sheet.createRow(rowNum++);
            honRow.createCell(4).setCellValue("Honorários");
            org.apache.poi.ss.usermodel.Cell honValue = honRow.createCell(5);
            honValue.setCellValue(resultado.getValorHonorarios().doubleValue());
            honValue.setCellStyle(currencyStyle);
        }

        // Total Geral
        org.apache.poi.ss.usermodel.Row totalRow = sheet.createRow(rowNum);
        org.apache.poi.ss.usermodel.Cell totalLabel = totalRow.createCell(4);
        totalLabel.setCellValue("TOTAL GERAL");
        totalLabel.setCellStyle(headerStyle);
        org.apache.poi.ss.usermodel.Cell totalValue = totalRow.createCell(5);
        totalValue.setCellValue(resultado.getValorTotal().doubleValue());
        totalValue.setCellStyle(currencyStyle);
    }

    // =====================================================================
    //  EXCEL - ESTILOS
    // =====================================================================

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("R$ #,##0.00"));
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createPercentStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("0.0000\"%\""));
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createBorderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createBorderCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("R$ #,##0.00"));
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createBorderPercentStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("0.0000\"%\""));
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createZebraStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createZebraCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("R$ #,##0.00"));
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createZebraPercentStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("0.0000\"%\""));
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private void createResultRow(Sheet sheet, int rowNum, String label, BigDecimal value, CellStyle currencyStyle) {
        org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(label);
        org.apache.poi.ss.usermodel.Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value.doubleValue());
        valueCell.setCellStyle(currencyStyle);
    }

    private void createParamRow(Sheet sheet, int rowNum, String label, String value) {
        org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value);
    }

    private String formatDecimal(BigDecimal value) {
        if (value == null) return "-";
        return String.format("%.6f", value);
    }
}
