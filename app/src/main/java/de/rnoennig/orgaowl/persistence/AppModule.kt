package de.rnoennig.orgaowl.persistence

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.rnoennig.orgaowl.ITaskRepository
import de.rnoennig.orgaowl.TaskRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java, "orgaowl",

            )
            .build()

    @Provides
    fun provideTaskDao(appDatabase: AppDatabase): TaskDao = appDatabase.taskDao()

    @Provides
    fun provideTasklistDao(appDatabase: AppDatabase): TasklistDao = appDatabase.tasklistDao()

    @Provides
    fun provideItemRepository(taskDao: TaskDao, tasklistDao: TasklistDao): ITaskRepository =
        TaskRepository(taskDao, tasklistDao)
}