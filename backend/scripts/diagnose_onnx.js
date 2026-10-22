// Plan B: ejecución local en CPU con el mismo modelo exportado a ONNX.
// La entrada debe llevar exactamente el preprocesamiento usado en Android.
const fs = require('node:fs/promises');

async function infer({ modelPath, dims, data, labels, inputName, outputName, outputKind = 'probabilities' }, runtime) {
    if (!modelPath || !Array.isArray(dims) || !dims.length ||
        dims.some((n) => !Number.isSafeInteger(n) || n <= 0) ||
        !Array.isArray(data) || dims.reduce((a, b) => a * b, 1) !== data.length ||
        data.some((n) => typeof n !== 'number' || !Number.isFinite(n)) ||
        !Array.isArray(labels) || !labels.length || labels.some((label) => typeof label !== 'string' || !label.trim()) ||
        !['probabilities', 'logits'].includes(outputKind)) {
        throw new Error('Modelo, tensor float32, etiquetas u outputKind inválidos');
    }
    const ort = runtime || require('onnxruntime-node');
    const session = await ort.InferenceSession.create(modelPath, { executionProviders: ['cpu'] });
    try {
        const input = inputName || session.inputNames[0];
        const output = outputName || session.outputNames[0];
        if (session.inputNames.length !== 1 || !session.inputNames.includes(input) || !session.outputNames.includes(output)) {
            throw new Error('Se requiere modelo de clasificación con una entrada y salida seleccionada válidas');
        }
        const result = await session.run({ [input]: new ort.Tensor('float32', Float32Array.from(data), dims) });
        let scores = Array.from(result[output].data);
        if (scores.length !== labels.length || scores.some((n) => !Number.isFinite(n))) throw new Error('Salida incompatible con etiquetas');
        if (outputKind === 'logits') {
            const max = Math.max(...scores);
            scores = scores.map((score) => Math.exp(score - max));
            const total = scores.reduce((a, b) => a + b, 0);
            scores = scores.map((score) => score / total);
        } else if (scores.some((n) => n < 0 || n > 1) || Math.abs(scores.reduce((a, b) => a + b, 0) - 1) > 0.01) {
            throw new Error('La salida no contiene probabilidades; configure outputKind según el modelo');
        }
        const index = scores.indexOf(Math.max(...scores));
        return { enfermedad: labels[index], confianza: scores[index] };
    } finally {
        await session.release();
    }
}

if (require.main === module) {
    (async () => {
        if (!process.argv[2]) throw new Error('Uso: node scripts/diagnose_onnx.js entrada.json');
        const input = JSON.parse(await fs.readFile(process.argv[2], 'utf8'));
        console.log(JSON.stringify(await infer(input)));
    })().catch((error) => {
        console.error(error.message);
        process.exitCode = 1;
    });
}

module.exports = { infer };
