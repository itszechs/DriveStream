package zechs.drive.stream.data.remote

import retrofit2.http.GET
import zechs.drive.stream.data.model.LatestRelease

interface GithubApi {

    @GET("repos/itszechs/DriveStream/releases/latest")
    suspend fun getLatestRelease(): LatestRelease

}