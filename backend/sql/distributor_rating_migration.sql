-- Calificación de distribuidores desde sus propias reseñas.
--
-- Hasta ahora distribuidor.calificacion_promedio guardaba el promedio de las
-- reseñas de los productos que vende, y solo se actualizaba cuando alguien
-- abría su calificación, así que casi siempre estaba desactualizada. Desde
-- este cambio la calificación sale de resena_distribuidor y se recalcula al
-- guardar cada reseña; este script pone al día los valores existentes.
--
-- Se puede correr más de una vez: siempre deja el mismo resultado.

UPDATE distribuidor d
SET calificacion_promedio = COALESCE(
    (
        SELECT ROUND(AVG(r.calificacion)::numeric, 1)
        FROM resena_distribuidor r
        WHERE r.id_distribuidor = d.id_distribuidor
    ),
    0
);
