# Sistema de Cálculos Jurídicos Financeiros

Sistema web completo para cálculos de atualização financeira jurídica.

## Tecnologias

### Backend
- Java 17+
- Spring Boot 3.2
- Spring Security + JWT
- Spring Data JPA / Hibernate
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

## Estrutura do Projeto

```
├── backend/                    # API REST Spring Boot
│   ├── src/main/java/
│   │   └── com/calculosjuridicos/
│   │       ├── config/         # Configurações
│   │       ├── controller/     # Controllers REST
│   │       ├── dto/            # DTOs request/response
│   │       ├── entity/         # Entidades JPA
│   │       ├── exception/      # Tratamento de erros
│   │       ├── repository/     # Repositórios JPA
│   │       ├── security/       # JWT e autenticação
│   │       └── service/        # Lógica de negócio
│   ├── src/main/resources/
│   │   ├── db/migration/       # Scripts Flyway
│   │   └── application.yml     # Configurações
│   └── pom.xml
│
├── frontend/                   # SPA React
│   ├── src/
│   │   ├── api/               # Clientes API
│   │   ├── components/        # Componentes React
│   │   ├── context/           # Contexts (Auth)
│   │   ├── pages/             # Páginas
│   │   └── App.jsx
│   └── package.json
│
└── docker-compose.yml         # Orquestração Docker
```

## Pré-requisitos

- Java 17+ (JDK)
- Maven 3.8+
- Node.js 18+ e npm
- MySQL 8+ (ou Docker)

## Configuração do Banco de Dados

### Opção 1: MySQL local

```sql
-- Criar banco de dados
CREATE DATABASE calculos_juridicos
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- Criar usuário
CREATE USER 'calcjuridico'@'localhost' IDENTIFIED BY 'senha_segura';
GRANT ALL PRIVILEGES ON calculos_juridicos.* TO 'calcjuridico'@'localhost';
FLUSH PRIVILEGES;
```

### Opção 2: Docker

```bash
docker run -d \
  --name mysql-calcjuridico \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=calculos_juridicos \
  -e MYSQL_USER=calcjuridico \
  -e MYSQL_PASSWORD=senha_segura \
  -p 3306:3306 \
  mysql:8.0
```

## Executando o Backend

```bash
cd backend

# Instalar dependências e compilar
mvn clean install

# Executar (development)
mvn spring-boot:run

# Ou com perfil específico
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

A API estará disponível em: http://localhost:8081

### Documentação da API (Swagger)
- Swagger UI: http://localhost:8081/swagger-ui.html
- OpenAPI JSON: http://localhost:8081/api-docs

## Executando o Frontend

```bash
cd frontend

# Instalar dependências
npm install

# Executar (development)
npm run dev

# Build para produção
npm run build
```

O frontend estará disponível em: http://localhost:5173

## Variáveis de Ambiente

### Backend (application.yml)

| Variável | Descrição | Padrão |
|----------|-----------|--------|
| `DB_USERNAME` | Usuário MySQL | calcjuridico |
| `DB_PASSWORD` | Senha MySQL | senha_segura |
| `JWT_SECRET` | Chave secreta JWT | (gerada) |

### Frontend (.env)

```env
VITE_API_URL=http://localhost:8081/api
```

## Docker Compose (Produção)

```bash
# Iniciar todos os serviços
docker-compose up -d

# Verificar logs
docker-compose logs -f

# Parar serviços
docker-compose down
```

## Endpoints Principais

### Autenticação
- `POST /api/auth/register` - Registrar usuário
- `POST /api/auth/login` - Login
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
- `POST /api/calculos/{id}/executar` - Executar cálculo
- `DELETE /api/calculos/{id}` - Excluir cálculo
- `GET /api/calculos/{id}/relatorio` - Download relatório

### Índices Monetários
- `GET /api/indices` - Listar tabelas de índices
- `GET /api/indices/{id}/valores` - Listar valores
- `POST /api/indices/{id}/valores/import` - Importar CSV

## Funcionalidades

### Correção Monetária
- Índices suportados: IPCA-E, INPC, IGP-M, TR, SELIC
- Importação de valores via CSV
- Integração com APIs oficiais (IBGE, BCB)

### Juros
- Juros simples e compostos
- Periodicidade: diária, mensal, anual
- Pro rata die

### Cálculos
- Múltiplas parcelas com datas diferentes
- Multa e honorários configuráveis
- Detalhamento mensal da evolução
- Exportação PDF e Excel

## Testes

### Backend
```bash
cd backend
mvn test
```

### Frontend
```bash
cd frontend
npm test
```

## Licença

Proprietária - Todos os direitos reservados.

## Contato

Para suporte técnico: suporte@calculosjuridicos.com
