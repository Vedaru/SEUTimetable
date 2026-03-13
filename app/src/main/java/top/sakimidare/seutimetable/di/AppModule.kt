package top.sakimidare.seutimetable.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Singleton
import top.sakimidare.seutimetable.data.local.AppDatabase
import top.sakimidare.seutimetable.data.local.CourseDao
import top.sakimidare.seutimetable.data.local.TableDao
import top.sakimidare.seutimetable.data.repository.CourseRepository
import top.sakimidare.seutimetable.data.repository.UserPreferenceRepository

/**
 * Dependency injection module for core app components.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideCourseDao(database: AppDatabase): CourseDao = database.courseDao()

    @Provides
    fun provideTableDao(database: AppDatabase): TableDao = database.tableDao()

    @Provides
    @Singleton
    fun provideCourseRepository(
        courseDao: CourseDao,
        tableDao: TableDao,
        @ApplicationContext context: Context
    ): CourseRepository = CourseRepository(courseDao, tableDao, context)

    @Provides
    @Singleton
    fun provideUserPreferenceRepository(@ApplicationContext context: Context): UserPreferenceRepository =
        UserPreferenceRepository(context)
}
