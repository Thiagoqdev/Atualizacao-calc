package com.calculosjuridicos.service;

import com.calculosjuridicos.dto.response.ResultadoCalculoResponse;
import com.calculosjuridicos.entity.Calculo;
import com.calculosjuridicos.entity.ResultadoCalculo;
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
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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

            if (calculo.getProcesso() != null) {
                adicionarDadosProcessoPdf(document, calculo);
            }

            adicionarParametrosPdf(document, calculo);
            adicionarResultadoPdf(document, resultado);

            if ("completo".equals(nivel) && resultado.getDetalhamentoJson() != null) {
                adicionarDetalhamentoPdf(document, resultado);
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

            Sheet resumoSheet = workbook.createSheet("Resumo");
            criarAbaResumo(workbook, resumoSheet, calculo, resultado);

            Sheet parametrosSheet = workbook.createSheet("Parâmetros");
            criarAbaParametros(workbook, parametrosSheet, calculo);

            if ("completo".equals(nivel) && resultado.getDetalhamentoJson() != null) {
                Sheet detalhamentoSheet = workbook.createSheet("Evolução Mensal");
                criarAbaDetalhamento(workbook, detalhamentoSheet, resultado);
            }

            workbook.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Erro ao gerar Excel: ", e);
            throw new BusinessException("Erro ao gerar relatório Excel");
        }
    }

    // ========== Métodos auxiliares PDF ==========

    private void adicionarCabecalhoPdf(Document document, Calculo calculo) throws DocumentException {
        com.lowagie.text.Font titleFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 18, com.lowagie.text.Font.BOLD, new Color(44, 62, 80));
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
        com.lowagie.text.Font sectionFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 12, com.lowagie.text.Font.BOLD);
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
        com.lowagie.text.Font sectionFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 12, com.lowagie.text.Font.BOLD);
        Paragraph section = new Paragraph("PARÂMETROS DO CÁLCULO", sectionFont);
        section.setSpacingBefore(10);
        section.setSpacingAfter(10);
        document.add(section);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 2});

        addTableRow(table, "Valor Principal:", CURRENCY_FORMAT.format(calculo.getValorPrincipal()));
        addTableRow(table, "Data Inicial:", calculo.getDataInicial().format(DATE_FORMAT));
        addTableRow(table, "Data Final:", calculo.getDataFinal().format(DATE_FORMAT));
        addTableRow(table, "Índice de Correção:", calculo.getTabelaIndice() != null ?
            calculo.getTabelaIndice().getNome() : "Sem correção");
        addTableRow(table, "Tipo de Juros:", calculo.getTipoJuros().toString());
        addTableRow(table, "Taxa de Juros:", calculo.getTaxaJuros() != null ?
            calculo.getTaxaJuros() + "% " + calculo.getPeriodicidadeJuros() : "-");
        addTableRow(table, "Multa:", calculo.getMultaPercentual() + "%");
        addTableRow(table, "Honorários:", calculo.getHonorariosPercentual() + "%");

        document.add(table);
        document.add(new Paragraph(" "));
    }

    private void adicionarResultadoPdf(Document document, ResultadoCalculo resultado) throws DocumentException {
        com.lowagie.text.Font sectionFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 12, com.lowagie.text.Font.BOLD);
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

        com.lowagie.text.Font totalFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 14, com.lowagie.text.Font.BOLD, new Color(39, 174, 96));
        Paragraph total = new Paragraph("VALOR TOTAL: " + CURRENCY_FORMAT.format(resultado.getValorTotal()), totalFont);
        total.setAlignment(Element.ALIGN_RIGHT);
        total.setSpacingBefore(10);
        document.add(total);
        document.add(new Paragraph(" "));
    }

    private void adicionarDetalhamentoPdf(Document document, ResultadoCalculo resultado) throws DocumentException {
        com.lowagie.text.Font sectionFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 12, com.lowagie.text.Font.BOLD);
        Paragraph section = new Paragraph("EVOLUÇÃO MENSAL", sectionFont);
        section.setSpacingBefore(10);
        section.setSpacingAfter(10);
        document.add(section);

        try {
            List<ResultadoCalculoResponse.DetalhamentoMensalResponse> detalhamento =
                objectMapper.readValue(resultado.getDetalhamentoJson(),
                    new TypeReference<List<ResultadoCalculoResponse.DetalhamentoMensalResponse>>() {});

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1, 1.2f, 1.2f, 1.2f, 1.2f});

            addHeaderCell(table, "Competência");
            addHeaderCell(table, "Índice");
            addHeaderCell(table, "Fator");
            addHeaderCell(table, "Corrigido");
            addHeaderCell(table, "Subtotal");

            for (ResultadoCalculoResponse.DetalhamentoMensalResponse det : detalhamento) {
                addCell(table, det.getCompetencia());
                addCell(table, formatDecimal(det.getIndice()));
                addCell(table, formatDecimal(det.getFatorAcumulado()));
                addCell(table, CURRENCY_FORMAT.format(det.getValorCorrigidoParcial()));
                addCell(table, CURRENCY_FORMAT.format(det.getSubtotalParcial()));
            }

            document.add(table);
        } catch (Exception e) {
            log.warn("Erro ao processar detalhamento: ", e);
        }
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
        com.lowagie.text.Font font = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.BOLD, Color.WHITE);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(44, 62, 80));
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addCell(PdfPTable table, String text) {
        com.lowagie.text.Font font = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 8, com.lowagie.text.Font.NORMAL);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(4);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(cell);
    }

    // ========== Métodos auxiliares Excel ==========

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

        createParamRow(sheet, rowNum++, "Título", calculo.getTitulo());
        createParamRow(sheet, rowNum++, "Data Inicial", calculo.getDataInicial().format(DATE_FORMAT));
        createParamRow(sheet, rowNum++, "Data Final", calculo.getDataFinal().format(DATE_FORMAT));
        createParamRow(sheet, rowNum++, "Índice", calculo.getTabelaIndice() != null ?
            calculo.getTabelaIndice().getNome() : "Sem correção");
        createParamRow(sheet, rowNum++, "Tipo de Juros", calculo.getTipoJuros().toString());
        createParamRow(sheet, rowNum++, "Taxa de Juros", calculo.getTaxaJuros() != null ?
            calculo.getTaxaJuros() + "%" : "-");
        createParamRow(sheet, rowNum++, "Periodicidade", calculo.getPeriodicidadeJuros().toString());
        createParamRow(sheet, rowNum++, "Multa", calculo.getMultaPercentual() + "%");
        createParamRow(sheet, rowNum++, "Honorários", calculo.getHonorariosPercentual() + "%");

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void criarAbaDetalhamento(Workbook workbook, Sheet sheet, ResultadoCalculo resultado) {
        try {
            List<ResultadoCalculoResponse.DetalhamentoMensalResponse> detalhamento =
                objectMapper.readValue(resultado.getDetalhamentoJson(),
                    new TypeReference<List<ResultadoCalculoResponse.DetalhamentoMensalResponse>>() {});

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);

            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            String[] headers = {"Competência", "Índice", "Fator Acumulado", "Valor Corrigido", "Juros", "Subtotal"};
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (ResultadoCalculoResponse.DetalhamentoMensalResponse det : detalhamento) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(det.getCompetencia());
                row.createCell(1).setCellValue(det.getIndice() != null ? det.getIndice().doubleValue() : 0);
                row.createCell(2).setCellValue(det.getFatorAcumulado() != null ? det.getFatorAcumulado().doubleValue() : 0);

                org.apache.poi.ss.usermodel.Cell corrigidoCell = row.createCell(3);
                corrigidoCell.setCellValue(det.getValorCorrigidoParcial() != null ? det.getValorCorrigidoParcial().doubleValue() : 0);
                corrigidoCell.setCellStyle(currencyStyle);

                org.apache.poi.ss.usermodel.Cell jurosCell = row.createCell(4);
                jurosCell.setCellValue(det.getJurosParcial() != null ? det.getJurosParcial().doubleValue() : 0);
                jurosCell.setCellStyle(currencyStyle);

                org.apache.poi.ss.usermodel.Cell subtotalCell = row.createCell(5);
                subtotalCell.setCellValue(det.getSubtotalParcial() != null ? det.getSubtotalParcial().doubleValue() : 0);
                subtotalCell.setCellStyle(currencyStyle);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
        } catch (Exception e) {
            log.warn("Erro ao criar aba de detalhamento: ", e);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("R$ #,##0.00"));
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
