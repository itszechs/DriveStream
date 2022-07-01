package zechs.drive.stream.data.model

import androidx.annotation.Keep
import com.squareup.moshi.Json

@Keep
data class TokenResponse(
    @Json(name = "access_token")
    val accessToken: String,
    @Json(name = "expires_in")
    val expiresIn: Long,
    @Json(name = "token_type")
    val tokenType: String,
    val scope: String
)

@Keep
data class AuthorizationResponse(
    @Json(name = "access_token")
    val accessToken: String,
    @Json(name = "expires_in")
    val expiresIn: Long,
    @Json(name = "refresh_token")
    val refreshToken: String,
    @Json(name = "token_type")
    val tokenType: String,
    val scope: String
) {

    fun toTokenResponse() = TokenResponse(
        accessToken, expiresIn, tokenType, scope
    )

}