import httpx
from app.utils.exceptions import ExternalAPIException

# Mapeamento dos índices para os códigos do BCB
BCB_SERIES = {
    "selic_meta": 11,
    "selic_acumulada": 4390,
    "tr": 226,
    "igpm": 189
}

async def get_bcb_series(indice, data_inicial, data_final):
    if indice not in BCB_SERIES:
        raise ValueError("Índice não suportado. Use: selic_meta, selic_acumulada, tr, igpm.")
    codigo = BCB_SERIES[indice]
    url = (
        f"https://api.bcb.gov.br/dados/serie/bcdata.sgs.{codigo}/dados"
        f"?formato=json&dataInicial={data_inicial}&dataFinal={data_final}"
    )
    try:
        async with httpx.AsyncClient(timeout=10) as client:
            resp = await client.get(url)
            resp.raise_for_status()
            return resp.json()
    except Exception as e:
        raise ExternalAPIException(f"Erro ao acessar API do BCB: {e}")
