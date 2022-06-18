package zechs.drive.stream.ui.player.utils

import android.net.Uri
import com.google.android.exoplayer2.upstream.*
import com.google.android.exoplayer2.util.Assertions
import com.google.android.exoplayer2.util.Log
import zechs.drive.stream.data.remote.DriveHelper
import zechs.drive.stream.ui.player.PlayerActivity.Companion.TAG
import java.io.IOException


class AuthenticatingDataSource(
    private val wrappedDataSource: DefaultHttpDataSource,
    private val driveHelper: DriveHelper
) : DataSource {

    class Factory(
        private val wrappedFactory: DefaultHttpDataSource.Factory,
        private val driveHelper: DriveHelper
    ) : DataSource.Factory {
        override fun createDataSource(): AuthenticatingDataSource {
            return AuthenticatingDataSource(
                wrappedFactory.createDataSource(),
                driveHelper
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
            if (e.responseCode == 401) {
                // Token expired, trying to refresh it
                val token = driveHelper.getAccessToken(true)
                wrappedDataSource.setRequestProperty("Authorization", "Bearer $token")
                Log.d(TAG, "Token expired, refreshing...")
                Log.d(TAG, "ACCESS_TOKEN=$token")
            }
            if (e.responseCode == 403) {
                // Unauthorized
                val token = driveHelper.getAccessToken()
                wrappedDataSource.setRequestProperty("Authorization", "Bearer $token")
                Log.d(TAG, "Unauthorized, attaching token...")
                Log.d(TAG, "ACCESS_TOKEN=$token")
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