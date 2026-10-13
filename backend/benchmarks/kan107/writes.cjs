const fs=require('node:fs');const {Client}=require('pg');
const c=new Client({host:'127.0.0.1',user:'postgres',database:'kan107_benchmark'});
const statements=[
 ['insert_product',"INSERT INTO producto(id_categoria,nombre,activo) SELECT 1,'Escritura de prueba '||n,true FROM generate_series(1,1000) n"],
 ['insert_inventory',"INSERT INTO inventario_distribuidor(id_distribuidor,id_producto,precio,stock_disponible) SELECT 1,id_producto,100,10 FROM producto WHERE nombre LIKE 'Escritura de prueba %'"],
 ['insert_orders',"INSERT INTO pedido(id_agricultor,id_distribuidor,estado,total_pedido) SELECT 1,1,'confirmado',100 FROM generate_series(1,1000)"],
 ['insert_details',"INSERT INTO detalle_pedido(id_pedido,id_inventario,cantidad,precio_unitario) SELECT id_pedido,1,1,100 FROM pedido CROSS JOIN generate_series(1,4) WHERE id_pedido>200000"],
 ['update_product',"UPDATE producto SET id_categoria=2 WHERE id_producto BETWEEN 1 AND 1000"],
 ['update_inventory',"UPDATE inventario_distribuidor SET precio=precio+1,ultima_actualizacion=NOW() WHERE id_inventario BETWEEN 1 AND 1000"],
 ['update_orders',"UPDATE pedido SET estado='entregado' WHERE id_pedido BETWEEN 1 AND 1000"]
];
(async()=>{await c.connect();await c.query('BEGIN');if(process.argv[2]==='before') for(const m of fs.readFileSync('/repo/backend/sql/kan107_sql_indexes_migration.sql','utf8').matchAll(/CREATE INDEX IF NOT EXISTS (\w+)/g)) await c.query(`DROP INDEX IF EXISTS ${m[1]}`);const results=[];for(let run=0;run<6;run++){await c.query('SAVEPOINT write_run');for(const [name,sql] of statements){const r=await c.query('EXPLAIN (ANALYZE, BUFFERS, WAL, FORMAT JSON) '+sql);results.push({run,name,plan:r.rows[0]['QUERY PLAN'][0]});}await c.query('ROLLBACK TO SAVEPOINT write_run');}await c.query('ROLLBACK');fs.writeFileSync(`/bench/writes-${process.argv[2]}.json`,JSON.stringify(results,null,2));console.log('6 rolled-back runs of 7 write operations passed');await c.end();})().catch(e=>{console.error(e);process.exit(1)});
