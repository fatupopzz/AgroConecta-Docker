const request = require("supertest");
const app = require("../app");

describe("Auth Controller", () => {

    test("Debe retornar 400 si faltan datos en el registro", async () => {

        const response = await request(app)
            .post("/api/auth/register")
            .send({});

        expect(response.statusCode).toBe(400);

        expect(response.body).toEqual({
            error: "Datos incompletos. Requeridos: nombre, apellido, telefono, email, password, tipo_usuario"
        });

    });

    test("Debe retornas 400 si el tipo de usuario es invalido", async () => {

        const response = await request(app)
            .post("/api/auth/register")
            .send({
                nombre: "Juan",
                apellido: "Perez",
                telefono: "123456789",
                email: "juan@test.com",
                password: "password123",
                tipo_usuario: "cliente"
            });

        expect(response.statusCode).toBe(400);

        expect(response.body).toEqual({
            error: "tipo_usuario inválido. Use: agricultor | distribuidor"
        });
    });

    test("Debe retornar 400 si un distribuidor no envia nombre_negocio", async () => {

        const response = await request(app)
            .post("/api/auth/register")
            .send({
                nombre: "Juan",
                apellido: "Perez",
                telefono: "123456789",
                email: "juan@test.com",
                password: "password123",
                tipo_usuario: "distribuidor"
            });

        expect(response.statusCode).toBe(400);

        expect(response.body).toEqual({
            error: "nombre_negocio es obligatorio para distribuidores (mínimo 2 caracteres)"
        });
    });

   
});