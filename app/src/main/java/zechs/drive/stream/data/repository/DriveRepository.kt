package zechs.drive.stream.data.repository

import android.util.Log
import dagger.Lazy
import zechs.drive.stream.data.model.*
import zechs.drive.stream.data.remote.DriveApi
import zechs.drive.stream.data.remote.TokenApi
import zechs.drive.stream.utils.SessionManager
import zechs.drive.stream.utils.state.Resource
import zechs.drive.stream.utils.util.Constants.Companion.CLIENT_ID
import zechs.drive.stream.utils.util.Constants.Companion.CLIENT_SECRET
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DriveRepository @Inject constructor(
    private val driveApi: DriveApi,
    private val tokenApi: Lazy<TokenApi>,
    private val sessionManager: SessionManager
) {

    companion object {
        private const val TAG = "DriveRepository"
    }

    suspend fun getFiles(
        query: String,
        pageToken: String?,
        pageSize: Int,
    ): Resource<FilesResponse> {
        val tokenResponse = sessionManager.fetchAccessToken()
            ?: return Resource.Error("Access token can not be null")
        return try {
            val files = driveApi.getFiles(
                q = query,
                pageSize = pageSize,
                pageToken = pageToken,
                accessToken = "Bearer ${tokenResponse.accessToken}"
            )
            Resource.Success(files)
        } catch (e: Exception) {
            doOnError(e)
        }
    }

    suspend fun getDrives(
        pageToken: String?,
        pageSize: Int,
    ): Resource<DriveResponse> {
        val tokenResponse = sessionManager.fetchAccessToken()
            ?: return Resource.Error("Access token can not be null")
        return try {
            val drives = driveApi.getDrives(
                pageSize = pageSize,
                pageToken = pageToken,
                accessToken = "Bearer ${tokenResponse.accessToken}"
            )
            Resource.Success(drives)
        } catch (e: Exception) {
            doOnError(e)
        }
    }

    /**
     *
     * Retrieve access token from datastore.
     *
     * Note: Token is automatically refreshed
     * if it's expired.
     *
     * Note: Everytime token is refreshed it's
     * updated in datastore.
     *
     * @param forceRefresh forcefully refresh the token.
     *
     */
    suspend fun fetchAccessToken(
        forceRefresh: Boolean = false
    ): Resource<TokenResponse> {
        if (!forceRefresh) {
            val tokenResponse = sessionManager.fetchAccessToken()
            tokenResponse?.let {
                val currentTimeInSeconds = System.currentTimeMillis() / 1000
                if (currentTimeInSeconds >= it.expiresIn) {
                    Log.d(TAG, "Access token has expired. Trying to refresh")
                } else {
                    Log.d(TAG, "Access token is valid")
                    return Resource.Success(data = it)
                }
            }
        } else {
            Log.d(TAG, "Force refreshing access token")
        }

        val refreshToken = sessionManager.fetchRefreshToken()
            ?: return Resource.Error("Refresh token can not be null")

        return try {
            val token = tokenApi.get().getAccessToken(
                request = RefreshTokenRequest(
                    clientId = CLIENT_ID,
                    clientSecret = CLIENT_SECRET,
                    refreshToken = refreshToken
                )
            )
            Log.d(TAG, "Received access token (${token.accessToken})")
            sessionManager.saveAccessToken(token)
            Resource.Success(token)
        } catch (e: Exception) {
            doOnError(e)
        }
    }

    /**
     *
     * Exchange authorization code for refresh token.
     *
     * Note: Refresh token and access token both
     * are saved in datastore upon successfully request.
     *
     * @param authorizationCode Auth code received from Sign-in flow.
     *
     */
    suspend fun fetchRefreshToken(
        authorizationCode: String
    ): Resource<AuthorizationResponse> {
        return try {
            Log.d(TAG, "Requesting refresh token with authCode=$authorizationCode)")
            val token = tokenApi.get().getRefreshToken(
                request = AuthorizationTokenRequest(
                    clientId = CLIENT_ID,
                    clientSecret = CLIENT_SECRET,
                    authCode = authorizationCode
                )
            )

            Log.d(TAG, "Received refresh token (${token.refreshToken})")

            // saving in data store
            sessionManager.saveRefreshToken(token.refreshToken)
            sessionManager.saveAccessToken(token.toTokenResponse())

            Resource.Success(token)
        } catch (e: Exception) {
            doOnError(e)
        }
    }

    private fun <T> doOnError(e: Exception): Resource<T> {
        e.printStackTrace()
        val error = e.message ?: "An unknown error occurred."
        Log.d(TAG, error)
        return Resource.Error(error)
    }

}
