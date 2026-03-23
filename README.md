# AgroConecta – Entorno Docker (Infraestructura como Código)

**CC3090 – Ingeniería de Software 1 | Universidad del Valle de Guatemala | Semestre I-2026**

**Equipo:**
- Juan Jose Rivas Alvarez – 24856
- Fátima Navarro – 24044
- Daniel Estuardo Sandoval Vasquez – 24885
- Adrián Penagos Arriaza – 24914

---

## Descripción

Este repositorio contiene el entorno de desarrollo de AgroConecta definido como Infraestructura como Código (IaC) usando Docker. Con un solo comando se levanta el entorno completo incluyendo base de datos, backend y frontend.

## Servicios

| Servicio   | Tecnología          | Puerto | Descripción                          |
|------------|---------------------|--------|--------------------------------------|
| db         | PostgreSQL 15       | 5432   | Base de datos principal              |
| backend    | Node.js 20 (Express)| 8080   | API REST                             |
| frontend   | Node.js 20 (Express)| 3000   | Panel web de pruebas                 |
| adminer    | Adminer             | 8081   | Interfaz web para PostgreSQL         |

## Requisitos

- Docker Desktop instalado y corriendo
- Git

## Uso
```bash
# 1. Clonar el repositorio
git clone https://github.com/fatupopzz/AgroConecta-Docker.git
cd AgroConecta-Docker

# 2. Levantar el entorno
docker compose up --build

# 3. Verificar contenedores
docker compose ps

# 4. Detener el entorno
docker compose down
```

## Credenciales de base de datos

| Campo       | Valor             |
|-------------|-------------------|
| Sistema     | PostgreSQL        |
| Servidor    | db                |
| Usuario     | agroconecta_user  |
| Contraseña  | agroconecta_pass  |
| Base de datos | agroconecta     |

## Estructura del proyecto
```
AgroConecta-Docker/
├── docker-compose.yml
├── backend/
│   ├── Dockerfile
│   ├── index.js
│   ├── package.json
│   └── sql/
│       └── init.sql
└── frontend/
    ├── Dockerfile
    ├── server.js
    └── package.json
```

## Video de demostración

https://youtu.be/FYurMSCdaw0

## Repositorio principal del proyecto

https://github.com/Juanjoo-Alvarez/Software1_AgroConecta
