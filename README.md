# Sistema de Cálculos Jurídicos Financeiros

Sistema web completo para cálculos de atualização financeira jurídica, com integração automática com o Banco Central do Brasil (BCB SGS).

## Tecnologias

### Backend
- Java 17+
- Spring Boot 3.2
- Spring Security + JWT
- Spring Data JPA / Hibernate
- Spring WebFlux (WebClient para API BCB)
- MySQL 8+
- Flyway (migrações)
- OpenAPI/Swagger

### Frontend
- React 18
- Bootstrap 5 / React Bootstrap
- React Router DOM
- Axios
- React Hook Form
- React Toastify

### Infraestrutura
- Docker & Docker Compose
- Nginx (proxy reverso + SPA)
- MySQL 8.0

## Estrutura do Projeto

```
├── backend/                    # API REST Spring Boot
│   ├── src/main/java/
│   │   └── com/calculosjuridicos/
│   │       ├── config/         # Configurações (Security, OpenAPI)
│   │       ├── controller/     # Controllers REST
│   │       ├── dto/            # DTOs request/response
│   │       ├── entity/         # Entidades JPA
│   │       ├── exception/      # Tratamento de erros
│   │       ├── repository/     # Repositórios JPA
│   │       ├── security/       # JWT e autenticação
│   │       └── service/        # Lógica de negócio
│   │           ├── CalculoService.java           # Orquestrador principal
│   │           ├── CorrecaoMonetariaService.java  # Correção monetária
│   │           ├── JurosService.java              # Cálculo de juros
│   │           ├── IndiceService.java             # Gestão de índices
│   │           ├── IndicesSyncService.java        # Sincronização BCB SGS
│   │           └── RelatorioService.java          # Geração PDF/Excel
│   ├── src/main/resources/
│   │   ├── db/migration/       # Scripts Flyway
│   │   │   ├── V1__create_initial_schema.sql
│   │   │   └── V2__update_bcb_series_codes.sql
│   │   └── application.yml     # Configurações
│   └── pom.xml
│
├── frontend/                   # SPA React
│   ├── src/
│   │   ├── api/               # Clientes API (Axios)
│   │   ├── components/        # Componentes React
│   │   ├── context/           # Contexts (Auth)
│   │   ├── pages/             # Páginas
│   │   └── App.jsx
│   ├── nginx.conf             # Configuração Nginx
│   └── package.json
│
└── docker-compose.yml         # Orquestração Docker
```

## Como Executar

### Opção 1: Docker Compose (Recomendado)

Sobe tudo (MySQL + Backend + Frontend) com um único comando:

```bash
# Construir e iniciar todos os serviços
docker-compose up --build -d

# Acompanhar logs
docker-compose logs -f

# Parar tudo
docker-compose down
```

Após iniciar:
- **Frontend**: http://localhost:8080
- **Backend API**: http://localhost:8081
- **Swagger UI**: http://localhost:8081/swagger-ui.html

### Opção 2: Desenvolvimento Local

#### Pré-requisitos
- Java 17+ (JDK)
- Maven 3.8+
- Node.js 18+ e npm
- MySQL 8+

#### 1. Banco de Dados

```sql
CREATE DATABASE calculos_juridicos
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER 'calcjuridico'@'localhost' IDENTIFIED BY 'senha_segura';
GRANT ALL PRIVILEGES ON calculos_juridicos.* TO 'calcjuridico'@'localhost';
FLUSH PRIVILEGES;
```

#### 2. Backend

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

API disponível em: http://localhost:8081

#### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend disponível em: http://localhost:5173

## Primeiros Passos Após a Instalação

1. **Registrar um usuário** em `/api/auth/register` (ou pela tela de cadastro)
2. **Fazer login** para obter o token JWT
3. **Sincronizar os índices do Banco Central**:
   - Na interface: vá em **Índices Monetários** e clique em **"Sincronizar Todos (BCB)"**
   - Ou via API: `POST /api/indices/sync/todos`
   - Isso busca os últimos 5 anos de dados de IPCA-E, INPC, IGP-M, TR e SELIC
4. **Criar um Processo** (caso jurídico)
5. **Realizar cálculos** de atualização monetária com os índices carregados

## Integração BCB SGS (Banco Central)

O sistema busca dados diretamente da API pública do Banco Central do Brasil (SGS - Sistema Gerenciador de Séries Temporais).

### Índices Suportados

| Índice | Série BCB SGS | Descrição |
|--------|---------------|-----------|
| IPCA-E | 10764 | Índice Nacional de Preços ao Consumidor Amplo Especial |
| INPC   | 188   | Índice Nacional de Preços ao Consumidor |
| IGP-M  | 189   | Índice Geral de Preços do Mercado |
| TR     | 226   | Taxa Referencial |
| SELIC  | 4390  | Taxa SELIC acumulada |

