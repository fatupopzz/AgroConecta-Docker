-- Ubicación de la finca del agricultor.
--
-- Se guarda como su dirección de entrega predeterminada (la misma fila que
-- consultan el clima y el checkout). Este índice impide que un agricultor
-- quede con dos direcciones predeterminadas: con dos, el clima y la entrega
-- podían tomar una distinta cada vez.
CREATE UNIQUE INDEX IF NOT EXISTS idx_direccion_entrega_predeterminada
    ON direccion_entrega (id_agricultor)
    WHERE es_predeterminada;
