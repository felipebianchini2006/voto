# Sistema de Votação Eletrônica

Sistema seguro de votação eletrônica com preservação da privacidade do eleitor.

## Stack

- **Backend:** Java 21 + Spring Boot 3.4.12
- **Frontend:** React 19 + TypeScript + Vite
- **Database:** PostgreSQL 16
- **Security:** OAuth2 + AES-256-GCM encryption
- **Container:** Docker + Docker Compose

## Como Usar

### Docker (Recomendado)

```bash
# Iniciar sistema completo
docker-compose up -d
```

**Acessar:**
- Frontend: http://localhost
- Backend API: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html

### Manual

```bash
# Backend
./mvnw spring-boot:run

# Frontend (em outro terminal)
cd frontend
npm install && npm run dev
```

**Acessar:**
- Frontend: http://localhost:5173
- Backend: http://localhost:8080

