package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.BlockedUserEntity
import com.example.data.model.RideRatingEntity
import com.example.data.model.SafetyReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SafetyDao {

    // --- Ratings ---
    @Query("SELECT * FROM ride_ratings WHERE rideId = :rideId AND raterRole = :raterRole LIMIT 1")
    suspend fun getRatingForRide(rideId: String, raterRole: String): RideRatingEntity?

    @Query("SELECT * FROM ride_ratings WHERE rideId = :rideId")
    fun observeRatingsForRide(rideId: String): Flow<List<RideRatingEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM ride_ratings WHERE rideId = :rideId AND raterRole = :raterRole)")
    suspend fun hasRatedRide(rideId: String, raterRole: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM ride_ratings WHERE rideId = :rideId AND raterRole = :raterRole)")
    fun observeHasRatedRide(rideId: String, raterRole: String): Flow<Boolean>

    @Query("SELECT * FROM ride_ratings ORDER BY timestamp DESC")
    fun getAllRatings(): Flow<List<RideRatingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRating(rating: RideRatingEntity)

    // --- Safety Reports ---
    @Query("SELECT * FROM safety_reports ORDER BY timestamp DESC")
    fun getAllReports(): Flow<List<SafetyReportEntity>>

    @Query("SELECT * FROM safety_reports WHERE rideId = :rideId")
    fun getReportsForRide(rideId: String): Flow<List<SafetyReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: SafetyReportEntity)

    // --- Blocked Users ---
    @Query("SELECT * FROM blocked_users WHERE blockerUserId = :blockerUserId")
    fun getBlockedUsers(blockerUserId: String): Flow<List<BlockedUserEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_users WHERE blockerUserId = :blockerUserId AND blockedUserId = :blockedUserId)")
    suspend fun isUserBlocked(blockerUserId: String, blockedUserId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedUser(blocked: BlockedUserEntity)

    @Query("DELETE FROM blocked_users WHERE blockerUserId = :blockerUserId AND blockedUserId = :blockedUserId")
    suspend fun unblockUser(blockerUserId: String, blockedUserId: String)
}
