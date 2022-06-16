package zechs.drive.stream.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import zechs.drive.stream.data.remote.DriveHelper
import zechs.drive.stream.utils.SessionManager
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideDriveHelper(
        @ApplicationContext appContext: Context,
        sessionManager: SessionManager
    ): DriveHelper = DriveHelper(appContext, sessionManager)


    @Singleton
    @Provides
    fun provideSessionDataStore(
        @ApplicationContext appContext: Context
    ): SessionManager = SessionManager(appContext)

}