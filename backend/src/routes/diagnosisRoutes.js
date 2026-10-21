const router = require('express').Router();
const verifyToken = require('../middleware/authMiddleware');
const asyncHandler = require('../middleware/asyncHandler');
const { createDiagnosis, getMyDiagnoses, getDiagnosisProducts, publishDiagnosisAlert } = require('../controllers/diagnosisController');

router.use(verifyToken);
router.post('/', asyncHandler(createDiagnosis));
router.get('/mine', asyncHandler(getMyDiagnoses));
router.get('/:id/products', asyncHandler(getDiagnosisProducts));
router.post('/:id/alert', asyncHandler(publishDiagnosisAlert));

module.exports = router;
