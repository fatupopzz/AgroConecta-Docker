const bcrypt = require("bcrypt");
const request = require("supertest");

const app = require("../../app");
const { pool, shutdownPool } = require("../../src/config/db");

jest.setTimeout(30_000);

const testEmails = new Set();
let sequence = 0;

const buildFarmer = (overrides = {}) => {
  sequence += 1;
  const unique = `${Date.now()}-${process.pid}-${sequence}`;
  const email = `ci.integration.${unique}@example.test`;
  testEmails.add(email);

  return {
    nombre: "Prueba",
    apellido: "Integracion",
    telefono: `502${String(Date.now()).slice(-7)}${sequence}`.slice(0, 20),
    email,
    password: "ClaveSegura123!",
    tipo_usuario: "agricultor",
    departamento: "Guatemala",
    municipio: "Mixco",
    ...overrides,
  };
};

const deleteTestUsers = async () => {
  if (testEmails.size === 0) return;
  await pool.query("DELETE FROM usuario WHERE email = ANY($1::text[])", [
    [...testEmails],
  ]);
  testEmails.clear();
};

beforeAll(async () => {
  await pool.query("SELECT 1");
});

afterEach(deleteTestUsers);

afterAll(async () => {
  await deleteTestUsers();
  await shutdownPool();
});

describe("Integración de autenticación con PostgreSQL", () => {
  test("registra al agricultor en usuario y agricultor con contraseña protegida", async () => {
    const farmer = buildFarmer();

    const response = await request(app).post("/api/auth/register").send(farmer);

    expect(response.statusCode).toBe(201);
    expect(response.body.message).toBe("Usuario creado correctamente");
    expect(response.body.user).not.toHaveProperty("contrasena_hash");
    expect(response.body.perfil).toMatchObject({
      departamento: farmer.departamento,
      municipio: farmer.municipio,
      tipo_agricultor: "pequena_escala",
    });

    const persisted = await pool.query(
      `SELECT u.email, u.contrasena_hash, u.tipo_usuario,
              a.departamento, a.municipio
       FROM usuario u
       JOIN agricultor a ON a.id_usuario = u.id_usuario
       WHERE u.email = $1`,
      [farmer.email],
    );

    expect(persisted.rowCount).toBe(1);
    expect(persisted.rows[0]).toMatchObject({
      email: farmer.email,
      tipo_usuario: "agricultor",
      departamento: farmer.departamento,
      municipio: farmer.municipio,
    });
    expect(persisted.rows[0].contrasena_hash).not.toBe(farmer.password);
    await expect(
      bcrypt.compare(farmer.password, persisted.rows[0].contrasena_hash),
    ).resolves.toBe(true);
  });

  test("inicia sesión con el usuario persistido y consulta su perfil autenticado", async () => {
    const farmer = buildFarmer();
    const registration = await request(app)
      .post("/api/auth/register")
      .send(farmer);
    expect(registration.statusCode).toBe(201);

    const login = await request(app).post("/api/auth/login").send({
      email: farmer.email,
      password: farmer.password,
    });

    expect(login.statusCode).toBe(200);
    expect(login.body).toMatchObject({
      message: "Login exitoso",
      nombre: farmer.nombre,
      tipoUsuario: "agricultor",
    });
    expect(login.body.token).toEqual(expect.any(String));

    const profile = await request(app)
      .get("/api/auth/me")
      .set("Authorization", `Bearer ${login.body.token}`);

    expect(profile.statusCode).toBe(200);
    expect(profile.body.user).toMatchObject({
      email: farmer.email,
      nombre: farmer.nombre,
      tipo_usuario: "agricultor",
    });
    expect(profile.body.perfil).toMatchObject({
      id_agricultor: login.body.idPerfil,
      departamento: farmer.departamento,
      municipio: farmer.municipio,
    });
  });

  test("rechaza un correo duplicado y conserva un solo usuario y perfil", async () => {
    const farmer = buildFarmer();
    const firstRegistration = await request(app)
      .post("/api/auth/register")
      .send(farmer);
    expect(firstRegistration.statusCode).toBe(201);

    const duplicate = await request(app)
      .post("/api/auth/register")
      .send({
        ...farmer,
        telefono: `${farmer.telefono}9`.slice(0, 20),
      });

    expect(duplicate.statusCode).toBe(400);
    expect(duplicate.body).toEqual({ error: "El usuario ya existe" });

    const persisted = await pool.query(
      `SELECT COUNT(*)::int AS users,
              COUNT(a.id_agricultor)::int AS farmer_profiles
       FROM usuario u
       LEFT JOIN agricultor a ON a.id_usuario = u.id_usuario
       WHERE u.email = $1`,
      [farmer.email],
    );

    expect(persisted.rows[0]).toEqual({ users: 1, farmer_profiles: 1 });
  });
});
