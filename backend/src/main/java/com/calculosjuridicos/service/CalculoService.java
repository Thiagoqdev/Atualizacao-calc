package com.calculosjuridicos.service;

import com.calculosjuridicos.dto.request.CalculoRequest;
import com.calculosjuridicos.dto.response.ResultadoCalculoResponse;
import com.calculosjuridicos.dto.response.ResultadoCalculoResponse.*;
import com.calculosjuridicos.entity.*;
import com.calculosjuridicos.exception.BusinessException;
import com.calculosjuridicos.exception.ResourceNotFoundException;
import com.calculosjuridicos.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalculoService {

    private final CalculoRepository calculoRepository;
    private final ProcessoRepository processoRepository;
    private final TabelaIndiceRepository tabelaIndiceRepository;
    private final ResultadoCalculoRepository resultadoCalculoRepository;
    private final CorrecaoMonetariaService correcaoService;
    private final JurosService jurosService;
    private final FazendaPublicaCalculoService fazendaPublicaService;
    private final ObjectMapper objectMapper;

    private static final BigDecimal CEM = new BigDecimal("100");
    private static final DateTimeFormatter COMPETENCIA_FORMAT = DateTimeFormatter.ofPattern("MM - yyyy");

    /**
     * Executa um preview do cálculo sem persistir.
     */
    public ResultadoCalculoResponse preview(CalculoRequest request) {
        validarRequest(request);

        // Desviar para serviço especializado se for Fazenda Pública
        if (request.getTipoCalculo() == TipoCalculo.FAZENDA_PUBLICA) {
            return fazendaPublicaService.calcular(request);
        }

        return executarCalculo(request);
    }

    /**
     * Cria um novo cálculo associado a um processo.
     */
    @Transactional
    public Calculo criar(Long processoId, CalculoRequest request, Long usuarioId) {
        Processo processo = processoRepository.findByIdAndUsuarioId(processoId, usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Processo", "id", processoId));

        validarRequest(request);

        Calculo calculo = Calculo.builder()
            .processo(processo)
            .titulo(request.getTitulo() != null ? request.getTitulo() : "Cálculo " + LocalDateTime.now())
            .tipoCalculo(request.getTipoCalculo() != null ? request.getTipoCalculo() : TipoCalculo.PADRAO)
            .valorPrincipal(request.getValorPrincipal())
            .dataInicial(request.getDataInicial())
            .dataFinal(request.getDataFinal())
            .tipoJuros(request.getTipoJuros())
            .taxaJuros(request.getTaxaJuros())
            .periodicidadeJuros(request.getPeriodicidadeJuros())
            .multaPercentual(request.getMultaPercentual())
            .honorariosPercentual(request.getHonorariosPercentual())
            .jurosSobreCorrigido(request.getJurosSobreCorrigido())
            .status(StatusCalculo.RASCUNHO)
            .build();

        if (request.getTabelaIndiceId() != null) {
            TabelaIndice tabela = tabelaIndiceRepository.findById(request.getTabelaIndiceId())
                .orElseThrow(() -> new ResourceNotFoundException("TabelaIndice", "id", request.getTabelaIndiceId()));
            calculo.setTabelaIndice(tabela);
        }

        // Adicionar parcelas se fornecidas
        if (request.getParcelas() != null && !request.getParcelas().isEmpty()) {
            for (CalculoRequest.ParcelaRequest parcelaReq : request.getParcelas()) {
                ParcelaCalculo parcela = ParcelaCalculo.builder()
                    .descricao(parcelaReq.getDescricao())
                    .valorOriginal(parcelaReq.getValorOriginal())
                    .dataVencimento(parcelaReq.getDataVencimento())
                    .build();

                // Indice especifico da parcela (override do global)
                if (parcelaReq.getTabelaIndiceId() != null) {
                    TabelaIndice tabelaParcela = tabelaIndiceRepository.findById(parcelaReq.getTabelaIndiceId())
                        .orElseThrow(() -> new ResourceNotFoundException("TabelaIndice", "id", parcelaReq.getTabelaIndiceId()));
                    parcela.setTabelaIndice(tabelaParcela);
                }

                calculo.addParcela(parcela);
            }
        }

        return calculoRepository.save(calculo);
    }

    /**
     * Executa o cálculo e persiste o resultado.
     */
    @Transactional
    public ResultadoCalculoResponse executar(Long calculoId, Long usuarioId) {
        Calculo calculo = calculoRepository.findByIdAndUsuarioId(calculoId, usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Calculo", "id", calculoId));

        CalculoRequest request = toRequest(calculo);
        ResultadoCalculoResponse response;

        // Desviar para serviço especializado se for Fazenda Pública
        if (request.getTipoCalculo() == TipoCalculo.FAZENDA_PUBLICA) {
            response = fazendaPublicaService.calcular(request);
        } else {
            response = executarCalculo(request);
        }
        response.setCalculoId(calculoId);

        // Persistir resultado
        ResultadoCalculo resultado = ResultadoCalculo.builder()
            .calculo(calculo)
            .valorCorrigido(response.getValorCorrigido())
            .valorJuros(response.getValorJuros())
            .valorMulta(response.getValorMulta())
            .valorHonorarios(response.getValorHonorarios())
            .valorTotal(response.getValorTotal())
            .build();

        try {
            resultado.setDetalhamentoJson(objectMapper.writeValueAsString(response.getDetalhamento()));
        } catch (JsonProcessingException e) {
            log.warn("Erro ao serializar detalhamento: {}", e.getMessage());
        }

        // Remover resultado anterior se existir
        resultadoCalculoRepository.findByCalculoId(calculoId)
            .ifPresent(r -> resultadoCalculoRepository.delete(r));

        resultadoCalculoRepository.save(resultado);

        calculo.setStatus(StatusCalculo.CALCULADO);
        calculoRepository.save(calculo);

        return response;
    }

    /**
     * Busca um cálculo por ID.
     */
    @Transactional(readOnly = true)
    public Calculo buscarPorId(Long id, Long usuarioId) {
        return calculoRepository.findByIdAndUsuarioId(id, usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Calculo", "id", id));
    }

    /**
     * Lista cálculos do usuário.
     */
    @Transactional(readOnly = true)
    public Page<Calculo> listarPorUsuario(Long usuarioId, Pageable pageable) {
        return calculoRepository.findByUsuarioId(usuarioId, pageable);
    }

    /**
     * Lista cálculos de um processo.
     */
    @Transactional(readOnly = true)
    public Page<Calculo> listarPorProcesso(Long processoId, Pageable pageable) {
        return calculoRepository.findByProcessoId(processoId, pageable);
    }

    /**
     * Exclui um cálculo.
     */
    @Transactional
    public void excluir(Long id, Long usuarioId) {
        Calculo calculo = calculoRepository.findByIdAndUsuarioId(id, usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Calculo", "id", id));
        calculoRepository.delete(calculo);
    }

    // ============================================
    // Métodos privados de cálculo
    // ============================================

    private ResultadoCalculoResponse executarCalculo(CalculoRequest request) {
        List<ResultadoParcelaResponse> resultadosParcelas = new ArrayList<>();
        BigDecimal totalCorrigido = BigDecimal.ZERO;
        BigDecimal totalJuros = BigDecimal.ZERO;

        // Se não há parcelas, usar valor principal como parcela única
        List<CalculoRequest.ParcelaRequest> parcelas = request.getParcelas();
        if (parcelas == null || parcelas.isEmpty()) {
            parcelas = List.of(CalculoRequest.ParcelaRequest.builder()
                .descricao("Valor principal")
                .valorOriginal(request.getValorPrincipal())
                .dataVencimento(request.getDataInicial())
                .build());
        }

        // Cache de indices para evitar N+1 queries
        Map<Long, TabelaIndice> indiceCache = new HashMap<>();

        // Calcular cada parcela
        for (CalculoRequest.ParcelaRequest parcela : parcelas) {
            BigDecimal valorCorrigido;
            BigDecimal fatorCorrecao = BigDecimal.ONE;

            // Determinar indice efetivo: per-parcela override ou global
            Long effectiveIndiceId = parcela.getTabelaIndiceId() != null
                ? parcela.getTabelaIndiceId()
                : request.getTabelaIndiceId();

            // Resolver nome do indice para exibicao
            String indiceNome = null;
            if (effectiveIndiceId != null) {
                TabelaIndice tabela = indiceCache.computeIfAbsent(effectiveIndiceId,
                    id -> tabelaIndiceRepository.findById(id).orElse(null));
                if (tabela != null) {
                    indiceNome = tabela.getNome();
                }
            }

            // Correção monetária
            if (effectiveIndiceId != null) {
                valorCorrigido = correcaoService.calcular(
                    parcela.getValorOriginal(),
                    parcela.getDataVencimento(),
                    request.getDataFinal(),
                    effectiveIndiceId
                );
                fatorCorrecao = correcaoService.calcularFatorCorrecao(
                    parcela.getDataVencimento(),
                    request.getDataFinal(),
                    effectiveIndiceId
                );
            } else {
                valorCorrigido = parcela.getValorOriginal();
            }

            // Base para juros
            BigDecimal baseJuros = Boolean.TRUE.equals(request.getJurosSobreCorrigido())
                ? valorCorrigido
                : parcela.getValorOriginal();

            // Juros
            BigDecimal valorJuros = BigDecimal.ZERO;
            int mesesJuros = 0;
            if (request.getTaxaJuros() != null && request.getTaxaJuros().compareTo(BigDecimal.ZERO) > 0) {
                valorJuros = jurosService.calcular(
                    baseJuros,
                    request.getTaxaJuros(),
                    request.getTipoJuros(),
                    parcela.getDataVencimento(),
                    request.getDataFinal(),
                    request.getPeriodicidadeJuros()
                );
                mesesJuros = (int) ChronoUnit.MONTHS.between(parcela.getDataVencimento(), request.getDataFinal());
            }

            totalCorrigido = totalCorrigido.add(valorCorrigido);
            totalJuros = totalJuros.add(valorJuros);

            resultadosParcelas.add(ResultadoParcelaResponse.builder()
                .descricao(parcela.getDescricao())
                .valorOriginal(parcela.getValorOriginal())
                .dataVencimento(parcela.getDataVencimento())
                .valorCorrigido(valorCorrigido)
                .valorJuros(valorJuros)
                .subtotal(valorCorrigido.add(valorJuros))
                .mesesJuros(mesesJuros)
                .indiceNome(indiceNome)
                .build());
        }

        // Subtotal
        BigDecimal subtotal = totalCorrigido.add(totalJuros);

        // Multa
        BigDecimal valorMulta = BigDecimal.ZERO;
        if (request.getMultaPercentual() != null && request.getMultaPercentual().compareTo(BigDecimal.ZERO) > 0) {
            valorMulta = subtotal.multiply(request.getMultaPercentual())
                .divide(CEM, 2, RoundingMode.HALF_UP);
        }

        // Honorários
        BigDecimal baseHonorarios = subtotal.add(valorMulta);
        BigDecimal valorHonorarios = BigDecimal.ZERO;
        if (request.getHonorariosPercentual() != null && request.getHonorariosPercentual().compareTo(BigDecimal.ZERO) > 0) {
            valorHonorarios = baseHonorarios.multiply(request.getHonorariosPercentual())
                .divide(CEM, 2, RoundingMode.HALF_UP);
        }

        // Total final
        BigDecimal valorTotal = subtotal.add(valorMulta).add(valorHonorarios);

        // Fator de correção geral (para exibição)
        BigDecimal fatorCorrecaoGeral = BigDecimal.ONE;
        if (request.getTabelaIndiceId() != null && request.getValorPrincipal().compareTo(BigDecimal.ZERO) > 0) {
            fatorCorrecaoGeral = totalCorrigido.divide(request.getValorPrincipal(), 6, RoundingMode.HALF_UP);
        }

        // Gerar detalhamento mensal
        List<DetalhamentoMensalResponse> detalhamento = gerarDetalhamentoMensal(request);

        // Calcular variação total do período
        BigDecimal variacaoTotalPeriodo = null;
        if (!detalhamento.isEmpty()) {
            BigDecimal primeiroIndice = detalhamento.get(0).getIndice();
            BigDecimal ultimoIndice = detalhamento.get(detalhamento.size() - 1).getIndice();
            if (primeiroIndice != null && ultimoIndice != null && primeiroIndice.compareTo(BigDecimal.ZERO) > 0) {
                variacaoTotalPeriodo = ultimoIndice.subtract(primeiroIndice)
                    .divide(primeiroIndice, 6, RoundingMode.HALF_UP)
                    .multiply(CEM)
                    .setScale(4, RoundingMode.HALF_UP);
            }
        }

        return ResultadoCalculoResponse.builder()
            .valorOriginal(request.getValorPrincipal())
            .valorCorrigido(totalCorrigido)
            .valorJuros(totalJuros)
            .valorMulta(valorMulta)
            .valorHonorarios(valorHonorarios)
            .valorTotal(valorTotal)
            .fatorCorrecao(fatorCorrecaoGeral)
            .variacaoTotalPeriodo(variacaoTotalPeriodo)
            .dataCalculo(LocalDateTime.now())
            .parcelas(resultadosParcelas)
            .detalhamento(detalhamento)
            .build();
    }

    private List<DetalhamentoMensalResponse> gerarDetalhamentoMensal(CalculoRequest request) {
        List<DetalhamentoMensalResponse> detalhamento = new ArrayList<>();

        if (request.getTabelaIndiceId() == null) {
            return detalhamento;
        }

        // Resolver nome do índice para exibição
        String nomeIndice = null;
        TabelaIndice tabela = tabelaIndiceRepository.findById(request.getTabelaIndiceId()).orElse(null);
        if (tabela != null) {
            nomeIndice = tabela.getNome();
        }

        List<ValorIndice> indices = correcaoService.obterIndicesNoPeriodo(
            request.getTabelaIndiceId(),
            request.getDataInicial(),
            request.getDataFinal()
        );

        if (indices.isEmpty()) {
            return detalhamento;
        }

        BigDecimal indiceBase = indices.get(0).getValor();
        BigDecimal valorOriginal = request.getValorPrincipal();
        BigDecimal fatorAcumulado = BigDecimal.ONE;
        BigDecimal indiceAnterior = null;

        for (ValorIndice indice : indices) {
            fatorAcumulado = indice.getValor().divide(indiceBase, 6, RoundingMode.HALF_UP);
            BigDecimal valorCorrigidoParcial = valorOriginal.multiply(fatorAcumulado).setScale(2, RoundingMode.HALF_UP);

            // Variação percentual mês a mês
            BigDecimal variacaoPercentual = null;
            if (indiceAnterior != null && indiceAnterior.compareTo(BigDecimal.ZERO) > 0) {
                variacaoPercentual = indice.getValor().subtract(indiceAnterior)
                    .divide(indiceAnterior, 6, RoundingMode.HALF_UP)
                    .multiply(CEM)
                    .setScale(4, RoundingMode.HALF_UP);
            }
            indiceAnterior = indice.getValor();

            // Juros acumulados até este mês
            long meses = ChronoUnit.MONTHS.between(request.getDataInicial(), indice.getCompetencia().plusMonths(1));
            BigDecimal jurosParcial = BigDecimal.ZERO;
            if (request.getTaxaJuros() != null && request.getTaxaJuros().compareTo(BigDecimal.ZERO) > 0 && meses > 0) {
                BigDecimal baseJuros = Boolean.TRUE.equals(request.getJurosSobreCorrigido())
                    ? valorCorrigidoParcial : valorOriginal;
                if (request.getTipoJuros() == TipoJuros.SIMPLES) {
                    jurosParcial = baseJuros
                        .multiply(request.getTaxaJuros().divide(CEM, 10, RoundingMode.HALF_UP))
                        .multiply(new BigDecimal(meses))
                        .setScale(2, RoundingMode.HALF_UP);
                }
            }

            detalhamento.add(DetalhamentoMensalResponse.builder()
                .competencia(indice.getCompetencia().format(COMPETENCIA_FORMAT))
                .nomeIndice(nomeIndice)
                .indice(indice.getValor())
                .variacaoPercentual(variacaoPercentual)
                .fatorAcumulado(fatorAcumulado)
                .valorCorrigidoParcial(valorCorrigidoParcial)
                .jurosParcial(jurosParcial)
                .subtotalParcial(valorCorrigidoParcial.add(jurosParcial))
                .build());
        }

        return detalhamento;
    }

    private void validarRequest(CalculoRequest request) {
        if (request.getDataInicial().isAfter(request.getDataFinal())) {
            throw new BusinessException("Data inicial não pode ser posterior à data final");
        }

        if (request.getParcelas() != null) {
            for (CalculoRequest.ParcelaRequest parcela : request.getParcelas()) {
                if (parcela.getDataVencimento().isAfter(request.getDataFinal())) {
                    throw new BusinessException("Data de vencimento da parcela não pode ser posterior à data final do cálculo");
                }
            }
        }
    }

    private CalculoRequest toRequest(Calculo calculo) {
        List<CalculoRequest.ParcelaRequest> parcelas = null;
        if (calculo.getParcelas() != null && !calculo.getParcelas().isEmpty()) {
            parcelas = calculo.getParcelas().stream()
                .map(p -> CalculoRequest.ParcelaRequest.builder()
                    .descricao(p.getDescricao())
                    .valorOriginal(p.getValorOriginal())
                    .dataVencimento(p.getDataVencimento())
                    .tabelaIndiceId(p.getTabelaIndice() != null ? p.getTabelaIndice().getId() : null)
                    .build())
                .toList();
        }

        return CalculoRequest.builder()
            .titulo(calculo.getTitulo())
            .tipoCalculo(calculo.getTipoCalculo())
            .valorPrincipal(calculo.getValorPrincipal())
            .dataInicial(calculo.getDataInicial())
            .dataFinal(calculo.getDataFinal())
            .tabelaIndiceId(calculo.getTabelaIndice() != null ? calculo.getTabelaIndice().getId() : null)
            .tipoJuros(calculo.getTipoJuros())
            .taxaJuros(calculo.getTaxaJuros())
            .periodicidadeJuros(calculo.getPeriodicidadeJuros())
            .multaPercentual(calculo.getMultaPercentual())
            .honorariosPercentual(calculo.getHonorariosPercentual())
            .jurosSobreCorrigido(calculo.getJurosSobreCorrigido())
            .parcelas(parcelas)
            .build();
    }
}
