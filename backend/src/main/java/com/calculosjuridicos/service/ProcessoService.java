package com.calculosjuridicos.service;

import com.calculosjuridicos.dto.request.ProcessoRequest;
import com.calculosjuridicos.entity.Processo;
import com.calculosjuridicos.entity.TipoAcao;
import com.calculosjuridicos.exception.ResourceNotFoundException;
import com.calculosjuridicos.repository.ProcessoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessoService {

    private final ProcessoRepository processoRepository;

    @Transactional
    public Processo criar(ProcessoRequest request) {
        Processo processo = Processo.builder()
            .numeroProcesso(request.getNumeroProcesso())
            .descricao(request.getDescricao())
            .varaTribunal(request.getVaraTribunal())
            .tipoAcao(request.getTipoAcao())
            .build();

        return processoRepository.save(processo);
    }

    @Transactional(readOnly = true)
    public Processo buscarPorId(Long id) {
        return processoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Processo", "id", id));
    }

    @Transactional(readOnly = true)
    public Page<Processo> listar(String numeroProcesso, TipoAcao tipoAcao, Pageable pageable) {
        return processoRepository.findWithFilters(numeroProcesso, tipoAcao, pageable);
    }

    @Transactional
    public Processo atualizar(Long id, ProcessoRequest request) {
        Processo processo = processoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Processo", "id", id));

        processo.setNumeroProcesso(request.getNumeroProcesso());
        processo.setDescricao(request.getDescricao());
        processo.setVaraTribunal(request.getVaraTribunal());
        processo.setTipoAcao(request.getTipoAcao());

        return processoRepository.save(processo);
    }

    @Transactional
    public void excluir(Long id) {
        Processo processo = processoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Processo", "id", id));
        processoRepository.delete(processo);
    }

    @Transactional(readOnly = true)
    public long contar() {
        return processoRepository.count();
    }
}
