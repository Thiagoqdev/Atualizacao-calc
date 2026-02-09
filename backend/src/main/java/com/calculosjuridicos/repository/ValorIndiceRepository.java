package com.calculosjuridicos.repository;

import com.calculosjuridicos.entity.ValorIndice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ValorIndiceRepository extends JpaRepository<ValorIndice, Long> {

    @Query("SELECT v FROM ValorIndice v WHERE v.tabelaIndice.id = :tabelaIndiceId " +
           "AND v.competencia >= :dataInicial AND v.competencia <= :dataFinal " +
           "ORDER BY v.competencia ASC")
    List<ValorIndice> findByTabelaIndiceIdAndPeriodo(
        @Param("tabelaIndiceId") Long tabelaIndiceId,
        @Param("dataInicial") LocalDate dataInicial,
        @Param("dataFinal") LocalDate dataFinal
    );

    Optional<ValorIndice> findFirstByTabelaIndiceIdAndCompetenciaLessThanEqualOrderByCompetenciaDesc(
        Long tabelaIndiceId,
        LocalDate competencia
    );

    Optional<ValorIndice> findByTabelaIndiceIdAndCompetencia(Long tabelaIndiceId, LocalDate competencia);

    List<ValorIndice> findByTabelaIndiceIdOrderByCompetenciaDesc(Long tabelaIndiceId);

    @Query("SELECT MAX(v.competencia) FROM ValorIndice v WHERE v.tabelaIndice.id = :tabelaIndiceId")
    Optional<LocalDate> findMaxCompetenciaByTabelaIndiceId(@Param("tabelaIndiceId") Long tabelaIndiceId);

    @Query("SELECT MIN(v.competencia) FROM ValorIndice v WHERE v.tabelaIndice.id = :tabelaIndiceId")
    Optional<LocalDate> findMinCompetenciaByTabelaIndiceId(@Param("tabelaIndiceId") Long tabelaIndiceId);

    long countByTabelaIndiceId(Long tabelaIndiceId);

    void deleteByTabelaIndiceIdAndCompetenciaBetween(Long tabelaIndiceId, LocalDate inicio, LocalDate fim);
}
