package com.calculosjuridicos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tabela_indice")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TabelaIndice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String nome;

    @Column(length = 255)
    private String descricao;

    @Column(name = "codigo_oficial", length = 20)
    private String codigoOficial;

    @Enumerated(EnumType.STRING)
    @Column(name = "fonte_api", length = 20)
    private FonteApi fonteApi;

    @OneToMany(mappedBy = "tabelaIndice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ValorIndice> valores = new ArrayList<>();

    public enum FonteApi {
        IBGE,
        BCB,
        FGV,
        MANUAL
    }

    public static final String IPCA_E = "IPCA_E";
    public static final String INPC = "INPC";
    public static final String IGPM = "IGPM";
    public static final String TR = "TR";
    public static final String SELIC = "SELIC";
}
