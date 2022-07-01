package zechs.drive.stream.data.remote

import retrofit2.http.Body
import retrofit2.http.POST
import zechs.drive.stream.data.model.AuthorizationResponse
import zechs.drive.stream.data.model.AuthorizationTokenRequest
import zechs.drive.stream.data.model.RefreshTokenRequest
import zechs.drive.stream.data.model.TokenResponse

interface TokenApi {

    @POST("/o/oauth2/token")
    suspend fun getAccessToken(
        @Body request: RefreshTokenRequest
    ): TokenResponse

    @POST("/o/oauth2/token")
    suspend fun getRefreshToken(
        @Body request: AuthorizationTokenRequest
    ): AuthorizationResponse

}