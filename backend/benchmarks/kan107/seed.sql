DO $$ BEGIN
  IF current_database() <> 'kan107_benchmark' OR EXISTS (SELECT 1 FROM producto) THEN
    RAISE EXCEPTION 'Solo usar en kan107_benchmark recién inicializada y vacía';
  END IF;
END $$;
-- Solo para kan107_benchmark VACÍA. Semilla determinista, no datos de usuarios.
SELECT setseed(0.107);
INSERT INTO usuario(id_usuario,nombre,telefono,contrasena_hash,tipo_usuario)
SELECT n+1,'Persona '||n,'synthetic-'||n,'not-a-login',CASE WHEN n<=20000 THEN 'agricultor' ELSE 'distribuidor' END FROM generate_series(1,21000) n;
INSERT INTO agricultor(id_agricultor,id_usuario) SELECT n,n+1 FROM generate_series(1,20000) n;
INSERT INTO distribuidor(id_distribuidor,id_usuario,nombre_negocio,estado_verificacion)
SELECT n,n+20001,'Distribuidor '||lpad(n::text,4,'0'),CASE WHEN n%10<8 THEN 'verificado' ELSE 'pendiente' END FROM generate_series(1,1000) n;
INSERT INTO producto(id_producto,id_categoria,nombre,marca,descripcion,activo)
SELECT n,1+(n%5),'Producto '||n,'Marca '||(n%30),repeat('Insumo agrícola de prueba ',8),n%10<>0 FROM generate_series(1,10000) n;
INSERT INTO inventario_distribuidor(id_inventario,id_distribuidor,id_producto,precio,stock_disponible,unidad_medida,ultima_actualizacion)
SELECT (d-1)*100+s+1,d,CASE WHEN s=0 THEN 1 ELSE 2+((d*73+s)%9999) END,10+((d*31+s*17)%90000)/100.0,CASE WHEN d%7=0 THEN 0 ELSE 100 END,'unidad',TIMESTAMP '2026-10-11'-(d*100+s)*INTERVAL '1 minute'
FROM generate_series(1,1000) d CROSS JOIN generate_series(0,99) s;
INSERT INTO pedido(id_pedido,id_agricultor,id_distribuidor,fecha_pedido,estado,tipo_entrega,es_urgente,total_pedido)
SELECT n,1+floor(random()*20000)::int,1+floor(random()*1000)::int,NOW()-(random()*365)*INTERVAL '1 day',CASE WHEN n%10<6 THEN 'entregado' WHEN n%10=6 THEN 'confirmado' WHEN n%10=7 THEN 'preparando' WHEN n%10=8 THEN 'en_ruta' ELSE 'cancelado' END,'domicilio',n%20=0,200
FROM generate_series(1,200000) n;
INSERT INTO detalle_pedido(id_pedido,id_inventario,cantidad,precio_unitario)
SELECT p.id_pedido,(p.id_distribuidor-1)*100+CASE WHEN s=1 AND p.id_pedido%3=0 THEN 1 ELSE 2+((p.id_pedido*7+s*19)%99) END,2,25
FROM pedido p CROSS JOIN generate_series(1,4) s;
INSERT INTO resena(id_agricultor,id_producto,id_pedido,calificacion,comentario,fecha_resena)
SELECT p.id_agricultor,i.id_producto,p.id_pedido,1+(p.id_pedido%5),'Reseña sintética',p.fecha_pedido+INTERVAL '2 days'
FROM pedido p JOIN detalle_pedido dp ON dp.id_pedido=p.id_pedido JOIN inventario_distribuidor i USING(id_inventario)
WHERE p.id_pedido<=25000 ON CONFLICT(id_agricultor,id_producto) DO NOTHING;
INSERT INTO producto_seguido(id_agricultor,id_producto,precio_al_seguir,fecha)
SELECT 1+(n%20000),1+((n*31)%10000),100,NOW()-n*INTERVAL '1 minute' FROM generate_series(1,20000) n;
INSERT INTO notificacion(id_agricultor,id_distribuidor,tipo,fecha)
SELECT CASE WHEN n%2=0 THEN 1+(n%20000) END,CASE WHEN n%2=1 THEN 1+(n%1000) END,'prueba',NOW()-n*INTERVAL '1 minute' FROM generate_series(1,200000) n;
-- Avanzar secuencias tras IDs explícitos para verificar escrituras normales.
SELECT setval(pg_get_serial_sequence('usuario','id_usuario'),(SELECT max(id_usuario) FROM usuario));
SELECT setval(pg_get_serial_sequence('agricultor','id_agricultor'),20000);
SELECT setval(pg_get_serial_sequence('distribuidor','id_distribuidor'),1000);
SELECT setval(pg_get_serial_sequence('producto','id_producto'),10000);
SELECT setval(pg_get_serial_sequence('inventario_distribuidor','id_inventario'),100000);
SELECT setval(pg_get_serial_sequence('pedido','id_pedido'),200000);
ANALYZE;
