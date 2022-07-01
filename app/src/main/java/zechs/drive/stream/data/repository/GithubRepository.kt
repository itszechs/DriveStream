package zechs.drive.stream.data.repository

import android.util.Log
import dagger.Lazy
import zechs.drive.stream.data.model.LatestRelease
import zechs.drive.stream.data.remote.GithubApi
import zechs.drive.stream.utils.state.Resource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GithubRepository @Inject constructor(
    private val githubApi: Lazy<GithubApi>
) {

    companion object {
        private const val TAG = "GithubRepository"
    }

    suspend fun getLatestRelease(): Resource<LatestRelease> {
        return try {
            val latest = githubApi.get().getLatestRelease()
            Log.d(TAG, latest.toString())
            Resource.Success(latest)
        } catch (e: Exception) {
            e.printStackTrace()
            val error = e.message ?: "An unknown error occurred."
            Log.d(TAG, error)
            return Resource.Error(error)
        }
    }

}
