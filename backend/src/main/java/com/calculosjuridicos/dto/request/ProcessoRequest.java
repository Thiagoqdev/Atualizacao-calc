package com.calculosjuridicos.dto.request;

import com.calculosjuridicos.entity.TipoAcao;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessoRequest {

    @Size(max = 50, message = "Número do processo deve ter no máximo 50 caracteres")
    private String numeroProcesso;

    @Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres")
    private String descricao;

    @Size(max = 255, message = "Vara/Tribunal deve ter no máximo 255 caracteres")
    private String varaTribunal;

    @NotNull(message = "Tipo de ação é obrigatório")
    private TipoAcao tipoAcao;
}
