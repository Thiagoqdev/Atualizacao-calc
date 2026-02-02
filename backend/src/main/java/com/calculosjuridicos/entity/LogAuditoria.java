package com.calculosjuridicos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "log_auditoria", indexes = {
    @Index(name = "idx_log_usuario", columnList = "usuario_id"),
    @Index(name = "idx_log_data", columnList = "data_hora")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(nullable = false, length = 100)
    private String acao;

    @Column(length = 50)
    private String entidade;

    @Column(name = "entidade_id")
    private Long entidadeId;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(length = 45)
    private String ip;

    @Column(name = "data_hora", nullable = false)
    @Builder.Default
    private LocalDateTime dataHora = LocalDateTime.now();

    public static final String ACAO_LOGIN = "LOGIN";
    public static final String ACAO_LOGOUT = "LOGOUT";
    public static final String ACAO_CRIAR_CALCULO = "CRIAR_CALCULO";
    public static final String ACAO_EXECUTAR_CALCULO = "EXECUTAR_CALCULO";
    public static final String ACAO_EXCLUIR_CALCULO = "EXCLUIR_CALCULO";
    public static final String ACAO_CRIAR_PROCESSO = "CRIAR_PROCESSO";
    public static final String ACAO_EXCLUIR_PROCESSO = "EXCLUIR_PROCESSO";
    public static final String ACAO_IMPORTAR_INDICES = "IMPORTAR_INDICES";
    public static final String ACAO_SINCRONIZAR_INDICES = "SINCRONIZAR_INDICES";
    public static final String ACAO_GERAR_RELATORIO = "GERAR_RELATORIO";
}
