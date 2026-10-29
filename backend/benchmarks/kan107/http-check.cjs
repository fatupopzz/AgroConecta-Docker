const fs=require('node:fs');
const assert=require('node:assert/strict');
const {Client}=require('pg');
const request=require('supertest');
const jwt=require('jsonwebtoken');
process.env.JWT_SECRET=require('node:crypto').randomBytes(48).toString('hex');
delete process.env.REDIS_URL;
const client=new Client({host:'127.0.0.1',user:'postgres',database:'kan107_benchmark'});
const dbPath=require.resolve('/repo/backend/src/config/db');
require.cache[dbPath]={id:dbPath,filename:dbPath,loaded:true,exports:{pool:{query:(...a)=>client.query(...a),connect:async()=>({query:(...a)=>client.query(...a),release(){}})}}};
const app=require('/repo/backend/app');
const farmer=jwt.sign({id:2,tipo:'agricultor'},process.env.JWT_SECRET);
const dist=jwt.sign({id:20002,tipo:'distribuidor'},process.env.JWT_SECRET);
const tests=[
 ['/api/products/74',null,200],['/api/products/74/compare',null,200],['/api/products/74/reviews',null,200],
 ['/api/inventory',dist,200],['/api/inventory',null,401],
 ['/api/distribuidores',farmer,200],['/api/distribuidores/1/reviews',farmer,200],['/api/distribuidores/1/stats',dist,200],['/api/distribuidores/2/stats',dist,403],
 ['/api/orders/farmer/1?page=1&limit=5',farmer,200],['/api/orders/farmer/1?page=2&limit=5&estado=entregado',farmer,200],['/api/orders/farmer/2',farmer,404],
 ['/api/orders/distributor/1',dist,200],['/api/orders/distributor/2',dist,403],['/api/orders/distributor/1',null,401],
 ['/api/products?id_categoria=1&limit=100',null,200],['/api/products?nombre=Producto%2074&id_categoria=5',null,200],
 ['/api/notifications',farmer,200],['/api/notifications',dist,200],['/api/notifications',null,401]
];
(async()=>{await client.connect();await client.query('BEGIN');if(process.argv[2]==='before') for(const m of fs.readFileSync('/repo/backend/sql/014_kan107_sql_indexes.sql','utf8').matchAll(/CREATE INDEX IF NOT EXISTS (\w+)/g)) await client.query(`DROP INDEX IF EXISTS ${m[1]}`);const results=[];
 for(const [url,token,status] of tests){let req=request(app).get(url);if(token)req=req.set('Authorization',`Bearer ${token}`);const res=await req;assert.equal(res.status,status,url);results.push({url,status:res.status,body:res.body});}
 const stage=process.argv[2];
 if(stage==='after'){
  const before=JSON.parse(fs.readFileSync('/bench/http-before.json'));
  for(let i=0;i<results.length;i++){
   if(results[i].url.startsWith('/api/products?')) {
    assert.equal(results[i].body.total,before[i].body.total);
    assert.equal(results[i].body.products.length,before[i].body.products.length);
    for(const p of results[i].body.products)assert.equal(p.categoria,results[i].url.includes('id_categoria=1')?'Fertilizantes':'Herramientas');
   } else if(results[i].url==='/api/distribuidores/1/reviews') {
    const a=results[i].body,b=before[i].body;
    assert.deepEqual({...a,reviews:[]},{...b,reviews:[]});
    assert.deepEqual(a.reviews.map(r=>r.fecha_resena),b.reviews.map(r=>r.fecha_resena));
    const boundary=a.reviews.at(-1).fecha_resena;
    const normalize=rows=>rows.filter(r=>r.fecha_resena!==boundary).sort((x,y)=>x.id_resena-y.id_resena);
    assert.deepEqual(normalize(a.reviews),normalize(b.reviews));
    for(const row of a.reviews) {
     const db=(await client.query('SELECT id_producto,calificacion,comentario FROM resena WHERE id_resena=$1',[row.id_resena])).rows[0];
     assert.deepEqual(db,{id_producto:row.id_producto,calificacion:row.calificacion,comentario:row.comentario});
    }
   } else assert.deepEqual(results[i],before[i],results[i].url);
  }
 }
 fs.writeFileSync(`/bench/http-${stage}.json`,JSON.stringify(results,null,2));
 console.log(`${results.length} HTTP checks passed (${stage}).`);
 await client.query('ROLLBACK');await client.end();
})().catch(e=>{console.error(e);process.exit(1)});
