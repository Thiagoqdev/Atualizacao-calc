from fastapi import APIRouter, Query, Path, HTTPException, Body
from app.services.bcb_service import get_bcb_series
from app.services.ibge_service import get_ibge_series

router = APIRouter(prefix="/indices", tags=["Índices"])

@router.get(
    "/bcb/{indice}",
    summary="Consulta índices do Banco Central (BCB)",
    response_description="Lista de valores históricos do índice solicitado.",
    responses={
        200: {"description": "Consulta realizada com sucesso."},
        400: {"description": "Parâmetro inválido ou índice não suportado."}
    }
)
async def get_bcb(
    indice: str = Path(
        ...,
        description=(
            "Índice a ser consultado. Valores aceitos:\n"
            "- **selic_meta**: SELIC meta ao ano\n"
            "- **selic_acumulada**: SELIC acumulada no mês\n"
            "- **tr**: Taxa Referencial (TR)\n"
            "- **igpm**: IGP-M (variação mensal)\n\n"
            "Exemplo correto: `selic_meta`\n"
            "Exemplo incorreto: `selic`"
        )
    ),
    data_inicial: str = Query(
        ...,
        description=(
            "Data inicial no formato `dd/mm/aaaa`.\n"
            "Exemplo correto: `01/01/2020`\n"
            "Exemplo incorreto: `2020-01-01`"
        )
    ),
    data_final: str = Query(
        ...,
        description=(
            "Data final no formato `dd/mm/aaaa`.\n"
            "Exemplo correto: `31/12/2022`\n"
            "Exemplo incorreto: `2022-12-31`"
        )
    )
):
    """
    Consulta índices oficiais do Banco Central do Brasil (BCB).

    **Índices disponíveis**:
    - `selic_meta`: SELIC meta ao ano
    - `selic_acumulada`: SELIC acumulada no mês
    - `tr`: Taxa Referencial (TR)
    - `igpm`: IGP-M (variação mensal)

    **Exemplo de uso**:
    /indices/bcb/selic_meta?data_inicial=01/01/2020&data_final=31/12/2022

    **Erros comuns**:
    - Índice não suportado: retorna erro 400 com mensagem explicando os valores aceitos.
    - Formato de data inválido: retorna erro 400.
    """
    try:
        return await get_bcb_series(indice, data_inicial, data_final)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))



