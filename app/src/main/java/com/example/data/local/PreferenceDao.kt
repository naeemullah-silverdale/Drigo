package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.UserPreferenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PreferenceDao {
    @Query("SELECT * FROM user_preferences WHERE userId = :userId LIMIT 1")
    fun getUserPreferences(userId: String = "current_user"): Flow<UserPreferenceEntity?>

    @Query("SELECT * FROM user_preferences WHERE userId = :userId LIMIT 1")
    suspend fun getUserPreferencesDirect(userId: String = "current_user"): UserPreferenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(preferences: UserPreferenceEntity)

    @Update
    suspend fun updatePreferences(preferences: UserPreferenceEntity)

    @Query("UPDATE user_preferences SET isDriverMode = :isDriver WHERE userId = :userId")
    suspend fun setDriverMode(userId: String = "current_user", isDriver: Boolean)

    @Query("UPDATE user_preferences SET totalCo2SavedKg = totalCo2SavedKg + :co2Kg, totalMoneySavedUsd = totalMoneySavedUsd + :moneySaved WHERE userId = :userId")
    suspend fun addImpact(userId: String = "current_user", co2Kg: Double, moneySaved: Double)
}
