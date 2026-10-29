const fs = require('node:fs');
const {Client} = require('pg');
const root='/repo/backend';
const out='/bench';
const client=new Client({host:'127.0.0.1',port:5432,user:'postgres',database:'kan107_benchmark'});
function query(file,needle,replacements={}) {
 const source=fs.readFileSync(`${root}/src/${file}.js`,'utf8');
 const candidates=[...source.matchAll(/`([\s\S]*?)`/g)].map(m=>m[1]).filter(q=>q.includes(needle));
 if(candidates.length!==1) throw Error(`${file} ${needle}: ${candidates.length} matches`);
 let sql=candidates[0];
 const constants={'LEGACY_ORDER_STATES.PENDING':'pendiente','LEGACY_ORDER_STATES.IN_TRANSIT':'en_camino','ORDER_STATES.CONFIRMED':'confirmado','ORDER_STATES.IN_ROUTE':'en_ruta',...replacements};
 sql=sql.replace(/\$\{([^}]+)\}/g,(_,key)=>{if(!(key in constants)) throw Error(key); return constants[key];});
 return sql;
}
const queries=[];
function add(name,file,needle,params,replacements){queries.push({name,source:file,sql:query(file,needle,replacements),params});}
add('catalog_category','controllers/productController','MIN(i.precio) AS precio_desde',[1,20,0],{whereClause:'WHERE p.activo = true AND p.id_categoria = $1','queryParams.length - 1':2,'queryParams.length':3});
add('catalog_search','controllers/productController','MIN(i.precio) AS precio_desde',['%Producto 74%',20,0],{whereClause:'WHERE p.activo = true AND p.nombre ILIKE $1','queryParams.length - 1':2,'queryParams.length':3});
add('catalog_count','controllers/productController','SELECT COUNT(*) AS total',[1],{whereClause:'WHERE p.activo = true AND p.id_categoria = $1'});
add('offers','controllers/productController','SELECT i.id_inventario, i.precio',[74]);
add('offers_hot','controllers/productController','SELECT i.id_inventario, i.precio',[1]);
add('inventory','controllers/inventoryController','p.nombre AS producto',[1]);
add('distributors','controllers/distribuidorController','COALESCE(reviews.calificacion_promedio',[],{distanceColumn:'NULL::numeric AS distancia_km',orderBy:'d.nombre_negocio ASC'});
for(const state of [false,true]) add(state?'farmer_state':'farmer_orders','controllers/orderController','GROUP BY p.id_pedido, p.estado',state?[1,'entregado',20,0]:[1,20,0],{whereClause:'p.id_agricultor = $1'+(state?' AND p.estado = $2':''),'dataParams.length - 1':state?3:2,'dataParams.length':state?4:3});
add('distributor_orders','controllers/orderController','ORDER BY p.es_urgente DESC',[1]);
add('stats_summary','controllers/distribuidorController','COUNT(*)::int AS total_pedidos',[1,'entregado']);
add('stats_products','controllers/distribuidorController','SUM(dp.cantidad)::int AS cantidad',[1,'entregado']);
add('stats_states','controllers/distribuidorController','SELECT estado, COUNT(*)::int AS cantidad',[1]);
add('details','controllers/orderController','pr.marca AS producto_marca',[1]);
add('reviews','controllers/resenaController','SELECT r.id_resena',[74]);
add('distributor_reviews','controllers/distribuidorController','u.nombre AS agricultor_nombre',[1,20,0]);
add('frequent','repositories/CompraFrecuenteRepository','SELECT p.id_agricultor',[74]);
add('frequent_hot','repositories/CompraFrecuenteRepository','SELECT p.id_agricultor',[1]);
add('followers','repositories/ProductoSeguidoRepository','ORDER BY fecha DESC',[74]);
for(const role of ['agricultor','distribuidor']) add(`notifications_${role}`,'controllers/notificationController','id_notificacion,',[role==='agricultor'?1:2],{profileId:`id_${role}`});
const stage=process.argv[2]||'before';
(async()=>{
 await client.connect();
 if(stage==='seed') {
  await client.query(fs.readFileSync(`${root}/sql/001_init.sql`,'utf8'));
  await client.query(fs.readFileSync(`${out}/seed.sql`,'utf8'));
  await client.end(); return;
 }
 if(stage==='before') { await client.query('BEGIN'); for(const m of fs.readFileSync(`${root}/sql/014_kan107_sql_indexes.sql`,'utf8').matchAll(/CREATE INDEX IF NOT EXISTS (\w+)/g)) await client.query(`DROP INDEX IF EXISTS ${m[1]}`); }
 await client.query('ANALYZE');
 const results={stage,version:(await client.query('SELECT version()')).rows[0],indexes:(await client.query("SELECT tablename,indexname,indexdef,pg_relation_size(indexname::regclass) AS bytes FROM pg_indexes WHERE schemaname='public' ORDER BY tablename,indexname")).rows,constraints:(await client.query("SELECT conrelid::regclass::text AS table_name,conname,pg_get_constraintdef(oid) AS definition FROM pg_constraint WHERE connamespace='public'::regnamespace")).rows,queries:[]};
 for(const q of queries){
  const runs=[];
  for(let n=0;n<6;n++){
   const r=await client.query('EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) '+q.sql,q.params);
   runs.push(r.rows[0]['QUERY PLAN'][0]);
  }
  results.queries.push({...q,warmup:runs.shift(),runs});
  const sorted=runs.map(r=>r['Execution Time']).sort((a,b)=>a-b);
  console.log(stage,q.name,sorted[2]);
 }
 fs.writeFileSync(`${out}/${stage}.json`,JSON.stringify(results,null,2));
 if(stage==='before') await client.query('ROLLBACK');
 await client.end();
})().catch(e=>{console.error(e);process.exit(1)});
