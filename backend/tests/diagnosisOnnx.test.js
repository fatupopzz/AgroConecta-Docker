const { infer } = require('../scripts/diagnose_onnx');
const input = { modelPath: '/model.onnx', dims: [1, 2], data: [0.1, 0.5], labels: ['sano', 'enfermedad-prueba'] };
function runtime(scores) {
    const session = {
        inputNames: ['image'], outputNames: ['scores'],
        run: vi.fn().mockResolvedValue({ scores: { data: Float32Array.from(scores) } }),
        release: vi.fn().mockResolvedValue(),
    };
    return { session, InferenceSession: { create: vi.fn().mockResolvedValue(session) }, Tensor: class { constructor(type, data, dims) { Object.assign(this, { type, data, dims }); } } };
}
test('Plan B interpreta probabilidades y libera sesión CPU', async () => {
    const ort = runtime([0.1, 0.9]);
    const result = await infer(input, ort);
    expect(result.enfermedad).toBe('enfermedad-prueba');
    expect(result.confianza).toBeCloseTo(0.9);
    expect(ort.InferenceSession.create).toHaveBeenCalledWith('/model.onnx', { executionProviders: ['cpu'] });
    expect(ort.session.release).toHaveBeenCalledOnce();
});
test('convierte logits de forma numéricamente estable', async () => {
    expect((await infer({ ...input, outputKind: 'logits' }, runtime([1000, 1002]))).confianza).toBeCloseTo(0.880797);
});
test('rechaza tensor incompatible antes de cargar modelo', async () => {
    const ort = runtime([]);
    await expect(infer({ ...input, dims: [1, 3] }, ort)).rejects.toThrow('inválidos');
    expect(ort.InferenceSession.create).not.toHaveBeenCalled();
});
test.each([{ scores: [0.2] }, { scores: [3, 4] }])('rechaza salida incompatible y libera sesión: %j', async ({ scores }) => {
    const ort = runtime(scores);
    await expect(infer(input, ort)).rejects.toThrow();
    expect(ort.session.release).toHaveBeenCalledOnce();
});
