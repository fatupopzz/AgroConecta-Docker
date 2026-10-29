-- Se guarda solo la ruta relativa (/uploads/productos/<archivo>) y no la URL
-- absoluta: así la IP o el dominio de la VM pueden cambiar sin tener que
-- reescribir los registros, y cada cliente arma la URL con su propia base.
-- IF NOT EXISTS permite volver a correr el script sin romper nada, porque
-- las migraciones se aplican a mano y no siempre se sabe si ya se aplicó.
ALTER TABLE producto
ADD COLUMN IF NOT EXISTS imagen_url VARCHAR(255);
