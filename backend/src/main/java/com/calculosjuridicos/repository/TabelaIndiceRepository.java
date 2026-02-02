package com.calculosjuridicos.repository;

import com.calculosjuridicos.entity.TabelaIndice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TabelaIndiceRepository extends JpaRepository<TabelaIndice, Long> {

    Optional<TabelaIndice> findByNome(String nome);

    boolean existsByNome(String nome);
}
