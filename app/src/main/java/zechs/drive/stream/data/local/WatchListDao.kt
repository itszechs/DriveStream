package zechs.drive.stream.data.local


import androidx.room.*
import zechs.drive.stream.data.model.WatchList

@Dao
interface WatchListDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWatch(watch: WatchList): Long

    @Query("SELECT * FROM `watch_list` WHERE videoId = :videoId LIMIT 1")
    suspend fun getWatch(videoId: String): WatchList?

    @Delete
    suspend fun deleteWatch(watch: WatchList)

}