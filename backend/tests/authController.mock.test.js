jest.mock("../src/config/db", () => ({
    pool: {
        connect: jest.fn(),
        query: jest.fn()
    }
}));

jest.mock("bcrypt", () => ({
    hash: jest.fn(),
    compare: jest.fn()
}));

jest.mock("jsonwebtoken", () => ({
    sign: jest.fn()
}));

const bcrypt = require("bcrypt");
const jwt = require("jsonwebtoken");
const request = require("supertest");
const app = require("../app");
const { pool } = require("../src/config/db");

describe("Auth Controller - Mock Database", () => {

    let mockClient;

    beforeEach(() => {

        jest.clearAllMocks();

        mockClient = {
            query: jest.fn(),
            release: jest.fn()
        };

        pool.connect.mockResolvedValue(mockClient);

    });

    test("Debe retornar 400 cuando el usuario ya existe", async () => {

        mockClient.query.mockResolvedValueOnce({
            rows: [{}]
        });

        const response = await request(app)
            .post("/api/auth/register")
            .send({
                nombre: "Juan",
                apellido: "Perez",
                telefono: "55555555",
                email: "juan@test.com",
                password: "123456",
                tipo_usuario: "agricultor"
            });

        expect(response.statusCode).toBe(400);

        expect(response.body).toEqual({
            error: "El usuario ya existe"
        });

        expect(mockClient.release).toHaveBeenCalled();

    });

    test("Debe registrar un agricultor correctamente", async () => {

        bcrypt.hash.mockResolvedValue("hash123");

        mockClient.query
            .mockResolvedValueOnce({
                rows: [] // Usuario no existe
            })
            .mockResolvedValueOnce({}) // BEGIN
            .mockResolvedValueOnce({
                rows: [{
                    id_usuario: 1,
                    nombre: "Juan",
                    apellido: "Perez",
                    telefono: "55555555",
                    email: "juan@test.com",
                    tipo_usuario: "agricultor",
                    fecha_registro: "2026-01-01"
                }]
            })
            .mockResolvedValueOnce({
                rows: [{
                    id_agricultor: 10,
                    departamento: "Guatemala",
                    municipio: "Guatemala",
                    tipo_agricultor: "pequena_escala"
                }]
            })
            .mockResolvedValueOnce({}); // COMMIT

        const response = await request(app)
            .post("/api/auth/register")
            .send({
                nombre: "Juan",
                apellido: "Perez",
                telefono: "55555555",
                email: "juan@test.com",
                password: "123456",
                tipo_usuario: "agricultor",
                departamento: "Guatemala",
                municipio: "Guatemala"
            });

        expect(response.statusCode).toBe(201);

        expect(response.body.message).toBe("Usuario creado correctamente");

        expect(response.body.user.nombre).toBe("Juan");

        expect(response.body.perfil.id_agricultor).toBe(10);

        expect(mockClient.release).toHaveBeenCalled();

    });

    test("Debe iniciar sesión correctamente", async () => {

        bcrypt.compare.mockResolvedValue(true);
        jwt.sign.mockReturnValue("token-falso");

        pool.query
            .mockResolvedValueOnce({
                rows: [{
                    id_usuario: 1,
                    nombre: "Juan",
                    email: "juan@test.com",
                    contrasena_hash: "hash123",
                    tipo_usuario: "agricultor"
                }]
            })
            .mockResolvedValueOnce({
                rows: [{
                    id_agricultor: 10
                }]
            });

        const response = await request(app)
            .post("/api/auth/login")
            .send({
                email: "juan@test.com",
                password: "123456"
            });

        expect(response.statusCode).toBe(200);

        expect(response.body.message).toBe("Login exitoso");
        expect(response.body.token).toBe("token-falso");
        expect(response.body.nombre).toBe("Juan");
        expect(response.body.tipoUsuario).toBe("agricultor");
        expect(response.body.idPerfil).toBe(10);

    });

    test("Debe retornar 401 cuando la contraseña es incorrecta", async () => {

        pool.query.mockResolvedValueOnce({
            rows: [{
                id_usuario: 1,
                nombre: "Juan",
                email: "juan@test.com",
                contrasena_hash: "hash123",
                tipo_usuario: "agricultor"
            }]
        });

        bcrypt.compare.mockResolvedValue(false);

        const response = await request(app)
            .post("/api/auth/login")
            .send({
                email: "juan@test.com",
                password: "incorrecta"
            });

        expect(response.statusCode).toBe(401);

        expect(response.body).toEqual({
            error: "Contraseña incorrecta"
        });

    });

});