# राज लक्ष्मी ज्वेलर्स — Backend API

**भगवान दास एंड संस** | Wazirganj, Gaya, Bihar — 805131

> *शाश्वत सुंदरता, भरोसे की विरासत*

Production-ready Spring Boot backend for the RajLaxmi Jewellers e-commerce platform.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.2.x |
| Security | Spring Security + JWT |
| Database | PostgreSQL 16 |
| Cache | Redis 7 |
| Build | Maven 3.9 |
| Docs | Swagger / OpenAPI 3 |
| Containerization | Docker + Docker Compose |

---

## Quick Start (Local Development)

### Prerequisites
- Java 21+
- Maven 3.9+
- Docker & Docker Compose

### 1. Clone and configure
```bash
cp .env.example .env
# Edit .env with your values
```

### 2. Start infrastructure
```bash
docker-compose up -d postgres redis
```

### 3. Run the application
```bash
mvn spring-boot:run
```

### 4. Access Swagger UI
```
http://localhost:8080/api/v1/swagger-ui.html
```

---

## Docker (Full Stack)

```bash
docker-compose up -d
```

Services:
- **App**: http://localhost:8080
- **PostgreSQL**: localhost:5432
- **Redis**: localhost:6379

---

## API Base URL

```
http://localhost:8080/api/v1
```

## Key Endpoints

| Module | Base Path |
|--------|-----------|
| Auth | `/auth/**` |
| Products | `/products/**` |
| Categories | `/categories/**` |
| Gold Rates | `/gold-rates/**` |
| Cart | `/cart/**` |
| Wishlist | `/wishlist/**` |
| Orders | `/orders/**` |
| Addresses | `/addresses/**` |
| Store Visits | `/store-visits/**` |
| Admin | `/admin/**` |
| User Profile | `/user/**` |

---

## Authentication

All protected endpoints require:
```
Authorization: Bearer <access_token>
```

1. Register: `POST /auth/register`
2. Verify OTP: `POST /auth/verify-otp`
3. Login: `POST /auth/login` → returns `accessToken` + `refreshToken`
4. Refresh: `POST /auth/refresh`

---

## Deploy to Railway

1. Connect GitHub repo to Railway
2. Add environment variables from `.env.example`
3. Add PostgreSQL and Redis plugins in Railway
4. Deploy — Railway auto-detects the Dockerfile

---

## Business Info

- **Shop**: Near Santoshi Mata Mandir, Wazirganj, Gaya, Bihar — 805131
- **Phone**: 9102316789 / 9693436005
- **WhatsApp**: 9102316789
- **Email**: rajlaxmijewellers.gaya@gmail.com
- **UPI**: nitinseth753@okhdfcbank
- **Instagram**: [@rajlaxmi_jewellerss](https://www.instagram.com/rajlaxmi_jewellerss)
