package zechs.drive.stream.data.local


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import zechs.drive.stream.data.model.Account
import zechs.drive.stream.data.model.AccountWithClient
import zechs.drive.stream.data.model.Client

@Dao
interface AccountsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addClient(client: Client)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addAccount(account: Account)

    @Transaction
    @Query("DELETE FROM clients WHERE id = :clientId")
    suspend fun deleteClient(clientId: String)

    @Query("SELECT * FROM clients")
    fun getClients(): Flow<List<Client>>

    @Transaction
    @Query(
        "SELECT accounts.*, clients.secret AS clientSecret, clients.redirectUri " +
                "FROM accounts JOIN clients ON accounts.clientId = clients.id"
    )
    fun getAccounts(): Flow<List<AccountWithClient>>

    @Transaction
    @Query(
        "SELECT accounts.*, clients.secret AS clientSecret, clients.redirectUri " +
                "FROM accounts JOIN clients ON accounts.clientId = clients.id " +
                "WHERE accounts.name = :accountName"
    )
    suspend fun getAccount(accountName: String): AccountWithClient?

    @Query("UPDATE accounts SET name = :newName WHERE name = :oldName")
    suspend fun updateAccountName(oldName: String, newName: String)

    @Query("DELETE FROM accounts WHERE name = :accountName")
    suspend fun deleteAccount(accountName: String)

    @Transaction
    @Query("UPDATE clients SET secret = :secret, redirectUri = :redirectUri WHERE id = :clientId")
    suspend fun updateClient(clientId: String, secret: String, redirectUri: String)

    @Query("UPDATE accounts SET accessToken = :newToken WHERE name = :accountName")
    suspend fun updateAccessToken(accountName: String, newToken: String)

}