@router.post(
    "/atualizacao-monetaria",
    summary="Atualização monetária de valores contra a Fazenda Pública (2020+)",
    response_description="Valor atualizado conforme as regras legais vigentes.",
    responses={
        200: {"description": "Valor atualizado com sucesso."},
        400: {"description": "Erro de parâmetro ou regra de negócio."}
    }
)
async def atualizar_valor(
    valor_base: float = Body(
        ...,
        description=(
            "Valor original devido, em reais. "
            "Exemplo: 1000.00"
        )
    ),
    data_base: str = Body(
        ...,
        description=(
            "Data base do valor devido. "
            "Aceita os formatos `dd/mm/aaaa` ou `aaaa-mm-dd`. "
            "Exemplo: `05/05/2020` ou `2020-05-05`"
        )
    ),
    data_pagamento: str = Body(
        ...,
        description=(
            "Data prevista para o pagamento. "
            "Aceita os formatos `dd/mm/aaaa` ou `aaaa-mm-dd`. "
            "Exemplo: `10/10/2024` ou `2024-10-10`"
        )
    )
):
    """
    Calcula a atualização monetária de valores devidos em condenações contra a Fazenda Pública Federal, Estadual ou Municipal, conforme as regras legais vigentes a partir de 2020.

    **Como funciona a atualização:**

    - **De 01/2020 até 11/2021:**
      O valor é corrigido mês a mês pelo INPC/IBGE acumulado do período.

    - **A partir de 12/2021:**
      O valor já corrigido pelo INPC/IBGE é atualizado mês a mês pela taxa SELIC simples (sem capitalização composta), até a data do pagamento.

    - **Transição automática:**
      Se o período abranger ambos os índices, a função faz a transição automaticamente: aplica o INPC/IBGE até novembro de 2021 e, a partir de dezembro de 2021, aplica a SELIC sobre o valor já corrigido.

    **Exemplo prático:**
    - Valor devido em 05/2020, pago em 10/2024:
      1. Corrige de 05/2020 até 11/2021 pelo INPC/IBGE.
      2. Corrige de 12/2021 até 10/2024 pela SELIC simples, sobre o valor já corrigido.

    **Fundamento legal:**
    - INPC/IBGE: Lei 10.741/2003, MP 316/2006, Lei 11.430/2006, RE 870.947 (STF).
    - SELIC: Emenda Constitucional 113/2021, art. 3º.

    **Observações:**
    - Caso haja decisão judicial determinando índice diverso, prevalece a decisão judicial.
    - Esta regra se aplica exclusivamente à atualização monetária de valores em condenações contra a Fazenda Pública.

    **Parâmetros:**
    - `valor_base`: Valor original devido (em reais).
    - `data_base`: Data base do valor devido.
    - `data_pagamento`: Data prevista para o pagamento.

    **Retorno:**
    - `valor_atualizado`: Valor final atualizado conforme as regras acima.
    """
    from app.services.atualizacao_monetaria import atualizar_monetariamente
    try:
        valor_atualizado = await atualizar_monetariamente(valor_base, data_base, data_pagamento)
        return {"valor_atualizado": valor_atualizado}
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@router.get(
    "/ibge/{indice}",
    summary="Consulta índices do IBGE",
    response_description="Lista de valores históricos do índice solicitado.",
    responses={
        200: {"description": "Consulta realizada com sucesso."},
        400: {"description": "Parâmetro inválido ou índice não suportado."}
    }
)
async def get_ibge(
    indice: str = Path(
        ...,
        description=(
            "Índice a ser consultado. Valores aceitos:\n"
            "- **ipca**: IPCA (Índice Nacional de Preços ao Consumidor Amplo)\n"
            "- **inpc**: INPC (Índice Nacional de Preços ao Consumidor)\n"
            "- **ipcae**: IPCA-E (IPCA Especial, trimestral)\n\n"
            "Exemplo correto: `ipca`\n"
            "Exemplo incorreto: `ipca_mensal`"
        )
    ),
    periodo_inicial: str = Query(
        ...,
        description=(
            "Período inicial no formato `yyyymm` (ano e mês).\n"
            "Exemplo correto: `202001`\n"
            "Exemplo incorreto: `2020-01`"
        )
    ),
    periodo_final: str = Query(
        ...,
        description=(
            "Período final no formato `yyyymm` (ano e mês).\n"
            "Exemplo correto: `202012`\n"
            "Exemplo incorreto: `2020-12`"
        )
    ),
    variavel: int = Query(
        63,
        description=(
            "Código da variável do IBGE.\n"
            "- `63`: Variação mensal (%)\n"
            "- `69`: Número-índice (ex: para IPCA-E)\n"
            "Exemplo correto: `63`\n"
            "Exemplo incorreto: `abc`"
        )
    )
):
    """
    Consulta índices oficiais do IBGE.

    **Índices disponíveis**:
    - `ipca`: IPCA (variação mensal)
    - `inpc`: INPC (variação mensal)
    - `ipcae`: IPCA-E (número-índice, trimestral)

    **Exemplo de uso**:
    /indices/ibge/ipca?periodo_inicial=202001&periodo_final=202012&variavel=63

    **Erros comuns**:
    - Índice não suportado: retorna erro 400 com mensagem explicando os valores aceitos.
    - Formato de período ou variável inválido: retorna erro 400.
    """
    try:
        return await get_ibge_series(indice, periodo_inicial, periodo_final, variavel)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))
