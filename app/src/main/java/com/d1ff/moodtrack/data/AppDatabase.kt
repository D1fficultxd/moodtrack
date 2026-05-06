package com.d1ff.moodtrack.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [DailyEntry::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dailyEntryDao(): DailyEntryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE daily_entries ADD COLUMN mood INTEGER NOT NULL DEFAULT 5")
                db.execSQL("ALTER TABLE daily_entries ADD COLUMN apathy INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE daily_entries ADD COLUMN fatigue INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE daily_entries ADD COLUMN lossOfInterest INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE daily_entries ADD COLUMN hopelessness INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "moodtrack_database"
                )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration() // Just in case migration fails
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
