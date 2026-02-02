package com.calculosjuridicos.repository;

import com.calculosjuridicos.entity.LogAuditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, Long> {

    Page<LogAuditoria> findByUsuarioId(Long usuarioId, Pageable pageable);

    List<LogAuditoria> findByUsuarioIdAndDataHoraBetween(
        Long usuarioId,
        LocalDateTime inicio,
        LocalDateTime fim
    );

    Page<LogAuditoria> findByAcao(String acao, Pageable pageable);

    Page<LogAuditoria> findByEntidadeAndEntidadeId(String entidade, Long entidadeId, Pageable pageable);
}
