package zechs.drive.stream.data.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Keep
@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey val name: String,
    val clientId: String,
    val clientSecret: String,
    val redirectUri: String,
    val refreshToken: String,
    val accessToken: String
) {

    fun getDriveClient() = DriveClient(
        clientId = clientId,
        clientSecret = clientSecret,
        redirectUri = redirectUri,
        scopes = listOf("https://www.googleapis.com/auth/drive")
    )

    fun getAccessTokenResponse(): TokenResponse {
        val type = object : TypeToken<TokenResponse?>() {}.type
        return Gson().fromJson(accessToken, type)
    }

}
