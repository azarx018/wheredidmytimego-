package com.timetrace.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.timetrace.app.data.local.entity.AppCategoryEntity
import com.timetrace.app.data.local.entity.AppInfoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppCategoryDao {
    @Query("SELECT * FROM app_category ORDER BY name ASC")
    fun observeAll(): Flow<List<AppCategoryEntity>>

    @Query("SELECT * FROM app_category ORDER BY name ASC")
    suspend fun getAll(): List<AppCategoryEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<AppCategoryEntity>)

    @Insert
    suspend fun insert(category: AppCategoryEntity): Long

    @Query("SELECT COUNT(*) FROM app_category")
    suspend fun count(): Int
}

@Dao
interface AppInfoDao {
    @Query("SELECT * FROM app_info WHERE packageName = :packageName")
    suspend fun get(packageName: String): AppInfoEntity?

    @Query("SELECT * FROM app_info")
    fun observeAll(): Flow<List<AppInfoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(appInfo: AppInfoEntity)

    @Update
    suspend fun update(appInfo: AppInfoEntity)

    @Query("DELETE FROM app_info")
    suspend fun deleteAll()

    @Query("SELECT packageName FROM app_info WHERE isCodingApp = 1")
    suspend fun getCodingAppPackages(): List<String>
}
