package zechs.drive.stream.utils.util

import zechs.drive.stream.BuildConfig

class Constants {
    companion object {
        const val GOOGLE_API = "https://www.googleapis.com/"
        const val DRIVE_API = "${GOOGLE_API}/drive/v3"
        const val GOOGLE_ACCOUNTS_URL = "https://accounts.google.com"

        const val CLIENT_ID = BuildConfig.CLIENT_ID
        const val CLIENT_SECRET = BuildConfig.CLIENT_SECRET

        const val REDIRECT_URI = "urn:ietf:wg:oauth:2.0:oob"
        private const val SCOPES = "https://www.googleapis.com/auth/drive"

        const val AUTH_URL = "${GOOGLE_ACCOUNTS_URL}/o/oauth2/auth?" +
                "response_type=code&approval_prompt=force&access_type=offline" +
                "&client_id=${CLIENT_ID}&redirect_uri=${REDIRECT_URI}&scope=${SCOPES}"

    }
}