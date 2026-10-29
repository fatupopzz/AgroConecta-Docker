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
│   ├── scripts/
│   │   └── migrate.js
│   └── sql/
│       ├── 001_init.sql
│       ├── 002_cart.sql
│       └── … (migraciones numeradas)
│
└── frontend/
    ├── Dockerfile
    ├── package.json
    └── server.js
```
## Pruebas automatizadas

Cada pull request hacia `develop` corre en GitHub Actions las pruebas
unitarias del backend, las de integración con PostgreSQL y las de la app
Android; si alguna falla, no se puede fusionar. Detalle, ejecución local y
protección de la rama en [PRUEBAS_AUTOMATIZADAS.md](PRUEBAS_AUTOMATIZADAS.md).

## Migraciones de la base de datos

El esquema se versiona con los archivos numerados de `backend/sql/`
(`001_init.sql`, `002_cart.sql`, …) y la tabla `schema_migrations`, que guarda
el nombre de cada migración aplicada y cuándo se aplicó.

- `001_init.sql` es el esquema completo. Docker lo corre solo al crear la base
  por primera vez y se registra a sí mismo.
- **Al arrancar, el backend aplica las migraciones pendientes**, en orden y una
  sola vez cada una. No hay que correr scripts a mano al desplegar.
- Cada migración corre dentro de una transacción junto con su registro: si
  falla, no queda aplicada a medias ni registrada.

### Agregar un cambio al esquema

1. Crea `backend/sql/NNN_descripcion.sql` con el siguiente número libre
   (por ejemplo, `021_ubicacion_agricultor.sql`). Solo se aplican los archivos
   con ese formato.
2. Escríbela para que se pueda repetir sin error (`IF NOT EXISTS`, `IF EXISTS`,
   `ON CONFLICT`): así una base nueva y una vieja terminan igual.
3. Si el cambio es estructural, agrégalo también a `001_init.sql`, que debe
   seguir siendo el esquema completo.
4. **No edites una migración que ya se aplicó en la VM**: no se vuelve a correr.
   Crea una nueva.

### Comandos

Dentro del contenedor del backend (`docker compose exec backend …`) o desde
`backend/` con las variables de `.env`:

```bash
node scripts/migrate.js --status    # aplicadas y pendientes
node scripts/migrate.js --dry-run   # qué aplicaría, sin tocar la base
node scripts/migrate.js             # aplica las pendientes
```

### Bases anteriores al versionado

Una base que ya tenía las tablas pero no `schema_migrations` (la VM y las bases
locales creadas antes de este cambio) no se migra sola: el migrador se niega
para no volver a correr scripts como `002_cart.sql` o `006_hu_tracking_order.sql`.
Primero se marcan como aplicadas las que ya tiene, sin correrlas:

```bash
docker compose exec backend node scripts/migrate.js --baseline 019 --dry-run
docker compose exec backend node scripts/migrate.js --baseline 019
docker compose exec backend node scripts/migrate.js
```

`019_startup_schema.sql` contiene lo que el backend aplicaba en cada arranque
antes del versionado, así que toda base existente ya tiene hasta la 019. La
`020_align_schema.sql` corrige las diferencias que había entre la VM y una base
nueva (entre ellas, los estados de pago que la VM rechazaba).

En una base local vieja también se puede empezar de cero:
`docker compose down -v && docker compose up -d` (borra los datos locales).

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
docker compose exec backend node scripts/migrate.js --status
```

El backend aplica las migraciones pendientes al arrancar; `--status` sirve
para confirmar que no quedó ninguna.

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
