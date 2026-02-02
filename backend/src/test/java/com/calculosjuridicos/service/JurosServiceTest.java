package com.calculosjuridicos.service;

import com.calculosjuridicos.entity.PeriodicidadeJuros;
import com.calculosjuridicos.entity.TipoJuros;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class JurosServiceTest {

    private JurosService jurosService;

    @BeforeEach
    void setUp() {
        jurosService = new JurosService();
    }

    // ==================== Testes de Juros Simples ====================

    @Test
    @DisplayName("Deve calcular juros simples mensais corretamente")
    void deveCalcularJurosSimplesMensaisCorretamente() {
        // Arrange
        BigDecimal principal = new BigDecimal("10000.00");
        BigDecimal taxa = new BigDecimal("1.0"); // 1% ao mês
        LocalDate dataInicial = LocalDate.of(2023, 1, 1);
        LocalDate dataFinal = LocalDate.of(2024, 1, 1); // 12 meses

        // Act
        BigDecimal juros = jurosService.calcularJurosSimples(
            principal, taxa, dataInicial, dataFinal, PeriodicidadeJuros.MENSAL
        );

        // Assert - J = 10000 * 0.01 * 12 = 1200
        assertEquals(new BigDecimal("1200.00"), juros);
    }

    @Test
    @DisplayName("Deve calcular juros simples anuais corretamente")
    void deveCalcularJurosSimplesAnuaisCorretamente() {
        // Arrange
        BigDecimal principal = new BigDecimal("10000.00");
        BigDecimal taxa = new BigDecimal("1.0"); // 1% ao mês = 12% ao ano
        LocalDate dataInicial = LocalDate.of(2020, 1, 1);
        LocalDate dataFinal = LocalDate.of(2024, 1, 1); // 4 anos

        // Act
        BigDecimal juros = jurosService.calcularJurosSimples(
            principal, taxa, dataInicial, dataFinal, PeriodicidadeJuros.ANUAL
        );

        // Assert - J = 10000 * 0.12 * 4 = 4800
        assertEquals(new BigDecimal("4800.00"), juros);
    }

    @Test
    @DisplayName("Deve calcular juros simples diários corretamente")
    void deveCalcularJurosSimplesDiariosCorretamente() {
        // Arrange
        BigDecimal principal = new BigDecimal("10000.00");
        BigDecimal taxa = new BigDecimal("1.0"); // 1% ao mês
        LocalDate dataInicial = LocalDate.of(2024, 1, 1);
        LocalDate dataFinal = LocalDate.of(2024, 1, 31); // 30 dias

        // Act
        BigDecimal juros = jurosService.calcularJurosSimples(
            principal, taxa, dataInicial, dataFinal, PeriodicidadeJuros.DIARIO
        );

        // Assert - Taxa diária = 0.01/30, J = 10000 * (0.01/30) * 30 = 100
        assertEquals(new BigDecimal("100.00"), juros);
    }

    @Test
    @DisplayName("Deve retornar zero quando principal é zero")
    void deveRetornarZeroQuandoPrincipalZero() {
        BigDecimal juros = jurosService.calcularJurosSimples(
            BigDecimal.ZERO,
            new BigDecimal("1.0"),
            LocalDate.of(2023, 1, 1),
            LocalDate.of(2024, 1, 1),
            PeriodicidadeJuros.MENSAL
        );

        assertEquals(BigDecimal.ZERO, juros);
    }

    @Test
    @DisplayName("Deve retornar zero quando taxa é zero")
    void deveRetornarZeroQuandoTaxaZero() {
        BigDecimal juros = jurosService.calcularJurosSimples(
            new BigDecimal("10000.00"),
            BigDecimal.ZERO,
            LocalDate.of(2023, 1, 1),
            LocalDate.of(2024, 1, 1),
            PeriodicidadeJuros.MENSAL
        );

        assertEquals(BigDecimal.ZERO, juros);
    }

    // ==================== Testes de Juros Compostos ====================

    @Test
    @DisplayName("Deve calcular juros compostos mensais corretamente")
    void deveCalcularJurosCompostosMensaisCorretamente() {
        // Arrange
        BigDecimal principal = new BigDecimal("10000.00");
        BigDecimal taxa = new BigDecimal("1.0"); // 1% ao mês
        LocalDate dataInicial = LocalDate.of(2023, 1, 1);
        LocalDate dataFinal = LocalDate.of(2024, 1, 1); // 12 meses

        // Act
        BigDecimal juros = jurosService.calcularJurosCompostos(
            principal, taxa, dataInicial, dataFinal, PeriodicidadeJuros.MENSAL
        );

        // Assert - M = 10000 * (1.01)^12 = 11268.25, J = 1268.25
        assertTrue(juros.compareTo(new BigDecimal("1268.00")) > 0);
        assertTrue(juros.compareTo(new BigDecimal("1269.00")) < 0);
    }

    @Test
    @DisplayName("Juros compostos devem ser maiores que juros simples no mesmo período")
    void jurosCompostosDevemSerMaioresQueJurosSimples() {
        // Arrange
        BigDecimal principal = new BigDecimal("10000.00");
        BigDecimal taxa = new BigDecimal("1.0");
        LocalDate dataInicial = LocalDate.of(2023, 1, 1);
        LocalDate dataFinal = LocalDate.of(2024, 1, 1);

        // Act
        BigDecimal jurosSimples = jurosService.calcularJurosSimples(
            principal, taxa, dataInicial, dataFinal, PeriodicidadeJuros.MENSAL
        );

        BigDecimal jurosCompostos = jurosService.calcularJurosCompostos(
            principal, taxa, dataInicial, dataFinal, PeriodicidadeJuros.MENSAL
        );

        // Assert
        assertTrue(jurosCompostos.compareTo(jurosSimples) > 0,
            "Juros compostos (" + jurosCompostos + ") devem ser maiores que simples (" + jurosSimples + ")");
    }

    // ==================== Testes de Pro Rata Die ====================

    @Test
    @DisplayName("Deve calcular pro rata die corretamente")
    void deveCalcularProRataDieCorretamente() {
        // Arrange
        BigDecimal principal = new BigDecimal("10000.00");
        BigDecimal taxaMensal = new BigDecimal("1.0"); // 1% ao mês
        int dias = 15;

        // Act
        BigDecimal juros = jurosService.calcularProRataDie(principal, taxaMensal, dias);

        // Assert - J = 10000 * (0.01/30) * 15 = 50
        assertEquals(new BigDecimal("50.00"), juros);
    }

    @Test
    @DisplayName("Deve calcular pro rata die para mês completo igual a juros mensal")
    void deveCalcularProRataDieParaMesCompletoIgualJurosMensal() {
        // Arrange
        BigDecimal principal = new BigDecimal("10000.00");
        BigDecimal taxaMensal = new BigDecimal("1.0");
        int dias = 30;

        // Act
        BigDecimal juros = jurosService.calcularProRataDie(principal, taxaMensal, dias);

        // Assert - J = 10000 * 0.01 = 100
        assertEquals(new BigDecimal("100.00"), juros);
    }

    // ==================== Testes do método calcular (dispatcher) ====================

    @Test
    @DisplayName("Deve usar juros simples quando tipo é SIMPLES")
    void deveUsarJurosSimplesQuandoTipoSimples() {
        BigDecimal resultado = jurosService.calcular(
            new BigDecimal("10000.00"),
            new BigDecimal("1.0"),
            TipoJuros.SIMPLES,
            LocalDate.of(2023, 1, 1),
            LocalDate.of(2024, 1, 1),
            PeriodicidadeJuros.MENSAL
        );

        assertEquals(new BigDecimal("1200.00"), resultado);
    }

    @Test
    @DisplayName("Deve usar juros compostos quando tipo é COMPOSTO")
    void deveUsarJurosCompostosQuandoTipoComposto() {
        BigDecimal resultado = jurosService.calcular(
            new BigDecimal("10000.00"),
            new BigDecimal("1.0"),
            TipoJuros.COMPOSTO,
            LocalDate.of(2023, 1, 1),
            LocalDate.of(2024, 1, 1),
            PeriodicidadeJuros.MENSAL
        );

        // Juros compostos > 1200
        assertTrue(resultado.compareTo(new BigDecimal("1200.00")) > 0);
    }

    @Test
    @DisplayName("Deve retornar zero quando principal é nulo")
    void deveRetornarZeroQuandoPrincipalNulo() {
        BigDecimal resultado = jurosService.calcular(
            null,
            new BigDecimal("1.0"),
            TipoJuros.SIMPLES,
            LocalDate.of(2023, 1, 1),
            LocalDate.of(2024, 1, 1),
            PeriodicidadeJuros.MENSAL
        );

        assertEquals(BigDecimal.ZERO, resultado);
    }

    @Test
    @DisplayName("Deve retornar zero quando taxa é nula")
    void deveRetornarZeroQuandoTaxaNula() {
        BigDecimal resultado = jurosService.calcular(
            new BigDecimal("10000.00"),
            null,
            TipoJuros.SIMPLES,
            LocalDate.of(2023, 1, 1),
            LocalDate.of(2024, 1, 1),
            PeriodicidadeJuros.MENSAL
        );

        assertEquals(BigDecimal.ZERO, resultado);
    }
}
