package zechs.drive.stream.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import zechs.drive.stream.data.model.WatchList


@Database(
    entities = [WatchList::class],
    version = 1,
    exportSchema = false
)
abstract class WatchListDatabase : RoomDatabase() {

    abstract fun getWatchListDao(): WatchListDao

}