package com.calculosjuridicos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "processo", indexes = {
    @Index(name = "idx_usuario", columnList = "usuario_id"),
    @Index(name = "idx_numero", columnList = "numero_processo")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Processo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_processo", length = 50)
    private String numeroProcesso;

    @Column(length = 500)
    private String descricao;

    @Column(name = "vara_tribunal")
    private String varaTribunal;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_acao", nullable = false)
    private TipoAcao tipoAcao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime dataCriacao = LocalDateTime.now();

    @OneToMany(mappedBy = "processo", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Calculo> calculos = new ArrayList<>();

    public void addCalculo(Calculo calculo) {
        calculos.add(calculo);
        calculo.setProcesso(this);
    }

    public void removeCalculo(Calculo calculo) {
        calculos.remove(calculo);
        calculo.setProcesso(null);
    }
}
