package zechs.drive.stream.data.remote

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import zechs.drive.stream.R
import zechs.drive.stream.utils.SessionManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class DriveHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionManager: SessionManager
) {

    companion object {
        const val TAG = "DriveHelper"
    }

    var drive: Drive? = null
        private set

    var hasSignedIn = false
        private set

    /**
     * Public method to send intent result from
     * Google Sign-in flow
     */
    suspend fun handleSignIn(result: Intent) {
        val drive = handleSignInResult(result)
        setDrive(drive)
    }

    private suspend fun handleSignInResult(
        result: Intent
    ): Drive? = suspendCancellableCoroutine { cont ->
        GoogleSignIn.getSignedInAccountFromIntent(result)
            .addOnSuccessListener { googleAccount ->
                Log.v(TAG, "Signed in as " + googleAccount.email)

                val credential = GoogleAccountCredential.usingOAuth2(
                    /* context */ context,
                    /* scopes */ listOf(DriveScopes.DRIVE)
                ).apply { selectedAccount = googleAccount.account }

                val permission = hasPermission()
                Log.d(TAG, "hasPermission=$permission")

                if (permission) {
                    Log.d(TAG, "Building drive service...")
                    val driveBuilder = Drive.Builder(
                        /* HttpTransport */ AndroidHttp.newCompatibleTransport(),
                        /* jsonFactory */  GsonFactory(),
                        /* httpRequestInitializer */ credential
                    ).setApplicationName(
                        context.getString(R.string.app_name)
                    ).build()

                    hasSignedIn = true
                    Log.d(TAG, "hasSignedIn=$hasSignedIn")

                    cont.resume(driveBuilder)

                    if (driveBuilder != null) {
                        Log.d(TAG, "Drive service built successfully")
                    }
                } else {
                    cont.resume(null)
                    Log.d(TAG, "Required permissions not granted")
                }
            }
            .addOnFailureListener { e ->
                hasSignedIn = false
                Log.e(TAG, "Unable to sign in.", e)
                cont.resume(null)
            }
    }

    private fun hasPermission(): Boolean {
        return GoogleSignIn.hasPermissions(
            GoogleSignIn.getLastSignedInAccount(context),
            Scope(DriveScopes.DRIVE)
        )
    }

    private fun setDrive(drive: Drive?) {
        this.drive = drive
    }

    /**
     * Singular point to get access token
     *
     */
    fun getAccessToken(refresh: Boolean = false): String? {
        var accessToken: String?
        val token = CoroutineScope(Dispatchers.IO).async {
            val fetchAccessToken = sessionManager.fetchAccessToken()
            if (fetchAccessToken != null && !refresh) {
                Log.d(TAG, "Access token acquired")
                return@async fetchAccessToken
            } else {
                Log.d(TAG, "Attempting to refresh access token")
                val requestAccessToken = requestAccessToken()
                if (requestAccessToken != null) {
                    Log.d(TAG, "Access token refreshed successfully")
                    sessionManager.saveAccessToken(requestAccessToken)
                    return@async requestAccessToken
                }
                Log.d(TAG, "Unable to refresh access token")
            }
            Log.d(TAG, "Access token not found")
            return@async null
        }
        runBlocking {
            accessToken = token.await()
        }
        return accessToken
    }

    private suspend fun requestAccessToken(): String? =
        suspendCancellableCoroutine { cont ->
            try {
                GoogleSignIn.getLastSignedInAccount(context)?.let {
                    it.account?.let { account ->
                        GoogleAuthUtil.getToken(
                            context, account,
                            /* scope */ "oauth2:${DriveScopes.DRIVE}"
                        ).also { t -> cont.resume(t) }
                    } ?: cont.resume(null)
                } ?: cont.resume(null)
            } catch (e: Exception) {
                cont.resume(null)
                Log.d(TAG, "requestAccessToken: ${e.message.toString()}")
            }
        }

}