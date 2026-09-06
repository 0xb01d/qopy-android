package com.qopy.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PeerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(peer: PeerEntity)

    @Query("SELECT * FROM peers WHERE device_id = :deviceId LIMIT 1")
    suspend fun getPeer(deviceId: String): PeerEntity?

    @Query("SELECT * FROM peers ORDER BY updated_at DESC")
    fun getAllPeersFlow(): Flow<List<PeerEntity>>

    @Query("SELECT * FROM peers ORDER BY updated_at DESC")
    suspend fun getAllPeers(): List<PeerEntity>

    @Query("DELETE FROM peers WHERE device_id = :deviceId")
    suspend fun deletePeer(deviceId: String): Int
}
