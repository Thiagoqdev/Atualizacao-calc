package com.calculosjuridicos.service;

import com.calculosjuridicos.dto.request.CalculoRequest;
import com.calculosjuridicos.dto.response.ResultadoCalculoResponse;
import com.calculosjuridicos.entity.TabelaIndice;
import com.calculosjuridicos.entity.ValorIndice;
import com.calculosjuridicos.repository.TabelaIndiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FazendaPublicaCalculoServiceTest {

    @Mock
    private CorrecaoMonetariaService correcaoService;

    @Mock
    private TabelaIndiceRepository tabelaIndiceRepository;

    private FazendaPublicaCalculoService service;

    @BeforeEach
    void setUp() {
        service = new FazendaPublicaCalculoService(correcaoService, tabelaIndiceRepository);

        when(tabelaIndiceRepository.findByNome(TabelaIndice.INPC))
            .thenReturn(Optional.of(TabelaIndice.builder().id(1L).nome(TabelaIndice.INPC).build()));
        when(tabelaIndiceRepository.findByNome(TabelaIndice.IPCA_E))
            .thenReturn(Optional.of(TabelaIndice.builder().id(2L).nome(TabelaIndice.IPCA_E).build()));
        when(tabelaIndiceRepository.findByNome(TabelaIndice.SELIC))
            .thenReturn(Optional.of(TabelaIndice.builder().id(3L).nome(TabelaIndice.SELIC).build()));

        when(correcaoService.obterIndicesNoPeriodo(anyLong(), any(LocalDate.class), any(LocalDate.class)))
            .thenAnswer(invocation -> {
                Long indiceId = invocation.getArgument(0);
                LocalDate inicio = invocation.getArgument(1);
                LocalDate fim = invocation.getArgument(2);
                if (!inicio.equals(fim)) {
                    return List.of();
                }

                BigDecimal valor = mockValorIndice(indiceId, inicio);
                if (valor == null) {
                    return List.of();
                }

                return List.of(ValorIndice.builder()
                    .competencia(inicio)
                    .valor(valor)
                    .build());
            });
    }

    @Test
    @DisplayName("Deve carregar subtotal do mês anterior na entrada da SELIC unificada em 12/2021")
    void deveCarregarSubtotalAnteriorNaEntradaDaSelicUnificada() {
        when(correcaoService.calcular(any(), any(), any(), anyLong())).thenReturn(null);

        ResultadoCalculoResponse resultado = service.calcular(criarRequestTransicao());

        ResultadoCalculoResponse.DetalhamentoMensalResponse novembro2021 = resultado.getDetalhamento().stream()
            .filter(item -> "11 - 2021".equals(item.getCompetencia()))
            .findFirst()
            .orElse(null);
        ResultadoCalculoResponse.DetalhamentoMensalResponse dezembro2021 = resultado.getDetalhamento().stream()
            .filter(item -> "12 - 2021".equals(item.getCompetencia()))
            .findFirst()
            .orElse(null);

        assertNotNull(novembro2021);
        assertNotNull(dezembro2021);
        assertEquals("SELIC", dezembro2021.getNomeIndice());
        assertEquals(0, novembro2021.getSubtotalParcial().compareTo(dezembro2021.getValorCorrigidoParcial()));
    }

    @Test
    @DisplayName("Deve manter continuidade na transição para EC 136 usando índice do mês anterior da mesma série")
    void deveManterContinuidadeNaTransicaoDeIndiceComGap() {
        when(correcaoService.calcular(any(), any(), any(), anyLong())).thenReturn(null);

        ResultadoCalculoResponse resultado = service.calcular(criarRequestTransicao());

        ResultadoCalculoResponse.DetalhamentoMensalResponse outubro2025 = resultado.getDetalhamento().stream()
            .filter(item -> "10 - 2025".equals(item.getCompetencia()))
            .findFirst()
            .orElse(null);

        assertNotNull(outubro2025);
        assertEquals(0, outubro2025.getValorCorrigidoParcial().compareTo(new BigDecimal("1012.73")));
        assertEquals(0, outubro2025.getVariacaoPercentual().compareTo(new BigDecimal("0.7692")));

        verify(correcaoService, atLeastOnce()).obterIndicesNoPeriodo(
            eq(2L),
            eq(LocalDate.of(2025, 9, 1)),
            eq(LocalDate.of(2025, 9, 1))
        );
    }

    @Test
    @DisplayName("Deve exibir nome de índice IPCA + 2% quando teto da SELIC for aplicado")
    void deveExibirNomeIpcaMaisDoisQuandoTetoAplicado() {
        when(correcaoService.calcular(any(), any(), any(), eq(3L)))
            .thenReturn(new BigDecimal("900.00"));

        ResultadoCalculoResponse resultado = service.calcular(criarRequestTransicao());

        ResultadoCalculoResponse.DetalhamentoMensalResponse outubro2025 = resultado.getDetalhamento().stream()
            .filter(item -> "10 - 2025".equals(item.getCompetencia()))
            .findFirst()
            .orElse(null);

        assertNotNull(outubro2025);
        assertEquals("IPCA + 2%", outubro2025.getNomeIndice());
        assertTrue(outubro2025.getSubtotalParcial().compareTo(new BigDecimal("900.00")) >= 0);
    }

    private CalculoRequest criarRequestTransicao() {
        return CalculoRequest.builder()
            .valorPrincipal(new BigDecimal("1000.00"))
            .dataInicial(LocalDate.of(2021, 11, 1))
            .dataFinal(LocalDate.of(2025, 10, 31))
            .multaPercentual(BigDecimal.ZERO)
            .honorariosPercentual(BigDecimal.ZERO)
            .build();
    }

    private BigDecimal mockValorIndice(Long indiceId, LocalDate competencia) {
        if (indiceId == null || competencia == null) {
            return null;
        }

        if (indiceId.equals(2L)) { // IPCA-E
            if (competencia.equals(LocalDate.of(2021, 11, 1))) {
                return new BigDecimal("100.00000000");
            }
            if (competencia.equals(LocalDate.of(2025, 9, 1))) {
                return new BigDecimal("130.00000000");
            }
            if (competencia.equals(LocalDate.of(2025, 10, 1))) {
                return new BigDecimal("131.00000000");
            }
            return null;
        }

        if (indiceId.equals(3L)) { // SELIC
            if (!competencia.isBefore(LocalDate.of(2021, 12, 1))
                && !competencia.isAfter(LocalDate.of(2025, 9, 1))) {
                return new BigDecimal("200.00000000");
            }
            return null;
        }

        return null;
    }
}
