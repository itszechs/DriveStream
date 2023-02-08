package zechs.drive.stream.data.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey

@Keep
@Entity(tableName = "watch_list")
data class WatchList(
    val name: String,
    val videoId: String,
    val watchedDuration: Long,
    val totalDuration: Long,
    @PrimaryKey(autoGenerate = true) val id: Int? = null
) {

    fun watchProgress(): Int {
        val progress = ((watchedDuration.toDouble() / totalDuration) * 100)
        return progress.toInt()
    }

    /*
     * If video is watched more than 95% then we can say its watched
     */
    fun hasFinished() = watchProgress() > 95
}

