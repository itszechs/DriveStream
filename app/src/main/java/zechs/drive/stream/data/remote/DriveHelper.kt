package zechs.drive.stream.data.remote

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import zechs.drive.stream.R
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class DriveHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val TAG = "DriveHelper"
    }

    private var hasSignedIn = false

    suspend fun handleSignInResult(
        result: Intent
    ): Drive? = suspendCancellableCoroutine { cont ->
        GoogleSignIn.getSignedInAccountFromIntent(result)
            .addOnSuccessListener { googleAccount ->
                Log.v(TAG, "Signed in as " + googleAccount.email)

                val credential = GoogleAccountCredential.usingOAuth2(
                    /* context */ context,
                    /* scopes */ listOf(DriveScopes.DRIVE_FILE)
                ).apply { selectedAccount = googleAccount.account }

                val permission = hasPermission()
                Log.d(TAG, "hasPermission=$permission")

                if (permission) {
                    Log.d(TAG, "Building drive service...")
                    val driveBuilder = Drive.Builder(
                        /* HttpTransport */ AndroidHttp.newCompatibleTransport(),
                        /* jsonFactory */  GsonFactory(),
                        /* httpRequestInitializer */ credential
                    ).setApplicationName(context.getString(R.string.app_name)).build()

                    hasSignedIn = true
                    Log.d(TAG, "hasSignedIn=$hasSignedIn")
                    cont.resume(driveBuilder)
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

}