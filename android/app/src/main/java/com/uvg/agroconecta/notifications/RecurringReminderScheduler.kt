package com.uvg.agroconecta.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.uvg.agroconecta.R
import com.uvg.agroconecta.data.api.SessionManager
import com.uvg.agroconecta.data.models.RecurringOrder
import com.uvg.agroconecta.data.models.RecurringStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** KAN-89 no define anticipación. Se avisa 24 horas antes de fecha_proximo. */
const val RECURRING_REMINDER_LEAD_HOURS = 24L

class RecurringReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager get() = WorkManager.getInstance(context)
    private val preferences get() = context.getSharedPreferences("recurring_reminders", Context.MODE_PRIVATE)

    fun reconcile(orders: List<RecurringOrder>, userId: Int) {
        val active = orders.filter { it.userId == userId && it.status == RecurringStatus.ACTIVE }
        val wanted = active.map { workName(userId, it.id) }.toSet()
        val previous = preferences.getStringSet("names", emptySet()).orEmpty().toSet()
        (previous - wanted).forEach { name ->
            workManager.cancelUniqueWork(name)
            preferences.edit().remove("scheduled_$name").apply()
        }
        active.forEach { schedule(it) }
        preferences.edit().putStringSet("names", wanted).apply()
    }

    fun clearOtherUsers(userId: Int) {
        val previous = preferences.getStringSet("names", emptySet()).orEmpty().toSet()
        val keep = previous.filterTo(mutableSetOf()) { it.startsWith("recurring_reminder_${userId}_") }
        (previous - keep).forEach { name ->
            workManager.cancelUniqueWork(name)
            preferences.edit().remove("scheduled_$name").apply()
        }
        preferences.edit().putStringSet("names", keep).apply()
    }

    fun refreshAfterPermissionGranted(orders: List<RecurringOrder>, userId: Int) {
        orders.filter { it.userId == userId && it.status == RecurringStatus.ACTIVE }.forEach {
            preferences.edit().remove("scheduled_${workName(userId, it.id)}").apply()
        }
        reconcile(orders, userId)
    }

    fun schedule(order: RecurringOrder) {
        val name = workName(order.userId, order.id)
        val deliveredKey = "delivered_$name"
        if (order.status != RecurringStatus.ACTIVE || !order.nextAt.isAfter(Instant.now())) {
            workManager.cancelUniqueWork(name)
            preferences.edit().remove("scheduled_$name").apply()
            return
        }
        if (preferences.getString(deliveredKey, null) == order.nextAt.toString()) {
            workManager.cancelUniqueWork(name)
            preferences.edit().remove("scheduled_$name").apply()
            return
        }
        if (preferences.getString("scheduled_$name", null) == order.nextAt.toString()) return
        val delay = delayUntilReminder(order.nextAt, Instant.now())
        val request = OneTimeWorkRequestBuilder<RecurringReminderWorker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("id" to order.id, "userId" to order.userId, "nextAt" to order.nextAt.toString()))
            .addTag("recurring_reminder")
            .build()
        workManager.enqueueUniqueWork(name, ExistingWorkPolicy.REPLACE, request)
        preferences.edit().putString("scheduled_$name", order.nextAt.toString()).apply()
    }

    fun cancel(userId: Int, id: Int) {
        val name = workName(userId, id)
        workManager.cancelUniqueWork(name)
        val previous = preferences.getStringSet("names", emptySet()).orEmpty()
        preferences.edit().putStringSet("names", previous - name).remove("scheduled_$name").apply()
    }

    companion object {
        fun workName(userId: Int, id: Int) = "recurring_reminder_${userId}_$id"

        /** Si faltan menos de 24 h y la fecha sigue futura, avisa de inmediato. */
        fun delayUntilReminder(next: Instant, now: Instant): Duration =
            Duration.between(now, next.minus(Duration.ofHours(RECURRING_REMINDER_LEAD_HOURS)))
                .coerceAtLeast(Duration.ZERO)
    }
}

class RecurringReminderWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val id = inputData.getInt("id", -1)
        val userId = inputData.getInt("userId", -1)
        val nextAt = inputData.getString("nextAt") ?: return Result.success()
        if (id <= 0 || userId <= 0 ||
            SessionManager.getUserId(applicationContext).first() != userId ||
            !applicationContext.canPostRecurringNotifications()
        ) return Result.success()
        RecurringReminderNotifications.show(applicationContext, id)
        applicationContext.getSharedPreferences("recurring_reminders", Context.MODE_PRIVATE)
            .edit().putString("delivered_${RecurringReminderScheduler.workName(userId, id)}", nextAt).apply()
        return Result.success()
    }
}

object RecurringReminderNotifications {
    private const val CHANNEL_ID = "recurring_order_reminders"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Pedidos recurrentes",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "Recordatorios antes de procesar pedidos recurrentes" }
            )
        }
    }

    fun show(context: Context, id: Int) {
        if (!context.canPostRecurringNotifications()) return
        createChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_nav_catalog)
            .setContentTitle("Pedido recurrente próximo")
            .setContentText("Tu pedido recurrente #$id se procesará pronto.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id + 100_000, notification)
    }
}

fun Context.canPostRecurringNotifications(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
