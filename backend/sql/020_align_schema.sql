-- Alinea el esquema de la VM con el que sale de correr las migraciones en una
-- base nueva. Al comparar los dos aparecieron cuatro diferencias; en cada caso
-- se dejó lo que el código necesita.

-- 1. Estados de pago. La VM solo aceptaba 'fallido' y 'reembolsado', pero el
--    simulador de pagos escribe 'processing' y 'failed' (paymentController):
--    en la VM esos cambios fallaban en silencio y ningún pago pasaba de
--    'pendiente'. Todos los pagos existentes estaban en 'pendiente', así que
--    cambiar la regla no deja filas inválidas.
ALTER TABLE pago DROP CONSTRAINT IF EXISTS pago_estado_pago_check;
ALTER TABLE pago
    ADD CONSTRAINT pago_estado_pago_check
    CHECK (estado_pago IN ('pendiente', 'processing', 'completado', 'failed'));

-- 2. Las reseñas de producto se guardan sin pedido (resenaController); en una
--    base nueva la columna era obligatoria y no se podía reseñar nada.
ALTER TABLE resena ALTER COLUMN id_pedido DROP NOT NULL;

-- 3. Mismo valor por defecto que en la VM.
ALTER TABLE ciclo_cultivo
    ALTER COLUMN productos_recomendados SET DEFAULT ARRAY[]::TEXT[];

-- 4. Mismo nombre de restricción que en la VM, para que cualquier mensaje de
--    error o script que la nombre funcione igual en todos los ambientes.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'resena'::regclass
          AND conname = 'resena_id_agricultor_id_producto_key'
    ) THEN
        ALTER TABLE resena
            RENAME CONSTRAINT resena_id_agricultor_id_producto_key
            TO resena_agricultor_producto_unique;
    END IF;
END $$;
