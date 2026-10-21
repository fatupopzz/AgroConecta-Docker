-- Ejecutar en PostgreSQL de prueba después de init.sql y ambas migraciones.
BEGIN;
DO $$
DECLARE
    uid INT;
    cid INT;
    did INT;
    aid INT;
    matches INT;
BEGIN
    INSERT INTO usuario (nombre, telefono, contrasena_hash, tipo_usuario)
    VALUES ('Diagnóstico SQL test', 'diagnostico-sql-test', 'test', 'agricultor') RETURNING id_usuario INTO uid;
    INSERT INTO categoria (nombre) VALUES ('__diagnostico_test__') RETURNING id_categoria INTO cid;
    INSERT INTO enfermedad_tratamiento (enfermedad, cultivo, id_categoria, ingredientes_activos, recomendacion_general)
    VALUES ('test', 'test', cid, ARRAY['ingrediente-prueba'], 'Recomendación test');
    INSERT INTO producto (id_categoria, nombre, composicion, activo) VALUES
        (cid, 'Coincide', 'Ingrediente-prueba 50%', TRUE),
        (cid, 'Inactivo', 'ingrediente-prueba', FALSE),
        (cid, 'No coincide', 'otro ingrediente', TRUE),
        (cid, 'Subcadena incorrecta', 'superingrediente-prueba', TRUE);

    SELECT COUNT(*) INTO matches FROM producto p
    WHERE p.activo = TRUE AND EXISTS (
        SELECT 1 FROM enfermedad_tratamiento t
        CROSS JOIN LATERAL unnest(t.ingredientes_activos) AS ingrediente(nombre)
        WHERE LOWER(t.enfermedad) = 'test' AND LOWER(t.cultivo) = 'test'
          AND t.id_categoria = p.id_categoria
          AND BTRIM(ingrediente.nombre) <> ''
          AND STRPOS(
              ' ' || regexp_replace(LOWER(COALESCE(p.composicion, '')), '[^[:alnum:]]+', ' ', 'g') || ' ',
              ' ' || regexp_replace(LOWER(BTRIM(ingrediente.nombre)), '[^[:alnum:]]+', ' ', 'g') || ' '
          ) > 0
    );
    IF matches <> 1 THEN RAISE EXCEPTION 'Productos incorrectos: %', matches; END IF;

    INSERT INTO diagnostico (id_usuario, cultivo, enfermedad, confianza, foto, latitud, longitud)
    VALUES (uid, 'test', 'test', 0.95, '/test.jpg', 14.6, -90.5) RETURNING id_diagnostico INTO did;
    INSERT INTO alerta_plaga (id_usuario, tipo_plaga, cultivo_afectado, latitud, longitud)
    VALUES (uid, 'test', 'test', 14.6, -90.5) RETURNING id_alerta INTO aid;
    UPDATE diagnostico SET id_alerta = aid WHERE id_diagnostico = did;

    BEGIN
        INSERT INTO diagnostico (id_usuario, cultivo, enfermedad, confianza, foto)
        VALUES (uid, 'test', 'test', 1.01, '/test.jpg');
        RAISE EXCEPTION 'Aceptó confianza inválida';
    EXCEPTION WHEN check_violation THEN NULL;
    END;
    BEGIN
        INSERT INTO diagnostico (id_usuario, cultivo, enfermedad, confianza, foto, latitud)
        VALUES (uid, 'test', 'test', 0.5, '/test.jpg', 14.6);
        RAISE EXCEPTION 'Aceptó GPS incompleto';
    EXCEPTION WHEN check_violation THEN NULL;
    END;
END $$;
ROLLBACK;
