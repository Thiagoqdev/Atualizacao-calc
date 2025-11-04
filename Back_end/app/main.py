from fastapi import FastAPI
from app.routers import indices

app = FastAPI(
    title="API de Correção Monetária Judicial",
    description="Expondo índices oficiais do BCB e IBGE para uso em cálculos judiciais.",
    version="1.0.0"
)

app.include_router(indices.router)
