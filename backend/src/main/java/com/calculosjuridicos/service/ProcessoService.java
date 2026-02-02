package com.calculosjuridicos.service;

import com.calculosjuridicos.dto.request.ProcessoRequest;
import com.calculosjuridicos.entity.Processo;
import com.calculosjuridicos.entity.TipoAcao;
import com.calculosjuridicos.entity.Usuario;
import com.calculosjuridicos.exception.ResourceNotFoundException;
import com.calculosjuridicos.repository.ProcessoRepository;
import com.calculosjuridicos.repository.UsuarioRepository;
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
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public Processo criar(ProcessoRequest request, Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", usuarioId));

        Processo processo = Processo.builder()
            .numeroProcesso(request.getNumeroProcesso())
            .descricao(request.getDescricao())
            .varaTribunal(request.getVaraTribunal())
            .tipoAcao(request.getTipoAcao())
            .usuario(usuario)
            .build();

        return processoRepository.save(processo);
    }

    @Transactional(readOnly = true)
    public Processo buscarPorId(Long id, Long usuarioId) {
        return processoRepository.findByIdAndUsuarioId(id, usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Processo", "id", id));
    }

    @Transactional(readOnly = true)
    public Page<Processo> listarPorUsuario(Long usuarioId, String numeroProcesso, TipoAcao tipoAcao, Pageable pageable) {
        return processoRepository.findByUsuarioIdWithFilters(usuarioId, numeroProcesso, tipoAcao, pageable);
    }

    @Transactional
    public Processo atualizar(Long id, ProcessoRequest request, Long usuarioId) {
        Processo processo = processoRepository.findByIdAndUsuarioId(id, usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Processo", "id", id));

        processo.setNumeroProcesso(request.getNumeroProcesso());
        processo.setDescricao(request.getDescricao());
        processo.setVaraTribunal(request.getVaraTribunal());
        processo.setTipoAcao(request.getTipoAcao());

        return processoRepository.save(processo);
    }

    @Transactional
    public void excluir(Long id, Long usuarioId) {
        Processo processo = processoRepository.findByIdAndUsuarioId(id, usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Processo", "id", id));
        processoRepository.delete(processo);
    }

    @Transactional(readOnly = true)
    public long contarPorUsuario(Long usuarioId) {
        return processoRepository.countByUsuarioId(usuarioId);
    }
}