### Como funciona

- A API do BCB retorna **variações mensais (%)** para cada índice
- O serviço `IndicesSyncService` converte essas variações em **índices acumulados** (base 1000)
- A correção monetária usa a fórmula: `Valor Corrigido = Valor Original x (Índice Final / Índice Inicial)`

### Sincronização

| Método | Descrição |
|--------|-----------|
| **Manual (UI)** | Botão "Sincronizar Todos (BCB)" na página de Índices |
| **Manual (API)** | `POST /api/indices/{id}/sync` ou `POST /api/indices/sync/todos` |
| **Automática** | Executa no dia 15 de cada mês às 6h (configurável via cron) |

### Parâmetros opcionais da sincronização

```
POST /api/indices/{id}/sync?dataInicial=2020-01-01&dataFinal=2025-01-01
POST /api/indices/sync/todos?dataInicial=2020-01-01&dataFinal=2025-01-01
```

Se omitidos, busca os últimos 5 anos até a data atual.

## Variáveis de Ambiente

### Backend

| Variável | Descrição | Padrão |
|----------|-----------|--------|
| `DB_USERNAME` | Usuário MySQL | calcjuridico |
| `DB_PASSWORD` | Senha MySQL | senha_segura |
| `JWT_SECRET` | Chave secreta JWT (512+ bits) | (gerada) |
| `INDICES_BCB_BASE_URL` | URL base da API BCB | https://api.bcb.gov.br |
| `INDICES_BCB_TIMEOUT` | Timeout das chamadas BCB (ms) | 30000 |
| `INDICES_SYNC_ENABLED` | Habilitar sincronização agendada | true |
| `INDICES_SYNC_CRON` | Expressão cron para sync | 0 0 6 15 * ? |

### Frontend (.env)

```env
VITE_API_URL=http://localhost:8081/api
```

## Endpoints da API

### Autenticação
- `POST /api/auth/register` - Registrar usuário
- `POST /api/auth/login` - Login (retorna JWT)
- `POST /api/auth/refresh` - Renovar token
- `GET /api/auth/me` - Dados do usuário autenticado

### Processos
- `GET /api/processos` - Listar processos
- `POST /api/processos` - Criar processo
- `GET /api/processos/{id}` - Buscar processo
- `PUT /api/processos/{id}` - Atualizar processo
- `DELETE /api/processos/{id}` - Excluir processo

### Cálculos
- `POST /api/calculos/preview` - Preview sem salvar
- `GET /api/calculos` - Listar cálculos
- `POST /api/processos/{id}/calculos` - Criar cálculo
- `GET /api/calculos/{id}` - Buscar cálculo
- `POST /api/calculos/{id}/executar` - Executar e salvar resultado
- `DELETE /api/calculos/{id}` - Excluir cálculo
- `GET /api/calculos/{id}/relatorio` - Download relatório (PDF/Excel)

### Índices Monetários
- `GET /api/indices` - Listar tabelas de índices (público)
- `GET /api/indices/{id}/valores` - Listar valores de um índice (público)
- `POST /api/indices/{id}/valores/import` - Importar CSV (autenticado)
- `POST /api/indices/{id}/sync` - Sincronizar com BCB (autenticado)
- `POST /api/indices/sync/todos` - Sincronizar todos com BCB (autenticado)

## Funcionalidades

### Correção Monetária
- 5 índices oficiais: IPCA-E, INPC, IGP-M, TR, SELIC
- Sincronização automática com o Banco Central do Brasil
- Importação manual via CSV como alternativa

### Juros
- Juros simples e compostos
- Periodicidade: diária, mensal, anual
- Pro rata die (proporcional ao dia)
- Juros sobre valor original ou sobre valor corrigido

### Cálculos
- Múltiplas parcelas com datas de vencimento diferentes
- Multa percentual configurável
- Honorários advocatícios percentuais
- Detalhamento mensal da evolução do cálculo
- Exportação em PDF e Excel

## Testes

```bash
# Backend
cd backend
mvn test

# Frontend
cd frontend
npm test
```

## Changelog

### v1.1 - Integração BCB SGS
- Corrigido bug crítico no repositório de índices (JPQL LIMIT)
- Adicionada integração com a API BCB SGS para todos os 5 índices
- Unificados todos os índices para usar o Banco Central como fonte
- Sincronização manual (botão na UI) e automática (cron mensal)
- Migração Flyway V2 para atualizar códigos de série BCB
- Corrigido conflito de imports entre Apache POI e OpenPDF no RelatorioService

### v1.0 - Versão Inicial
- Sistema completo de cálculos jurídicos financeiros
- Autenticação JWT
- CRUD de processos e cálculos
- Geração de relatórios PDF e Excel
- Interface React com Bootstrap

## Licença

Proprietária - Todos os direitos reservados.
