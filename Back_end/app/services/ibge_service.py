import httpx
from app.utils.exceptions import ExternalAPIException

# Mapeamento dos índices para os códigos do IBGE
IBGE_TABLES = {
    "ipca": 1737,
    "inpc": 1735,
    "ipcae": 1736
}

async def get_ibge_series(indice, periodo_inicial, periodo_final, variavel):
    if indice not in IBGE_TABLES:
        raise ValueError("Índice não suportado. Use: ipca, inpc, ipcae.")
    tabela = IBGE_TABLES[indice]
    url = (
        f"https://servicodados.ibge.gov.br/api/v3/agregados/{tabela}/"
        f"periodos/{periodo_inicial}-{periodo_final}/variaveis/{variavel}?localidades=N1[all]"
    )
    try:
        async with httpx.AsyncClient(timeout=10) as client:
            resp = await client.get(url)
            resp.raise_for_status()
            return resp.json()
    except Exception as e:
        raise ExternalAPIException(f"Erro ao acessar API do IBGE: {e}")
