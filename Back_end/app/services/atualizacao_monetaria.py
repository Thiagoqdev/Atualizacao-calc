from datetime import datetime, timedelta
from app.services.bcb_service import get_bcb_series
from app.services.ibge_service import get_ibge_series

def str_to_date(data: str) -> datetime:
    # Aceita 'dd/mm/yyyy' ou 'yyyy-mm-dd'
    try:
        if '/' in data:
            return datetime.strptime(data, "%d/%m/%Y")
        return datetime.strptime(data, "%Y-%m-%d")
    except Exception:
        raise ValueError("Formato de data inválido. Use 'dd/mm/yyyy'.")

def yyyymm(dt: datetime) -> str:
    return dt.strftime("%Y%m")

async def atualizar_monetariamente(valor_base: float, data_base: str, data_pagamento: str):
    """
    Atualiza o valor conforme a regra de negócio da Fazenda Pública (2020+).
    """
    dt_base = str_to_date(data_base)
    dt_pagamento = str_to_date(data_pagamento)

    if dt_base > dt_pagamento:
        raise ValueError("Data base não pode ser posterior à data de pagamento.")

    # 1. Corrige pelo INPC/IBGE de 01/2020 até 11/2021
    inpc_fim = datetime(2021, 11, 1)
    periodo_inicial = yyyymm(dt_base)
    periodo_final = yyyymm(min(dt_pagamento, inpc_fim))

    valor_corrigido = valor_base

    if dt_base <= inpc_fim:
        inpc_series = await get_ibge_series(
            "inpc", periodo_inicial, periodo_final, 63
        )
        for mes in inpc_series:
            fator = 1 + float(mes["valor"]) / 100
            valor_corrigido *= fator

    # 2. Corrige pela SELIC simples de 12/2021 em diante
    selic_inicio = datetime(2021, 12, 1)
    if dt_pagamento >= selic_inicio:
        # Valor já corrigido pelo INPC até 11/2021
        selic_data_inicial = max(dt_base, selic_inicio)
        selic_periodo_inicial = selic_data_inicial.strftime("%d/%m/%Y")
        selic_periodo_final = dt_pagamento.strftime("%d/%m/%Y")
        selic_series = await get_bcb_series(
            "selic_acumulada", selic_periodo_inicial, selic_periodo_final
        )
        for mes in selic_series:
            fator = 1 + float(mes["valor"]) / 100
            valor_corrigido *= fator

    return round(valor_corrigido, 2)
