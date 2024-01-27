package zechs.drive.stream.data.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "clients")
data class Client(
    @PrimaryKey val id: String,
    val secret: String,
    val redirectUri: String
) {
    fun isEmpty() = id.isEmpty() || secret.isEmpty() || redirectUri.isEmpty()
}

@Entity(
    tableName = "accounts",
    foreignKeys = [ForeignKey(
        entity = Client::class,
        parentColumns = ["id"],
        childColumns = ["clientId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("clientId")]
)
data class Account(
    @PrimaryKey val name: String,
    val refreshToken: String,
    val accessToken: String,
    val clientId: String
)

@Keep
data class AccountWithClient(
    val name: String,
    val clientId: String,
    val clientSecret: String,
    val redirectUri: String,
    val refreshToken: String,
    val accessToken: String
) {

    @Ignore
    var isDefault: Boolean = false

    fun getDriveClient() = DriveClient(
        clientId = clientId,
        clientSecret = clientSecret,
        redirectUri = redirectUri,
        scopes = listOf("https://www.googleapis.com/auth/drive")
    )

    fun getAccessTokenResponse(): TokenResponse {
        val type = object : TypeToken<TokenResponse>() {}.type
        return Gson().fromJson(accessToken, type)
    }

}
