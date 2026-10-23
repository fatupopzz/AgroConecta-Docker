package com.uvg.agroconecta.data.repository

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.uvg.agroconecta.data.api.SessionManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Envía los diagnósticos hechos sin señal. WorkManager lo corre cuando hay red,
 * aunque la app esté cerrada, y lo reintenta con espera creciente si falla.
 */
class DiagnosisSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    // Se usa un EntryPoint en lugar de @HiltWorker para no tener que cambiar
    // cómo arranca WorkManager, que también usan los recordatorios de pedidos.
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun diagnosisRepository(): DiagnosisRepository
    }

    override suspend fun doWork(): Result {
        // Solo los pendientes de quien tiene la sesión abierta: el token que
        // agrega AuthInterceptor es el suyo, y no se deben enviar a nombre de
        // otra cuenta. Los de otros usuarios esperan a que vuelvan a entrar.
        val userId = SessionManager.getUserId(applicationContext).first() ?: return Result.success()
        val repository = EntryPointAccessors
            .fromApplication(applicationContext, Dependencies::class.java)
            .diagnosisRepository()

        return try {
            if (repository.syncPending(userId) == 0) Result.success() else Result.retry()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

class WorkManagerDiagnosisSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) : DiagnosisSyncScheduler {

    override fun schedule() {
        val request = OneTimeWorkRequestBuilder<DiagnosisSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()
        // Con KEEP, un diagnóstico guardado mientras el worker ya corre quedaría
        // sin enviar hasta el siguiente. APPEND_OR_REPLACE encola otra pasada
        // detrás; si no encuentra pendientes termina enseguida.
        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    private companion object {
        const val WORK_NAME = "diagnosis_sync"
        const val BACKOFF_SECONDS = 30L
    }
}
