package zechs.drive.stream.ui.player.utils

import android.net.Uri
import com.google.android.exoplayer2.upstream.*
import com.google.android.exoplayer2.util.Assertions
import com.google.android.exoplayer2.util.Log
import kotlinx.coroutines.runBlocking
import zechs.drive.stream.data.repository.DriveRepository
import zechs.drive.stream.ui.player.PlayerActivity.Companion.TAG
import zechs.drive.stream.utils.SessionManager
import zechs.drive.stream.utils.state.Resource
import java.io.IOException


class AuthenticatingDataSource(
    private val wrappedDataSource: DefaultHttpDataSource,
    private val driveRepository: DriveRepository,
    private val sessionManager: SessionManager
) : DataSource {

    class Factory(
        private val wrappedFactory: DefaultHttpDataSource.Factory,
        private val driveRepository: DriveRepository,
        private val sessionManager: SessionManager
    ) : DataSource.Factory {
        override fun createDataSource(): AuthenticatingDataSource {
            return AuthenticatingDataSource(
                wrappedFactory.createDataSource(),
                driveRepository,
                sessionManager
            )
        }
    }

    private var upstreamOpened = false

    override fun addTransferListener(transferListener: TransferListener) {
        Assertions.checkNotNull(transferListener)
        wrappedDataSource.addTransferListener(transferListener)
    }

    @Throws(IOException::class)
    override fun open(dataSpec: DataSpec): Long {
        upstreamOpened = true
        return try {
            wrappedDataSource.open(dataSpec)
        } catch (e: HttpDataSource.InvalidResponseCodeException) {
            val client = runBlocking { sessionManager.fetchClient()!! }
            if (e.responseCode == 401) {
                // Token expired, trying to refresh it
                val token = runBlocking {
                    driveRepository.fetchAccessToken(client, forceRefresh = true)
                }
                if (token is Resource.Success) {
                    val accessToken = token.data!!.accessToken
                    wrappedDataSource.setRequestProperty("Authorization", "Bearer $accessToken")
                    Log.d(TAG, "Token expired, refreshing...")
                    Log.d(TAG, "ACCESS_TOKEN=$accessToken")
                } else {
                    Log.d(TAG, "Unable to refresh access token")
                    Log.d(TAG, token.message!!)
                }
            }
            if (e.responseCode == 403) {
                // Unauthorized
                val token = runBlocking {
                    driveRepository.fetchAccessToken(client)
                }
                if (token is Resource.Success) {
                    val accessToken = token.data!!.accessToken
                    wrappedDataSource.setRequestProperty("Authorization", "Bearer $accessToken")
                    Log.d(TAG, "Unauthorized, attaching token...")
                    Log.d(TAG, "ACCESS_TOKEN=$accessToken")
                } else {
                    Log.d(TAG, "Unable to fetch access token")
                    Log.d(TAG, token.message!!)
                }
            }
            wrappedDataSource.open(dataSpec)
        }
    }

    @Throws(IOException::class)
    override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int {
        return wrappedDataSource.read(buffer, offset, readLength)
    }

    override fun getUri(): Uri? {
        return wrappedDataSource.uri
    }

    override fun getResponseHeaders(): Map<String, List<String>> {
        return wrappedDataSource.responseHeaders
    }

    @Throws(IOException::class)
    override fun close() {
        if (upstreamOpened) {
            upstreamOpened = false
            wrappedDataSource.close()
        }
    }
}