package zechs.drive.stream.data.repository

import zechs.drive.stream.data.local.WatchListDao
import zechs.drive.stream.data.model.WatchList
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchListRepository @Inject constructor(
    private val watchListDao: WatchListDao
) {

    suspend fun insertWatch(
        watchList: WatchList
    ) = watchListDao.upsertWatch(watchList)

    suspend fun getWatch(videoId: String): WatchList? {
        return watchListDao.getWatch(videoId)
    }

    suspend fun deleteWatch(
        watch: WatchList
    ) = watchListDao.deleteWatch(watch)

}