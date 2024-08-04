package de.rnoennig.orgaowl.persistence

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [Task::class, Tasklist::class],
    version = 1,
    autoMigrations = [
    ],
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun tasklistDao(): TasklistDao
}