package com.calculosjuridicos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "calculo", indexes = {
    @Index(name = "idx_processo", columnList = "processo_id"),
    @Index(name = "idx_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Calculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processo_id")
    private Processo processo;

    @Column(nullable = false)
    private String titulo;

    @Column(name = "valor_principal", nullable = false, precision = 18, scale = 2)
    private BigDecimal valorPrincipal;

    @Column(name = "data_inicial", nullable = false)
    private LocalDate dataInicial;

    @Column(name = "data_final", nullable = false)
    private LocalDate dataFinal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tabela_indice_id")
    private TabelaIndice tabelaIndice;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_juros")
    @Builder.Default
    private TipoJuros tipoJuros = TipoJuros.SIMPLES;

    @Column(name = "taxa_juros", precision = 8, scale = 4)
    private BigDecimal taxaJuros;

    @Enumerated(EnumType.STRING)
    @Column(name = "periodicidade_juros")
    @Builder.Default
    private PeriodicidadeJuros periodicidadeJuros = PeriodicidadeJuros.MENSAL;

    @Column(name = "multa_percentual", precision = 8, scale = 4)
    @Builder.Default
    private BigDecimal multaPercentual = BigDecimal.ZERO;

    @Column(name = "honorarios_percentual", precision = 8, scale = 4)
    @Builder.Default
    private BigDecimal honorariosPercentual = BigDecimal.ZERO;

    @Column(name = "juros_sobre_corrigido")
    @Builder.Default
    private Boolean jurosSobreCorrigido = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusCalculo status = StatusCalculo.RASCUNHO;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime dataCriacao = LocalDateTime.now();

    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;

    @OneToMany(mappedBy = "calculo", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ParcelaCalculo> parcelas = new ArrayList<>();

    @OneToOne(mappedBy = "calculo", cascade = CascadeType.ALL, orphanRemoval = true)
    private ResultadoCalculo resultado;

    @PreUpdate
    protected void onUpdate() {
        this.dataAtualizacao = LocalDateTime.now();
    }

    public void addParcela(ParcelaCalculo parcela) {
        parcelas.add(parcela);
        parcela.setCalculo(this);
    }

    public void removeParcela(ParcelaCalculo parcela) {
        parcelas.remove(parcela);
        parcela.setCalculo(null);
    }

    public void clearParcelas() {
        parcelas.forEach(p -> p.setCalculo(null));
        parcelas.clear();
    }
}
