package zechs.drive.stream.data.local


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import zechs.drive.stream.data.model.Account

@Dao
interface AccountsDao {

    @Query("SELECT * FROM `accounts`")
    suspend fun getAccounts(): List<Account>

    @Query("SELECT * FROM `accounts` WHERE name = :name LIMIT 1")
    suspend fun getAccount(name: String): Account?

    @Query("UPDATE `accounts` SET accessToken = :accessToken WHERE refreshToken = :refreshToken")
    suspend fun updateAccessToken(refreshToken: String, accessToken: String)

    @Query("DELETE FROM `accounts` WHERE name = :name")
    suspend fun deleteAccount(name: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addAccount(account: Account): Long

}