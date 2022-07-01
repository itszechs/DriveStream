package zechs.drive.stream.data.model

import androidx.annotation.Keep
import com.squareup.moshi.Json
import zechs.drive.stream.utils.util.Constants.Companion.REDIRECT_URI

@Keep
data class RefreshTokenRequest(
    @Json(name = "client_id")
    val clientId: String,
    @Json(name = "client_secret")
    val clientSecret: String,
    @Json(name = "refresh_token")
    val refreshToken: String,
    @Json(name = "grant_type")
    val grantType: String = "refresh_token"
)

@Keep
data class AuthorizationTokenRequest(
    @Json(name = "client_id")
    val clientId: String,
    @Json(name = "client_secret")
    val clientSecret: String,
    @Json(name = "redirect_uri")
    val redirectUri: String = REDIRECT_URI,
    @Json(name = "code")
    val authCode: String,
    @Json(name = "grant_type")
    val grantType: String = "authorization_code"
)


