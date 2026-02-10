package com.calculosjuridicos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "valor_indice",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_indice_competencia",
        columnNames = {"tabela_indice_id", "competencia"}
    ),
    indexes = @Index(name = "idx_competencia", columnList = "competencia")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValorIndice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tabela_indice_id", nullable = false)
    private TabelaIndice tabelaIndice;

    @Column(nullable = false)
    private LocalDate competencia;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(20)")
    @Builder.Default
    private FonteValor fonte = FonteValor.MANUAL;

    public enum FonteValor {
        API_IBGE,
        API_BCB,
        API_FGV,
        CSV_IMPORT,
        MANUAL
    }
}
