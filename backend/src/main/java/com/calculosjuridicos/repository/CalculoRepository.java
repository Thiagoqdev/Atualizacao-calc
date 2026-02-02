package com.calculosjuridicos.repository;

import com.calculosjuridicos.entity.Calculo;
import com.calculosjuridicos.entity.StatusCalculo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CalculoRepository extends JpaRepository<Calculo, Long> {

    Page<Calculo> findByProcessoId(Long processoId, Pageable pageable);

    List<Calculo> findByProcessoId(Long processoId);

    @Query("SELECT c FROM Calculo c WHERE c.processo.usuario.id = :usuarioId")
    Page<Calculo> findByUsuarioId(@Param("usuarioId") Long usuarioId, Pageable pageable);

    @Query("SELECT c FROM Calculo c WHERE c.id = :id AND c.processo.usuario.id = :usuarioId")
    Optional<Calculo> findByIdAndUsuarioId(@Param("id") Long id, @Param("usuarioId") Long usuarioId);

    @Query("SELECT c FROM Calculo c WHERE c.processo.usuario.id = :usuarioId " +
           "AND (:status IS NULL OR c.status = :status)")
    Page<Calculo> findByUsuarioIdWithFilters(
        @Param("usuarioId") Long usuarioId,
        @Param("status") StatusCalculo status,
        Pageable pageable
    );

    @Query("SELECT COUNT(c) FROM Calculo c WHERE c.processo.usuario.id = :usuarioId")
    long countByUsuarioId(@Param("usuarioId") Long usuarioId);

    @Query("SELECT c FROM Calculo c WHERE c.processo.usuario.id = :usuarioId ORDER BY c.dataCriacao DESC")
    List<Calculo> findRecentByUsuarioId(@Param("usuarioId") Long usuarioId, Pageable pageable);
}
