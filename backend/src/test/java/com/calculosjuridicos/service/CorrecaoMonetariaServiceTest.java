package com.calculosjuridicos.service;

import com.calculosjuridicos.entity.TabelaIndice;
import com.calculosjuridicos.entity.ValorIndice;
import com.calculosjuridicos.exception.BusinessException;
import com.calculosjuridicos.repository.ValorIndiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CorrecaoMonetariaServiceTest {

    @Mock
    private ValorIndiceRepository valorIndiceRepository;

    @InjectMocks
    private CorrecaoMonetariaService correcaoService;

    private TabelaIndice tabelaIndice;

    @BeforeEach
    void setUp() {
        tabelaIndice = TabelaIndice.builder()
            .id(1L)
            .nome("IPCA_E")
            .build();
    }

    @Test
    @DisplayName("Deve calcular correção monetária com índices válidos")
    void deveCalcularCorrecaoMonetariaComIndicesValidos() {
        // Arrange
        BigDecimal valorOriginal = new BigDecimal("10000.00");
        LocalDate dataInicial = LocalDate.of(2020, 1, 15);
        LocalDate dataFinal = LocalDate.of(2024, 1, 15);

        // Índice inicial (dezembro/2019) = 100
        ValorIndice indiceInicial = ValorIndice.builder()
            .id(1L)
            .tabelaIndice(tabelaIndice)
            .competencia(LocalDate.of(2019, 12, 1))
            .valor(new BigDecimal("100.00000000"))
            .build();

        // Índice final (janeiro/2024) = 125
        ValorIndice indiceFinal = ValorIndice.builder()
            .id(2L)
            .tabelaIndice(tabelaIndice)
            .competencia(LocalDate.of(2024, 1, 1))
            .valor(new BigDecimal("125.00000000"))
            .build();

        when(valorIndiceRepository.findByTabelaIndiceIdAndCompetenciaLessThanEqual(
            eq(1L), eq(LocalDate.of(2019, 12, 1))))
            .thenReturn(Optional.of(indiceInicial));

        when(valorIndiceRepository.findByTabelaIndiceIdAndCompetenciaLessThanEqual(
            eq(1L), eq(LocalDate.of(2024, 1, 1))))
            .thenReturn(Optional.of(indiceFinal));

        // Act
        BigDecimal resultado = correcaoService.calcular(valorOriginal, dataInicial, dataFinal, 1L);

        // Assert - Fator = 125/100 = 1.25, Valor = 10000 * 1.25 = 12500
        assertEquals(new BigDecimal("12500.00"), resultado);
    }

    @Test
    @DisplayName("Deve retornar zero para valor original zero")
    void deveRetornarZeroParaValorOriginalZero() {
        BigDecimal resultado = correcaoService.calcular(
            BigDecimal.ZERO,
            LocalDate.of(2020, 1, 1),
            LocalDate.of(2024, 1, 1),
            1L
        );

        assertEquals(BigDecimal.ZERO, resultado);
    }

    @Test
    @DisplayName("Deve retornar zero para valor original nulo")
    void deveRetornarZeroParaValorOriginalNulo() {
        BigDecimal resultado = correcaoService.calcular(
            null,
            LocalDate.of(2020, 1, 1),
            LocalDate.of(2024, 1, 1),
            1L
        );

        assertEquals(BigDecimal.ZERO, resultado);
    }

    @Test
    @DisplayName("Deve lançar exceção quando data inicial é posterior à data final")
    void deveLancarExcecaoQuandoDataInicialPosteriorDataFinal() {
        assertThrows(BusinessException.class, () ->
            correcaoService.calcular(
                new BigDecimal("10000.00"),
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2020, 1, 1),
                1L
            )
        );
    }

    @Test
    @DisplayName("Deve lançar exceção quando índice inicial não encontrado")
    void deveLancarExcecaoQuandoIndiceInicialNaoEncontrado() {
        when(valorIndiceRepository.findByTabelaIndiceIdAndCompetenciaLessThanEqual(
            any(), any()))
            .thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () ->
            correcaoService.calcular(
                new BigDecimal("10000.00"),
                LocalDate.of(2020, 1, 1),
                LocalDate.of(2024, 1, 1),
                1L
            )
        );
    }

    @Test
    @DisplayName("Deve calcular com índices usando BigDecimal precisamente")
    void deveCalcularComIndicesUsandoBigDecimalPrecisamente() {
        // Arrange
        BigDecimal valorOriginal = new BigDecimal("1000.00");

        ValorIndice indiceInicial = ValorIndice.builder()
            .valor(new BigDecimal("4532.52000000"))
            .build();

        ValorIndice indiceFinal = ValorIndice.builder()
            .valor(new BigDecimal("5678.25000000"))
            .build();

        when(valorIndiceRepository.findByTabelaIndiceIdAndCompetenciaLessThanEqual(
            eq(1L), any()))
            .thenReturn(Optional.of(indiceInicial))
            .thenReturn(Optional.of(indiceFinal));

        // Act
        BigDecimal resultado = correcaoService.calcular(
            valorOriginal,
            LocalDate.of(2020, 1, 1),
            LocalDate.of(2024, 1, 1),
            1L
        );

        // Assert - Fator = 5678.25 / 4532.52 = 1.252792...
        // Valor = 1000 * 1.252792 = 1252.79 (arredondado para 2 casas)
        assertTrue(resultado.compareTo(new BigDecimal("1252.00")) > 0);
        assertTrue(resultado.compareTo(new BigDecimal("1253.00")) < 0);
    }

    @Test
    @DisplayName("Deve calcular fator de correção corretamente")
    void deveCalcularFatorCorrecaoCorretamente() {
        // Arrange
        ValorIndice indiceInicial = ValorIndice.builder()
            .valor(new BigDecimal("100.00000000"))
            .build();

        ValorIndice indiceFinal = ValorIndice.builder()
            .valor(new BigDecimal("150.00000000"))
            .build();

        when(valorIndiceRepository.findByTabelaIndiceIdAndCompetenciaLessThanEqual(
            eq(1L), any()))
            .thenReturn(Optional.of(indiceInicial))
            .thenReturn(Optional.of(indiceFinal));

        // Act
        BigDecimal fator = correcaoService.calcularFatorCorrecao(
            LocalDate.of(2020, 1, 1),
            LocalDate.of(2024, 1, 1),
            1L
        );

        // Assert - Fator = 150/100 = 1.5
        assertEquals(0, fator.compareTo(new BigDecimal("1.5")));
    }
}
