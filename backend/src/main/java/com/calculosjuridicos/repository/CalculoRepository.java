package com.calculosjuridicos.repository;

import com.calculosjuridicos.entity.Calculo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CalculoRepository extends JpaRepository<Calculo, Long> {

    @Query("SELECT c FROM Calculo c ORDER BY c.dataCriacao DESC")
    List<Calculo> findRecent(Pageable pageable);
}
