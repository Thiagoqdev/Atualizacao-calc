package com.calculosjuridicos.repository;

import com.calculosjuridicos.entity.Processo;
import com.calculosjuridicos.entity.TipoAcao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProcessoRepository extends JpaRepository<Processo, Long> {

    @Query("SELECT p FROM Processo p WHERE " +
           "(:numeroProcesso IS NULL OR p.numeroProcesso LIKE %:numeroProcesso%) " +
           "AND (:tipoAcao IS NULL OR p.tipoAcao = :tipoAcao)")
    Page<Processo> findWithFilters(
        @Param("numeroProcesso") String numeroProcesso,
        @Param("tipoAcao") TipoAcao tipoAcao,
        Pageable pageable
    );
}
