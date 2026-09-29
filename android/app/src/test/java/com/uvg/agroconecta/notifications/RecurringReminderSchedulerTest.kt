package com.uvg.agroconecta.notifications

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.uvg.agroconecta.data.models.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class RecurringReminderSchedulerTest {
    private lateinit var context: Context
    private lateinit var scheduler: RecurringReminderScheduler
    private lateinit var workManager: WorkManager
    private val future = Instant.parse("2030-10-01T12:00:00Z")
    private val order = RecurringOrder(5, 17, RecurringFrequency.WEEKLY,
        listOf(RecurringProductDto(8, 2)), 3, "Parcela norte", future, RecurringStatus.ACTIVE)

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("recurring_reminders", Context.MODE_PRIVATE).edit().clear().commit()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        scheduler = RecurringReminderScheduler(context)
        workManager = WorkManager.getInstance(context)
    }

    @Test fun `active recurrence uses one unique work and edit replaces it`() {
        scheduler.reconcile(listOf(order), 17)
        val name = RecurringReminderScheduler.workName(17, 5)
        val first = workManager.getWorkInfosForUniqueWork(name).get().single { it.state == WorkInfo.State.ENQUEUED }
        scheduler.reconcile(listOf(order), 17)
        val unchanged = workManager.getWorkInfosForUniqueWork(name).get().single { it.state == WorkInfo.State.ENQUEUED }
        assertEquals(first.id, unchanged.id)
        scheduler.reconcile(listOf(order.copy(nextAt = future.plusSeconds(3600))), 17)
        val active = workManager.getWorkInfosForUniqueWork(name).get().filter { it.state == WorkInfo.State.ENQUEUED }
        assertEquals(1, active.size)
        assertNotEquals(first.id, active.single().id)
    }

    @Test fun `pause and cancel remove work`() {
        val name = RecurringReminderScheduler.workName(17, 5)
        scheduler.reconcile(listOf(order), 17)
        scheduler.reconcile(listOf(order.copy(status = RecurringStatus.PAUSED)), 17)
        assertTrue(workManager.getWorkInfosForUniqueWork(name).get().none { it.state == WorkInfo.State.ENQUEUED })
        scheduler.reconcile(listOf(order.copy(status = RecurringStatus.CANCELED)), 17)
        assertTrue(workManager.getWorkInfosForUniqueWork(name).get().none { it.state == WorkInfo.State.ENQUEUED })
    }

    @Test fun `less than lead time schedules immediately without negative delay`() {
        val now = Instant.parse("2026-10-01T10:00:00Z")
        assertEquals(Duration.ZERO, RecurringReminderScheduler.delayUntilReminder(now.plusSeconds(3600), now))
        assertEquals(Duration.ofHours(24), RecurringReminderScheduler.delayUntilReminder(now.plusSeconds(48 * 3600), now))
    }
}
