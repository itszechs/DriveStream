package zechs.drive.stream.data.model

import androidx.annotation.Keep
import com.squareup.moshi.Json
import zechs.drive.stream.BuildConfig

@Keep
data class LatestRelease(
    val name: String,
    @Json(name = "tag_name")
    val tagName: String,
    @Json(name = "html_url")
    val htmlUrl: String
) {

    fun isLatest() = tagName == BuildConfig.VERSION_NAME

}