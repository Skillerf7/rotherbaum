package com.rotherbaum.player.data

import android.content.Context
import androidx.room.*

@Entity(tableName = "favorites")
data class FavoriteEntity(@PrimaryKey val trackId: Long)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "trackId"]
)
data class PlaylistTrackEntity(
    val playlistId: Long,
    val trackId: Long,
    val position: Int
)

@Entity(tableName = "eq_presets")
data class EqPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val preampDb: Float,
    // Bandgains als "freq:gain:q,freq:gain:q,..." serialisiert - kein
    // extra JSON-Abhängigkeit für so eine einfache Liste nötig.
    val bandsSerialized: String,
    val bassPercent: Float,
    val treblePercent: Float,
    val limiterEnabled: Boolean
)

@Entity(tableName = "play_history")
data class PlayHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: Long,
    val playedAtSec: Long
)

@Entity(tableName = "ratings")
data class RatingEntity(
    @PrimaryKey val trackId: Long,
    val rating: Int // -1 = schlecht, 0 = neutral, 1 = gut
)

@Dao
interface FavoriteDao {
    @Query("SELECT trackId FROM favorites")
    suspend fun getAll(): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(fav: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE trackId = :trackId")
    suspend fun remove(trackId: Long)
}

@Dao
interface PlaylistDao {
    @Insert
    suspend fun createPlaylist(playlist: PlaylistEntity): Long

    @Query("SELECT * FROM playlists ORDER BY name ASC")
    suspend fun getPlaylists(): List<PlaylistEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTrack(entry: PlaylistTrackEntity)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrack(playlistId: Long, trackId: Long)

    @Query("SELECT trackId FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun getTrackIds(playlistId: Long): List<Long>

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)
}

@Dao
interface EqPresetDao {
    @Insert
    suspend fun save(preset: EqPresetEntity): Long

    @Query("SELECT * FROM eq_presets ORDER BY name ASC")
    suspend fun getAll(): List<EqPresetEntity>

    @Query("DELETE FROM eq_presets WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface PlayHistoryDao {
    @Insert
    suspend fun record(entry: PlayHistoryEntity)

    @Query("""
        SELECT trackId FROM play_history
        ORDER BY playedAtSec DESC
        LIMIT 200
    """)
    suspend fun recentlyPlayedTrackIds(): List<Long>

    @Query("""
        SELECT trackId FROM play_history
        GROUP BY trackId
        ORDER BY COUNT(*) DESC
        LIMIT 200
    """)
    suspend fun mostPlayedTrackIds(): List<Long>
}

@Dao
interface RatingDao {
    @Query("SELECT * FROM ratings")
    suspend fun getAll(): List<RatingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setRating(rating: RatingEntity)
}

@Database(
    entities = [
        FavoriteEntity::class, PlaylistEntity::class, PlaylistTrackEntity::class,
        EqPresetEntity::class, PlayHistoryEntity::class, RatingEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class RotherbaumDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun eqPresetDao(): EqPresetDao
    abstract fun playHistoryDao(): PlayHistoryDao
    abstract fun ratingDao(): RatingDao

    companion object {
        @Volatile private var instance: RotherbaumDatabase? = null

        fun get(context: Context): RotherbaumDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    RotherbaumDatabase::class.java,
                    "rotherbaum.db"
                ).build().also { instance = it }
            }
    }
}
