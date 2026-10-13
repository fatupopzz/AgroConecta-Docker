const fs=require('node:fs');const {Client}=require('pg');
const c=new Client({host:'127.0.0.1',user:'postgres',database:'kan107_benchmark'});
const targets={
 idx_kan107_producto_categoria_activo:['catalog_count'],
 idx_kan107_inventario_producto_precio:['offers','frequent','catalog_category'],
 idx_kan107_pedido_agricultor_fecha:['farmer_orders','farmer_state'],
 idx_kan107_pedido_distribuidor_estado:['stats_summary','stats_products','distributor_orders'],
 idx_kan107_detalle_pedido_orden:['details','farmer_orders','stats_products'],
 idx_kan107_detalle_inventario_pedido:['frequent','frequent_hot'],
 idx_kan107_resena_producto_fecha:['reviews','distributor_reviews'],
 idx_kan107_notificacion_distribuidor_fecha:['notifications_distribuidor'],
 idx_kan107_pedido_entregado_fecha:['frequent_hot']
};
(async()=>{await c.connect();const queries=JSON.parse(fs.readFileSync('/bench/after.json')).queries;const results=[];
for(const [index,names] of Object.entries(targets)) {await c.query('BEGIN');await c.query(`DROP INDEX ${index}`);for(const name of names){const q=queries.find(q=>q.name===name);const runs=[];for(let n=0;n<6;n++){const r=await c.query('EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) '+q.sql,q.params);runs.push(r.rows[0]['QUERY PLAN'][0]);}results.push({removed:index,name,warmup:runs.shift(),runs});console.log(index,name,runs.map(r=>r['Execution Time']).sort((a,b)=>a-b)[2]);}await c.query('ROLLBACK');}
fs.writeFileSync('/bench/ablation.json',JSON.stringify(results,null,2));await c.end();})().catch(e=>{console.error(e);process.exit(1)});
