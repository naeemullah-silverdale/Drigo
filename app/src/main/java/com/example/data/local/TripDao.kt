package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TripEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Query("SELECT * FROM trips ORDER BY createdAtTimestamp DESC")
    fun getAllTrips(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :tripId LIMIT 1")
    fun getTripById(tripId: String): Flow<TripEntity?>

    @Query("SELECT * FROM trips WHERE id = :tripId LIMIT 1")
    suspend fun getTripDirect(tripId: String): TripEntity?

    @Query("SELECT * FROM trips WHERE driverId = :driverId ORDER BY createdAtTimestamp DESC")
    fun getTripsByDriver(driverId: String): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE recurringFrequency != 'NONE' ORDER BY createdAtTimestamp DESC")
    fun getRecurringTrips(): Flow<List<TripEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrips(trips: List<TripEntity>)

    @Update
    suspend fun updateTrip(trip: TripEntity)

    @Query("UPDATE trips SET availableSeats = :newAvailableSeats WHERE id = :tripId")
    suspend fun updateAvailableSeats(tripId: String, newAvailableSeats: Int)

    @Query("UPDATE trips SET status = :status WHERE id = :tripId")
    suspend fun updateTripStatus(tripId: String, status: String)

    @Query("DELETE FROM trips WHERE id = :tripId")
    suspend fun deleteTrip(tripId: String)
}
