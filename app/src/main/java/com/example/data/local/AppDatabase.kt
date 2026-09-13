package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.BlockedUserEntity
import com.example.data.model.BookingEntity
import com.example.data.model.ChatMessageEntity
import com.example.data.model.RideRatingEntity
import com.example.data.model.SafetyReportEntity
import com.example.data.model.TripEntity
import com.example.data.model.UserPreferenceEntity
import com.example.data.model.WalletEntity
import com.example.data.model.WalletTransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TripEntity::class,
        BookingEntity::class,
        ChatMessageEntity::class,
        UserPreferenceEntity::class,
        WalletEntity::class,
        WalletTransactionEntity::class,
        RideRatingEntity::class,
        SafetyReportEntity::class,
        BlockedUserEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun bookingDao(): BookingDao
    abstract fun chatDao(): ChatDao
    abstract fun preferenceDao(): PreferenceDao
    abstract fun walletDao(): WalletDao
    abstract fun safetyDao(): SafetyDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "citylink_rideshare.db"
                )
                .addCallback(DatabaseCallback(scope))
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }
        }

        suspend fun populateInitialData(db: AppDatabase) {
            db.tripDao().insertTrips(SampleDataProvider.initialTrips)
            db.preferenceDao().insertOrUpdate(SampleDataProvider.initialPreferences)
            db.bookingDao().insertBooking(SampleDataProvider.initialBooking)
            db.chatDao().insertMessages(SampleDataProvider.initialChatMessages)
        }
    }
}
