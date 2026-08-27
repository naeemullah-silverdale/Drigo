package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BookingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookingDao {
    @Query("SELECT * FROM bookings ORDER BY bookedAtTimestamp DESC")
    fun getAllBookings(): Flow<List<BookingEntity>>

    @Query("SELECT * FROM bookings WHERE passengerId = :passengerId ORDER BY bookedAtTimestamp DESC")
    fun getBookingsByPassenger(passengerId: String): Flow<List<BookingEntity>>

    @Query("SELECT * FROM bookings WHERE tripId = :tripId")
    fun getBookingsForTrip(tripId: String): Flow<List<BookingEntity>>

    @Query("SELECT * FROM bookings WHERE id = :bookingId LIMIT 1")
    fun getBookingById(bookingId: String): Flow<BookingEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: BookingEntity)

    @Update
    suspend fun updateBooking(booking: BookingEntity)

    @Query("UPDATE bookings SET status = :status WHERE id = :bookingId")
    suspend fun updateBookingStatus(bookingId: String, status: String)

    @Query("DELETE FROM bookings WHERE id = :bookingId")
    suspend fun cancelBooking(bookingId: String)
}
