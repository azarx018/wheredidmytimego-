package com.timetrace.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.timetrace.app.data.local.entity.CodingSessionEntity
import com.timetrace.app.data.local.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goal WHERE enabled = 1")
    fun observeEnabled(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goal")
    suspend fun getAll(): List<GoalEntity>

    @Insert
    suspend fun insert(goal: GoalEntity): Long

    @Update
    suspend fun update(goal: GoalEntity)

    @Query("DELETE FROM goal WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM goal")
    suspend fun deleteAll()
}

@Dao
interface CodingSessionDao {
    @Query("SELECT * FROM coding_session WHERE endTimeMillis IS NULL LIMIT 1")
    suspend fun getActiveSession(): CodingSessionEntity?

    @Insert
    suspend fun insert(session: CodingSessionEntity): Long

    @Update
    suspend fun update(session: CodingSessionEntity)

    @Query("SELECT * FROM coding_session WHERE startTimeMillis >= :sinceMillis ORDER BY startTimeMillis DESC")
    fun observeSince(sinceMillis: Long): Flow<List<CodingSessionEntity>>

    @Query("DELETE FROM coding_session")
    suspend fun deleteAll()
}
