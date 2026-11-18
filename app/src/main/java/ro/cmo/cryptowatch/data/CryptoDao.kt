package ro.cmo.cryptowatch.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CryptoDao {
    @Query("SELECT * FROM crypto_table")
    fun getAllCryptos(): Flow<List<CryptoEntity>>

    @Query("SELECT * FROM crypto_table WHERE isFavorite = 1")
    fun getFavorites(): Flow<List<CryptoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCryptos(cryptos: List<CryptoEntity>)

    @Query("UPDATE crypto_table SET isFavorite = :isFav WHERE id = :cryptoId")
    suspend fun updateFavoriteStatus(cryptoId: String, isFav: Boolean)
}
