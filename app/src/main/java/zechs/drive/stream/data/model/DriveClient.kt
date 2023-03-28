package zechs.drive.stream.data.model

import android.net.Uri
import androidx.annotation.Keep
import zechs.drive.stream.utils.util.Constants.Companion.GOOGLE_ACCOUNTS_URL
import java.io.Serializable

@Keep
data class DriveClient(
    val clientId: String,
    val clientSecret: String,
    val redirectUri: String,
    val scopes: List<String>
) : Serializable {

    fun authUrl(): Uri? = try {
        Uri.parse(
            "${GOOGLE_ACCOUNTS_URL}/o/oauth2/auth?" +
                    "response_type=code&approval_prompt=force&access_type=offline" +
                    "&client_id=${clientId}&redirect_uri=${redirectUri}&scope=${scopes.joinToString(" ")}"
        )
    } catch (e: Exception) {
        null
    }
}
