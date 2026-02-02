package com.calculosjuridicos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "resultado_calculo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResultadoCalculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calculo_id", nullable = false, unique = true)
    private Calculo calculo;

    @Column(name = "valor_corrigido", nullable = false, precision = 18, scale = 2)
    private BigDecimal valorCorrigido;

    @Column(name = "valor_juros", nullable = false, precision = 18, scale = 2)
    private BigDecimal valorJuros;

    @Column(name = "valor_multa", nullable = false, precision = 18, scale = 2)
    private BigDecimal valorMulta;

    @Column(name = "valor_honorarios", nullable = false, precision = 18, scale = 2)
    private BigDecimal valorHonorarios;

    @Column(name = "valor_total", nullable = false, precision = 18, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "data_calculo", nullable = false)
    @Builder.Default
    private LocalDateTime dataCalculo = LocalDateTime.now();

    @Column(name = "detalhamento_json", columnDefinition = "LONGTEXT")
    private String detalhamentoJson;
}
