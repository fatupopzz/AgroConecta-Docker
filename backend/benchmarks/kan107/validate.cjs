const fs = require('node:fs');
const assert = require('node:assert/strict');
const { Client } = require('pg');
const c = new Client({host:'127.0.0.1',user:'postgres',database:'kan107_benchmark'});
(async () => {
 await c.connect();
 const sql=fs.readFileSync('/repo/backend/sql/kan107_sql_indexes_migration.sql','utf8');
 const indexes=()=>c.query("SELECT indexname,indexdef FROM pg_indexes WHERE schemaname='public' AND indexname LIKE 'idx_kan107_%' ORDER BY indexname");
 const before=(await indexes()).rows;
 assert.equal(before.length,9,'init.sql debe crear los nueve índices');
 await c.query(sql); await c.query(sql);
 assert.deepEqual((await indexes()).rows,before,'migración idempotente y coherente con init.sql');
 const counts={};
 for(const table of ['usuario','agricultor','distribuidor','producto','inventario_distribuidor','pedido','detalle_pedido','resena','producto_seguido','notificacion']) counts[table]=Number((await c.query(`SELECT COUNT(*) AS n FROM ${table}`)).rows[0].n);
 fs.writeFileSync('/bench/validation.json',JSON.stringify({indexes:before,counts,migrationRuns:2,success:true},null,2));
 console.log('Instalación nueva y dos ejecuciones de migración: 9 definiciones idénticas');
 await c.end();
})().catch(e=>{console.error(e);process.exit(1)});
