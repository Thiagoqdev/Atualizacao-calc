package com.calculosjuridicos.repository;

import com.calculosjuridicos.entity.ResultadoCalculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResultadoCalculoRepository extends JpaRepository<ResultadoCalculo, Long> {

    Optional<ResultadoCalculo> findByCalculoId(Long calculoId);

    void deleteByCalculoId(Long calculoId);
}
