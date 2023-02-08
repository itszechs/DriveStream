package zechs.drive.stream.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import zechs.drive.stream.data.local.WatchListDao
import zechs.drive.stream.data.local.WatchListDatabase
import zechs.drive.stream.data.repository.WatchListRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val WATCHLIST_DATABASE_NAME = "watch_list.db"

    @Singleton
    @Provides
    fun provideWatchListDatabase(
        @ApplicationContext appContext: Context
    ) = Room.databaseBuilder(
        appContext,
        WatchListDatabase::class.java,
        WATCHLIST_DATABASE_NAME
    ).build()

    @Singleton
    @Provides
    fun provideWatchListDao(
        db: WatchListDatabase
    ): WatchListDao {
        return db.getWatchListDao()
    }

    @Singleton
    @Provides
    fun provideWatchListRepository(
        watchListDao: WatchListDao
    ) = WatchListRepository(watchListDao)

}