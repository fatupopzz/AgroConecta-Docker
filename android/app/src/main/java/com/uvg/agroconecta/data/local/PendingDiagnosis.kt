package com.uvg.agroconecta.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * Diagnóstico hecho en el teléfono que todavía no llegó, o acaba de llegar, al
 * backend. [remoteId] queda en null hasta que POST /api/diagnoses responde. La
 * fila se conserva un tiempo después de enviarla para que la pantalla pueda
 * recuperar el id aunque el envío lo haya hecho el worker en segundo plano.
 */
@Entity(tableName = "pending_diagnosis")
data class PendingDiagnosisEntity(
    @PrimaryKey val localId: String,
    val userId: Int,
    val crop: String,
    val diseaseLabel: String,
    val confidence: Float,
    val latitude: Double?,
    val longitude: Double?,
    val createdAt: Long,
    val remoteId: Int? = null
)

@Dao
interface PendingDiagnosisDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(diagnosis: PendingDiagnosisEntity)

    @Query("SELECT * FROM pending_diagnosis WHERE localId = :localId")
    suspend fun get(localId: String): PendingDiagnosisEntity?

    @Query("SELECT * FROM pending_diagnosis WHERE userId = :userId AND remoteId IS NULL ORDER BY createdAt")
    suspend fun unsent(userId: Int): List<PendingDiagnosisEntity>

    @Query("UPDATE pending_diagnosis SET remoteId = :remoteId WHERE localId = :localId")
    suspend fun markSent(localId: String, remoteId: Int)

    @Query("DELETE FROM pending_diagnosis WHERE localId = :localId")
    suspend fun delete(localId: String)

    @Query("DELETE FROM pending_diagnosis WHERE remoteId IS NOT NULL AND createdAt < :before")
    suspend fun deleteSentBefore(before: Long)
}
