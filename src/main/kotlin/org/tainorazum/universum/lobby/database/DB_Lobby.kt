package org.tainorazum.universum.lobby.database

import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.tainorazum.universum.database.other.mediumBlob
import org.tainorazum.universum.utility.database.DatabaseUtil.dba
import java.io.ByteArrayOutputStream
import java.util.concurrent.CompletableFuture


object DB_Lobby : Table("lobby") {

    val spawnpoint_world = varchar("spawnpoint_world", 255)

    val spawnpoint_x = double("spawnpoint_x")
    val spawnpoint_y = double("spawnpoint_y")
    val spawnpoint_z = double("spawnpoint_z")
    val spawnpoint_pitch = float("spawnpoint_pitch")
    val spawnpoint_yaw = float("spawnpoint_yaw")

    val map_schematic = mediumBlob("map_schematic")

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
            }.get()

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

    fun clearSchematic(): CompletableFuture<Boolean> {
        return dba {
            transaction {
                val world = cachedLobbyWorld
                if (world != null) {
                    DB_Lobby.update(where = { DB_Lobby.spawnpoint_world eq world }) {
                        it[map_schematic] = ExposedBlob(byteArrayOf())
                    } > 0
                } else {
                    DB_Lobby.update {
                        it[map_schematic] = ExposedBlob(byteArrayOf())
                    } > 0
                }
            }
        }
    }

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

        val centerVector = region.center
        val centerLocation = Location(
            player.world,
            centerVector.x(),
            centerVector.y(),
            centerVector.z()
        )

        // WorldEdit's SpongeSchematicWriter automatically handles GZIP compression internally
        val bytes = ByteArrayOutputStream().use { baos ->
            BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(baos).use { writer ->
                writer.write(clipboard)
            }
            baos.toByteArray()
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
                val world = cachedLobbyWorld
                val query = if (world != null) {
                    DB_Lobby.selectAll().where { DB_Lobby.spawnpoint_world eq world }
                } else {
                    DB_Lobby.selectAll()
                }

                query.limit(1)
                    .firstOrNull()
                    ?.get(map_schematic)?.bytes
            }
        }
    }

    fun setSpawnLocation(location: Location): CompletableFuture<Boolean> {
        return dba {
            transaction {
                val worldName = location.world?.name ?: "world"
                val exists = DB_Lobby.selectAll()
                    .where { DB_Lobby.spawnpoint_world eq worldName }
                    .count() > 0

                if (exists) {
                    DB_Lobby.update(
                        where = { DB_Lobby.spawnpoint_world eq worldName }
                    ) {
                        it[spawnpoint_x] = location.x
                        it[spawnpoint_y] = location.y
                        it[spawnpoint_z] = location.z
                        it[spawnpoint_yaw] = location.yaw
                        it[spawnpoint_pitch] = location.pitch
                    }
                } else {
                    DB_Lobby.insert {
                        it[spawnpoint_world] = worldName
                        it[spawnpoint_x] = location.x
                        it[spawnpoint_y] = location.y
                        it[spawnpoint_z] = location.z
                        it[spawnpoint_yaw] = location.yaw
                        it[spawnpoint_pitch] = location.pitch
                        it[center_x] = location.x
                        it[center_y] = location.y
                        it[center_z] = location.z
                    }
                }
                synchronized(cacheLock) {
                    cachedLobbyWorld = worldName
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
                val world = cachedLobbyWorld
                val row = if (world != null) {
                    DB_Lobby.selectAll().where { DB_Lobby.spawnpoint_world eq world }.limit(1).firstOrNull()
                } else {
                    DB_Lobby.selectAll().limit(1).firstOrNull()
                }

                val worldName = row?.get(spawnpoint_world) ?: world ?: "world"
                val bukkitWorld = Bukkit.getWorld(worldName) ?: Bukkit.getWorlds().first()

                Location(
                    bukkitWorld,
                    row?.get(spawnpoint_x) ?: 0.0,
                    row?.get(spawnpoint_y) ?: 200.0,
                    row?.get(spawnpoint_z) ?: 0.0,
                    row?.get(spawnpoint_yaw) ?: 0f,
                    row?.get(spawnpoint_pitch) ?: 0f,
                )
            }
        }
    }

    fun setCenterLocation(location: Location): CompletableFuture<Boolean> {
        return dba {
            transaction {
                val worldName = location.world?.name ?: "world"
                val exists = DB_Lobby.selectAll()
                    .where { DB_Lobby.spawnpoint_world eq worldName }
                    .count() > 0

                if (exists) {
                    DB_Lobby.update(
                        where = { DB_Lobby.spawnpoint_world eq worldName }
                    ) {
                        it[center_x] = location.x
                        it[center_y] = location.y
                        it[center_z] = location.z
                    }
                } else {
                    DB_Lobby.insert {
                        it[spawnpoint_world] = worldName
                        it[spawnpoint_x] = location.x
                        it[spawnpoint_y] = location.y
                        it[spawnpoint_z] = location.z
                        it[spawnpoint_yaw] = 0f
                        it[spawnpoint_pitch] = 0f
                        it[center_x] = location.x
                        it[center_y] = location.y
                        it[center_z] = location.z
                    }
                }
                synchronized(cacheLock) {
                    cachedLobbyWorld = worldName
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
                val world = cachedLobbyWorld
                val row = if (world != null) {
                    DB_Lobby.selectAll().where { DB_Lobby.spawnpoint_world eq world }.limit(1).firstOrNull()
                } else {
                    DB_Lobby.selectAll().limit(1).firstOrNull()
                }

                val worldName = row?.get(spawnpoint_world) ?: world ?: "world"
                val bukkitWorld = Bukkit.getWorld(worldName) ?: Bukkit.getWorlds().first()

                Location(
                    bukkitWorld,
                    row?.get(center_x) ?: 0.0,
                    row?.get(center_y) ?: 100.0,
                    row?.get(center_z) ?: 0.0
                )
            }
        }
    }

    fun isLocationInLobby(location: Location): Boolean {
        val lobbyWorld = cachedLobbyWorld ?: return false
        return location.world?.name == lobbyWorld
    }

    fun invalidateCache() {
        synchronized(cacheLock) {
            cachedLobbyWorld = null
        }
        cachedCenterLocation = null
        cachedSpawnLocation = null
    }
}