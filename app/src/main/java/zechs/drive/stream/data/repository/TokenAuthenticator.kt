package zechs.drive.stream.data.repository

import android.util.Log
import dagger.Lazy
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import zechs.drive.stream.utils.SessionManager
import zechs.drive.stream.utils.state.Resource
import javax.inject.Inject

class TokenAuthenticator @Inject constructor(
    private val driveRepository: Lazy<DriveRepository>,
    private val sessionManager: Lazy<SessionManager>
) : Authenticator {

    companion object {
        private const val TAG = "TokenAuthenticator"
    }

    override fun authenticate(
        route: Route?, response: Response
    ): Request? {

        val client = runBlocking {
            sessionManager.get().fetchClient()
        } ?: return null

        val tokenResponse = runBlocking {
            driveRepository.get().fetchAccessToken(client, forceRefresh = true)
        }

        if (tokenResponse is Resource.Success) {
            tokenResponse.data?.let { token ->
                val newAccessToken = token.accessToken
                Log.d(TAG, "Received new access token (token=$newAccessToken)")
                return response.request.newBuilder()
                    .removeHeader("Authorization")
                    .addHeader("Authorization", "Bearer $newAccessToken")
                    .url(response.request.url.toString())
                    .build()
            }
        } else {
            Log.d(TAG, tokenResponse.message!!)
        }

        return null
    }

}