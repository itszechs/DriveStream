package zechs.drive.stream.data.repository

import android.util.Log
import dagger.Lazy
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import zechs.drive.stream.utils.state.Resource
import javax.inject.Inject

class TokenAuthenticator @Inject constructor(
    private val driveRepository: Lazy<DriveRepository>
) : Authenticator {

    companion object {
        private const val TAG = "TokenAuthenticator"
    }

    override fun authenticate(
        route: Route?, response: Response
    ): Request? {

        val tokenResponse = runBlocking {
            driveRepository.get().fetchAccessToken(forceRefresh = true)
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