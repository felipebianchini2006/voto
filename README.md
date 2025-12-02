# Sistema de Votação Eletrônica

Sistema de votação eletrônica seguro, auditável e com preservação da privacidade do eleitor.

## Características

- ✅ Votação anônima e segura
- ✅ Criptografia de votos (AES-256-GCM)
- ✅ Sistema de auditoria com hash encadeado
- ✅ Autenticação OAuth2 para administradores
- ✅ Autenticação 2FA/OTP para eleitores
- ✅ Logs imutáveis e rastreáveis
- ✅ API RESTful documentada com OpenAPI/Swagger
- ✅ Testes automatizados com Testcontainers

## Stack Tecnológico

- **Backend:** Java 21, Spring Boot 3.4.12
- **Database:** PostgreSQL 16
- **Security:** Spring Security, OAuth2 Resource Server
- **Crypto:** Bouncy Castle
- **Migrations:** Flyway
- **API Docs:** SpringDoc OpenAPI
- **Testing:** JUnit 5, Testcontainers
- **Build:** Maven
- **Container:** Docker, Docker Compose

## Pré-requisitos

- Java 21+
- Docker e Docker Compose
- Maven 3.8+ (ou use o wrapper incluído `./mvnw`)

## Setup Rápido

### 1. Clonar o repositório

```bash
git clone <repository-url>
cd voto
```

### 2. Configurar variáveis de ambiente

```bash
cp .env.example .env
# Edite o .env conforme necessário
```

### 3. Iniciar o PostgreSQL via Docker

```bash
docker-compose up -d postgres
```

### 4. Compilar e executar

```bash
# Usando Maven Wrapper (recomendado)
./mvnw spring-boot:run

# Ou se tiver Maven instalado
mvn spring-boot:run
```

### 5. Acessar a aplicação

- **API:** http://localhost:8080
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **Health Check:** http://localhost:8080/actuator/health
- **PgAdmin:** http://localhost:5050 (admin@voto.local / admin123)

## Estrutura do Projeto

```
voto/
├── src/
│   ├── main/
│   │   ├── java/com/votoeletronico/voto/
│   │   │   ├── config/          # Configurações (Security, Web, OpenAPI)
│   │   │   ├── domain/          # Entidades JPA
│   │   │   ├── repository/      # Repositories
│   │   │   ├── service/         # Serviços de negócio
│   │   │   ├── controller/      # REST Controllers
│   │   │   ├── dto/             # DTOs (Request/Response)
│   │   │   ├── security/        # Segurança customizada
│   │   │   ├── crypto/          # Utilitários criptográficos
│   │   │   ├── audit/           # Sistema de auditoria
│   │   │   └── exception/       # Exception handlers
│   │   └── resources/
│   │       ├── db/migration/    # Migrations Flyway
│   │       ├── application.yml  # Configuração principal
│   │       └── logback-spring.xml
│   └── test/
│       ├── java/
│       └── resources/
├── docker/                      # Scripts Docker
├── docker-compose.yml
├── Dockerfile
├── pom.xml
└── README.md
```

## Comandos Úteis

### Build

```bash
# Compilar (sem testes)
./mvnw clean package -DskipTests

# Compilar (com testes)
./mvnw clean package
```

### Testes

```bash
# Executar todos os testes
./mvnw test

# Executar testes de integração
./mvnw verify

# Executar teste específico
./mvnw test -Dtest=ElectionServiceTest
```

### Docker

```bash
# Build da imagem
docker build -t voto:latest .

# Executar container
docker run -p 8080:8080 --env-file .env voto:latest

# Docker Compose (todos os serviços)
docker-compose up -d

# Parar serviços
docker-compose down

# Ver logs
docker-compose logs -f voto
```

### Database

```bash
# Acessar PostgreSQL via psql
docker exec -it voto-postgres psql -U voto_user -d voto_dev

# Backup do database
docker exec voto-postgres pg_dump -U voto_user voto_dev > backup.sql

# Restore
docker exec -i voto-postgres psql -U voto_user voto_dev < backup.sql
```

