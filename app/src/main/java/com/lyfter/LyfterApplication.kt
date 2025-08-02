package com.lyfter

import android.app.Application
import androidx.room.Room
import com.lyfter.data.AppDatabase
import com.lyfter.repository.WorkoutRepository

class LyfterApplication : Application() {
    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "lyfter-database"
        ).fallbackToDestructiveMigration()
        .build()
    }

    val repository: WorkoutRepository by lazy {
        WorkoutRepository(database)
    }
}
