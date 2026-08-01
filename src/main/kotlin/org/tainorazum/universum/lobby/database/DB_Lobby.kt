package org.tainorazum.universum.lobby.database

import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat
import gg.flyte.twilight.data.sql.QueryBuilder.Companion.eq
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.tainorazum.universum.engine.arena.database.DB_ArenaLocations
import org.tainorazum.universum.utility.DatabaseUtil.dba
import java.io.ByteArrayOutputStream
import java.util.concurrent.CompletableFuture
import kotlin.collections.set

object DB_Lobby : Table("lobby") {

    val spawnpoint_world = varchar("spawnpoint_world", 255)

    val spawnpoint_x = double("spawnpoint_x")
    val spawnpoint_y = double("spawnpoint_y")
    val spawnpoint_z = double("spawnpoint_z")
    val spawnpoint_pitch = float("spawnpoint_pitch")
    val spawnpoint_yaw = float("spawnpoint_yaw")

    val map_schematic = blob("map_schematic")

    val center_x = double("center_x")
    val center_y = double("center_y")
    val center_z = double("center_z")

    override val primaryKey = PrimaryKey(spawnpoint_world, spawnpoint_x, spawnpoint_y, spawnpoint_z, spawnpoint_pitch, spawnpoint_yaw)

    @Volatile
    private var cachedLobbyWorld: String? = null
    private val cacheLock = Any()

    @Volatile
    private var cachedCenterLocation: Location? = null
    private var cachedSpawnLocation: Location? = null

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
     * Reads a player's FastAsyncWorldEdit / WorldEdit clipboard, calculates the schematic's
     * center point, updates both the schematic blob and center coordinates in the database,
     * and updates local memory caches.
     */
    fun setSchematic(player: Player): CompletableFuture<Boolean> {

        val actor = BukkitAdapter.adapt(player)
        val session = WorldEdit.getInstance().sessionManager.get(actor)

        val clipboardHolder = try {
            session.clipboard
        } catch (e: Exception) {
            return CompletableFuture.completedFuture(false)
        }

        val clipboard = clipboardHolder.clipboard
        val region = clipboard.region

        // Calculate geometric center of the clipboard bounds
        val centerVector = region.center
        val centerLocation = Location(
            player.world,
            centerVector.x(),
            centerVector.y(),
            centerVector.z()
        )

        // Serialize clipboard to Sponge Schematic byte array
        val bytes = ByteArrayOutputStream().use { outputStream ->
            BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(outputStream).use { writer ->
                writer.write(clipboard)
            }
            outputStream.toByteArray()
        }

        val worldName = player.world.name

        return dba {
            transaction {
                val exists = DB_Lobby.selectAll()
                    .where { DB_Lobby.spawnpoint_world eq worldName }
                    .count() > 0

                if (exists) {
                    DB_Lobby.update(
                        where = { DB_Lobby.spawnpoint_world eq worldName }
                    ) {
                        it[map_schematic] = ExposedBlob(bytes)
                        it[center_x] = centerLocation.x
                        it[center_y] = centerLocation.y
                        it[center_z] = centerLocation.z
                    }
                } else {
                    DB_Lobby.insert {
                        it[spawnpoint_world] = worldName
                        it[spawnpoint_x] = player.location.x
                        it[spawnpoint_y] = player.location.y
                        it[spawnpoint_z] = player.location.z
                        it[spawnpoint_yaw] = player.location.yaw
                        it[spawnpoint_pitch] = player.location.pitch
                        it[map_schematic] = ExposedBlob(bytes)
                        it[center_x] = centerLocation.x
                        it[center_y] = centerLocation.y
                        it[center_z] = centerLocation.z
                    }
                }

                // Update local memory caches
                synchronized(cacheLock) {
                    cachedLobbyWorld = worldName
                }
                cachedCenterLocation = centerLocation
                true
            }
        }
    }

