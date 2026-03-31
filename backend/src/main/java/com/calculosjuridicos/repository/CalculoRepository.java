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

    @Query("SELECT c FROM Calculo c ORDER BY c.dataCriacao DESC")
    List<Calculo> findRecent(Pageable pageable);
}
