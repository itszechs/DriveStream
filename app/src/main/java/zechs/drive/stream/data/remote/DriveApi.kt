package zechs.drive.stream.data.remote

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import zechs.drive.stream.data.model.DriveResponse
import zechs.drive.stream.data.model.FilesResponse

interface DriveApi {

    @GET("/drive/v3/files")
    suspend fun getFiles(
        @Header("Authorization")
        accessToken: String,
        @Query("supportsAllDrives")
        supportsAllDrives: Boolean = true,
        @Query("includeItemsFromAllDrives")
        includeItemsFromAllDrives: Boolean = true,
        @Query("pageSize")
        pageSize: Int = 25,
        @Query("pageToken")
        pageToken: String? = null,
        @Query("fields")
        fields: String = "nextPageToken, files(id, name, size, mimeType, iconLink)",
        @Query("orderBy")
        orderBy: String = "folder, name",
        @Query("q")
        q: String
    ): FilesResponse

    @GET("/drive/v3/drives")
    suspend fun getDrives(
        @Header("Authorization")
        accessToken: String,
        @Query("pageSize")
        pageSize: Int = 25,
        @Query("pageToken")
        pageToken: String? = null,
        @Query("fields")
        fields: String = "nextPageToken, drives(id, name, kind)"
    ): DriveResponse

}