package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.EventDao
import com.example.data.dao.GroupDao
import com.example.data.dao.MoodDao
import com.example.data.dao.UserDao
import com.example.data.model.EventEntity
import com.example.data.model.GroupEntity
import com.example.data.model.MoodPulseEntity
import com.example.data.model.UserProfileEntity

@Database(
    entities = [
        GroupEntity::class,
        EventEntity::class,
        MoodPulseEntity::class,
        UserProfileEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun groupDao(): GroupDao
    abstract fun eventDao(): EventDao
    abstract fun moodDao(): MoodDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gapoo_database.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