## Desenvolvimento

### Perfis de Ambiente

- **dev** - Desenvolvimento local (padrão)
- **staging** - Ambiente de staging
- **prod** - Produção

Ativar perfil:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=staging
```

### Flyway Migrations

Criar nova migration:
```bash
# Padrão: V{número}__{descrição}.sql
# Exemplo: V2__add_candidate_party.sql
```

### Logging

- **Console:** Plain text em dev, JSON em staging/prod
- **File:** JSON em `logs/application.json`
- **Audit:** Separado em `logs/audit.json`

### Hot Reload

Spring DevTools está habilitado em dev. Mudanças em código são recarregadas automaticamente.

## Arquitetura de Segurança

### Fluxo de Votação

1. **Autenticação do Eleitor**
   - Login com credenciais
   - Validação 2FA/OTP via e-mail
   - Verificação de elegibilidade

2. **Emissão de Token**
   - Token cego/assinado gerado
   - Token único e não reutilizável
   - Expiração configurável

3. **Submissão de Voto**
   - Voto criptografado no cliente
   - Token consumido (invalidado)
   - Voto armazenado sem link para identidade

4. **Auditoria**
   - Cada evento grava entrada no audit_log
   - Hash encadeado (blockchain-like)
   - Assinatura digital de cada entrada

### Endpoints Principais

#### Públicos (sem autenticação)
- `GET /api/public/elections` - Listar eleições públicas
- `POST /api/auth/login` - Login de eleitor
- `POST /api/vote/{electionId}/cast` - Submeter voto

#### Admin (OAuth2 JWT)
- `POST /api/admin/elections` - Criar eleição
- `POST /api/admin/elections/{id}/voters:import` - Importar eleitores
- `POST /api/admin/elections/{id}/start` - Iniciar eleição

#### Auditoria (ADMIN/AUDITOR)
- `GET /api/audit/{electionId}/log` - Log de auditoria
- `GET /api/audit/{electionId}/commitment` - Hash commitment

## Configuração OAuth2

### Keycloak (exemplo)

1. Criar realm `voto`
2. Criar client `voto-admin`
3. Configurar roles: `ADMIN`, `AUDITOR`, `OPERATOR`
4. Atualizar `application.yml`:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8081/realms/voto
          jwk-set-uri: http://localhost:8081/realms/voto/protocol/openid-connect/certs
```

## Monitoramento

### Actuator Endpoints

- `/actuator/health` - Status da aplicação
- `/actuator/metrics` - Métricas
- `/actuator/prometheus` - Métricas para Prometheus

### Métricas Disponíveis

- JVM (memória, threads, GC)
- HTTP requests (latência, throughput)
- Database connection pool
- Custom metrics (votos processados, tokens emitidos)

## Troubleshooting

### Erro: "Failed to connect to database"

Verifique se o PostgreSQL está rodando:
```bash
docker-compose ps
docker-compose logs postgres
```

### Erro: "Flyway migration failed"

Reset do database (⚠️ apenas em dev):
```bash
docker-compose down -v
docker-compose up -d postgres
```

### Erro: "Port 8080 already in use"

Altere a porta no `.env`:
```
SERVER_PORT=8081
```

## Contribuindo

1. Fork o projeto
2. Crie uma branch para sua feature (`git checkout -b feature/MinhaFeature`)
3. Commit suas mudanças (`git commit -m 'Add: MinhaFeature'`)
4. Push para a branch (`git push origin feature/MinhaFeature`)
5. Abra um Pull Request

## Licença

Proprietary - Todos os direitos reservados

## Contato

- **Email:** contato@voto.local
- **Documentação:** http://localhost:8080/swagger-ui.html

---

**Versão:** 0.0.1-SNAPSHOT
**Última atualização:** 2025-12-01
