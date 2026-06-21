package org.tainorazum.universum.lobby.database

import org.bukkit.Location
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.tainorazum.universum.utility.DatabaseUtil.dba

object DB_Lobby : Table("lobby") {

    val spawnpoint_world = varchar("spawnpoint_world", 255)

    val spawnpoint_x = double("spawnpoint_x")
    val spawnpoint_y = double("spawnpoint_y")
    val spawnpoint_z = double("spawnpoint_z")
    val spawnpoint_pitch = float("spawnpoint_pitch")
    val spawnpoint_yaw = float("spawnpoint_yaw")

    override val primaryKey = PrimaryKey(spawnpoint_world, spawnpoint_x, spawnpoint_y, spawnpoint_z, spawnpoint_pitch, spawnpoint_yaw)

    /**
     * Cached lobby world name. Loaded once at startup.
     */
    @Volatile
    private var cachedLobbyWorld: String? = null
    private val cacheLock = Any()

    /**
     * Load lobby world from DB (blocking). Called once at startup.
     * Returns true if successfully loaded and cached, false if no lobby row found.
     */
    fun loadAndCacheBoundary(): Boolean {
        return try {
            var world: String? = null
            dba {
                transaction {
                    val row = DB_Lobby.selectAll()
                        .limit(1)
                        .firstOrNull()

                    if (row != null) {
                        world = row[spawnpoint_world]
                    }
                }
            }.get() // block and wait for completion

            if (world != null) {
                synchronized(cacheLock) {
                    cachedLobbyWorld = world
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Fast, synchronous check: is location in the lobby world?
     * If cache is not loaded, returns false. Must call loadAndCacheBoundary() at startup.
     */
    fun isLocationInLobby(location: Location): Boolean {
        val lobbyWorld = cachedLobbyWorld ?: return false
        return location.world?.name == lobbyWorld
    }

    /**
     * Invalidate cache (e.g., if lobby was moved to a different world).
     * Next access to isLocationInLobby will return false until loadAndCacheBoundary() is called again.
     */
    fun invalidateCache() {
        synchronized(cacheLock) {
            cachedLobbyWorld = null
        }
    }

}