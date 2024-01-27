package zechs.drive.stream.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import zechs.drive.stream.data.model.Account


@Database(
    entities = [Account::class],
    version = 1,
    exportSchema = false
)
abstract class AccountsDatabase : RoomDatabase() {

    abstract fun getAccountsDao(): AccountsDao

}