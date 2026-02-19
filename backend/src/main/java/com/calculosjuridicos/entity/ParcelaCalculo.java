package com.calculosjuridicos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "parcela_calculo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParcelaCalculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calculo_id", nullable = false)
    private Calculo calculo;

    @Column(length = 255)
    private String descricao;

    @Column(name = "valor_original", nullable = false, precision = 18, scale = 2)
    private BigDecimal valorOriginal;

    @Column(name = "data_vencimento", nullable = false)
    private LocalDate dataVencimento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tabela_indice_id")
    private TabelaIndice tabelaIndice;

    public ParcelaCalculo(BigDecimal valorOriginal, LocalDate dataVencimento) {
        this.valorOriginal = valorOriginal;
        this.dataVencimento = dataVencimento;
    }

    public ParcelaCalculo(String descricao, BigDecimal valorOriginal, LocalDate dataVencimento) {
        this.descricao = descricao;
        this.valorOriginal = valorOriginal;
        this.dataVencimento = dataVencimento;
    }
}