    fun getSchematicBytes(): CompletableFuture<ByteArray?> {
        return dba {
            transaction {
                DB_Lobby.selectAll()
                    .where { DB_Lobby.spawnpoint_world eq cachedLobbyWorld!! }
                    .limit(1)
                    .firstOrNull()
                    ?.get(map_schematic)?.bytes
            }
        }
    }

    fun setSpawnLocation(location: Location): CompletableFuture<Boolean> {
        return dba {
            transaction {
                val exists = DB_Lobby.selectAll()
                    .where { DB_Lobby.spawnpoint_world eq location.world.name }
                    .count() > 0

                if (exists) {
                    DB_Lobby.update(
                        where = { DB_Lobby.spawnpoint_world eq location.world.name }
                    ) {
                        it[DB_Lobby.spawnpoint_x] = location.x
                        it[DB_Lobby.spawnpoint_y] = location.y
                        it[DB_Lobby.spawnpoint_z] = location.z
                        it[DB_Lobby.spawnpoint_yaw] = location.yaw
                        it[DB_Lobby.spawnpoint_pitch] = location.pitch
                    }
                } else {
                    DB_Lobby.insert {
                        it[DB_Lobby.spawnpoint_world] = location.world.name
                        it[DB_Lobby.spawnpoint_x] = location.x
                        it[DB_Lobby.spawnpoint_y] = location.y
                        it[DB_Lobby.spawnpoint_z] = location.z
                        it[DB_Lobby.spawnpoint_yaw] = location.yaw
                        it[DB_Lobby.spawnpoint_pitch] = location.pitch
                    }
                }
                cachedSpawnLocation = location
                true
            }
        }
    }

    fun getSpawnpointLocation(): CompletableFuture<Location> {
        cachedSpawnLocation?.let { return CompletableFuture.completedFuture(it) }
        return dba {
            transaction {
                val row = DB_Lobby.selectAll()
                    .where { DB_Lobby.spawnpoint_world eq cachedLobbyWorld!! }
                    .limit(1)
                    .firstOrNull()

                Location(
                    Bukkit.getWorld(cachedLobbyWorld!!),
                    row?.get(DB_Lobby.spawnpoint_x) ?: 0.0,
                    row?.get(DB_Lobby.spawnpoint_y) ?: 200.0,
                    row?.get(DB_Lobby.spawnpoint_z) ?: 0.0,
                    row?.get(DB_Lobby.spawnpoint_yaw) ?: 0f,
                    row?.get(DB_Lobby.spawnpoint_pitch) ?: 0f,
                )
            }
        }
    }

    fun setCenterLocation(location: Location): CompletableFuture<Boolean> {
        return dba {
            transaction {
                val exists = DB_Lobby.selectAll()
                    .where { DB_Lobby.spawnpoint_world eq location.world.name }
                    .count() > 0

                if (exists) {
                    DB_Lobby.update(
                        where = { DB_Lobby.spawnpoint_world eq location.world.name }
                    ) {
                        it[DB_Lobby.center_x] = location.x
                        it[DB_Lobby.center_y] = location.y
                        it[DB_Lobby.center_z] = location.z
                    }
                } else {
                    DB_Lobby.insert {
                        it[DB_Lobby.center_x] = location.x
                        it[DB_Lobby.center_y] = location.y
                        it[DB_Lobby.center_z] = location.z
                    }
                }
                cachedCenterLocation = location
                true
            }
        }
    }

    fun getCenterLocation(): CompletableFuture<Location> {
        cachedCenterLocation?.let { return CompletableFuture.completedFuture(it) }
        return dba {
            transaction {
                val row = DB_Lobby.selectAll()
                    .where { DB_Lobby.spawnpoint_world eq cachedLobbyWorld!! }
                    .limit(1)
                    .firstOrNull()

                Location(
                    Bukkit.getWorld(cachedLobbyWorld!!),
                    row?.get(DB_Lobby.center_x) ?: 0.0,
                    row?.get(DB_Lobby.center_y) ?: 100.0,
                    row?.get(DB_Lobby.center_z) ?: 0.0
                )
            }
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