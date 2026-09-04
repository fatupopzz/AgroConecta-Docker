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

# 2. Crear la configuración local (Linux)
cp .env.example .env
sed -i '/^JWT_SECRET=/d' .env
printf 'JWT_SECRET=' >> .env
openssl rand -hex 64 >> .env

# 3. Levantar el entorno
docker compose up --build

# 4. Verificar contenedores
docker compose ps

# 5. Detener el entorno
docker compose down
```

En PowerShell, crea `.env` con una clave criptográficamente aleatoria así:

```powershell
$bytes = New-Object byte[] 64
[Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
$secret = [Convert]::ToHexString($bytes).ToLowerInvariant()
"JWT_SECRET=$secret" | Set-Content .env
```

`docker-compose.yml` exige esta variable y no contiene un valor predeterminado.
El archivo `.env` está excluido de Git; solo `.env.example`, que no contiene
credenciales reales, debe versionarse. Cambiar `JWT_SECRET` invalida todos los
JWT emitidos anteriormente y obliga a los usuarios a iniciar sesión de nuevo.

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
│   ├── src/
│   │   ├── controllers/
│   │   ├── middleware/
│   │   ├── routes/
│   │   └── config/
│   ├── .env
│   ├── Dockerfile
│   ├── index.js
│   ├── package.json
│   └── sql/
│       └── init.sql
│
└── frontend/
    ├── Dockerfile
    ├── package.json
    └── server.js
```
## Despliegue en la Nube (Azure)

**IP Pública:** 20.63.8.63

| Servicio   | URL                              |
|------------|----------------------------------|
| Backend    | http://20.63.8.63:8080           |
| Frontend   | http://20.63.8.63:3000           |
| Adminer    | http://20.63.8.63:8081           |

### Actualizar el servidor tras cambios en develop

```bash
ssh -i ~/.ssh/agroconecta-key.pem azureuser@20.63.8.63
cd AgroConecta-Docker
git pull origin develop
docker compose down
docker compose up --build -d
```

### Rotar `JWT_SECRET` en la VM

Ejecutar dentro de `~/AgroConecta-Docker` con permisos restringidos:

```bash
umask 077
touch .env
sed -i '/^JWT_SECRET=/d' .env
printf 'JWT_SECRET=' >> .env
openssl rand -hex 64 >> .env
docker compose up --build -d --force-recreate backend
```

La clave no debe imprimirse, copiarse al README ni enviarse al repositorio.
Después de reiniciar el backend se debe comprobar registro, login y una ruta
autenticada. Los tokens creados antes de la rotación deben responder `401`.

### Notas
- La VM se apaga automáticamente a las 9:00 PM (Guatemala)
- Para encenderla ir al portal de Azure y dar Start a agroconecta-vm

## Repositorio principal del proyecto

https://github.com/Juanjoo-Alvarez/Software1_AgroConecta
