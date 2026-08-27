package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.LuggageAllowance
import com.example.data.model.RecurringFrequency
import com.example.data.model.TripStatus
import com.example.data.model.BookingStatus

class Converters {
    @TypeConverter
    fun fromTripStatus(status: TripStatus): String = status.name

    @TypeConverter
    fun toTripStatus(value: String): TripStatus = try {
        TripStatus.valueOf(value)
    } catch (e: Exception) {
        TripStatus.SCHEDULED
    }

    @TypeConverter
    fun fromRecurring(freq: RecurringFrequency): String = freq.name

    @TypeConverter
    fun toRecurring(value: String): RecurringFrequency = try {
        RecurringFrequency.valueOf(value)
    } catch (e: Exception) {
        RecurringFrequency.NONE
    }

    @TypeConverter
    fun fromLuggage(luggage: LuggageAllowance): String = luggage.name

    @TypeConverter
    fun toLuggage(value: String): LuggageAllowance = try {
        LuggageAllowance.valueOf(value)
    } catch (e: Exception) {
        LuggageAllowance.MEDIUM
    }

    @TypeConverter
    fun fromBookingStatus(status: BookingStatus): String = status.name

    @TypeConverter
    fun toBookingStatus(value: String): BookingStatus = try {
        BookingStatus.valueOf(value)
    } catch (e: Exception) {
        BookingStatus.CONFIRMED
    }
}
