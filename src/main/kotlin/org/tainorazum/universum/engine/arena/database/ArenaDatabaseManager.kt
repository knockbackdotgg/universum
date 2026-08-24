package org.tainorazum.universum.engine.arena.database

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.tainorazum.universum.engine.game.manager.fields.GameType
import org.tainorazum.universum.utility.text.ComponentSerializer
import org.tainorazum.universum.utility.database.DatabaseUtil.dba
import java.io.File
import java.util.concurrent.CompletableFuture
import kotlin.collections.map

data class ArenaTemplate(
    val id: String,
    val name: Component,
    val description: Component,
    val schematic: ByteArray,
    val center: Location,
    val radius: Double,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArenaTemplate) return false
        if (id != other.id) return false
        if (!schematic.contentEquals(other.schematic)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + schematic.contentHashCode()
        return result
    }
}

class ArenaDatabaseManager {

    fun saveArena(
        arenaId: String,
        name: Component,
        description: Component,
        schematic: File,
        center: Location,
        locations: Map<String, Location>
    ): CompletableFuture<Boolean> {

        if (!schematic.exists()) {
            val failedFuture = CompletableFuture<Boolean>()
            failedFuture.completeExceptionally(IllegalArgumentException("Schematic file does not exist: ${schematic.path}"))
            return failedFuture
        }

        return dba {
            try {
                val schematicBlob = ExposedBlob(schematic.readBytes())

                transaction {
                    DB_Arenas.insert {
                        it[identifier] = arenaId
                        it[DB_Arenas.name] = ComponentSerializer.serialize(name)
                        it[DB_Arenas.description] = ComponentSerializer.serialize(description)
                        it[DB_Arenas.schematic] = schematicBlob
                        it[centerLocation] = center.toSerializedString()
                    }

                    locations.forEach { (key, location) ->
                        DB_ArenaLocations.insert {
                            it[identifier] = arenaId
                            it[locationKey] = key
                            it[locationData] = location.toSerializedString()
                        }
                    }
                }
                true // successful
            } catch (e: Exception) {
                e.printStackTrace()
                false // failed
            }
        }
    }

    fun getArenaTemplate(arenaId: String): CompletableFuture<ArenaTemplate?> {
        return dba {
            try {
                transaction {
                    DB_Arenas.selectAll()
                        .where { DB_Arenas.identifier eq arenaId }
                        .map { row ->
                            val nameComponent = ComponentSerializer.deserialize(row[DB_Arenas.name])
                            val descriptionComponent = ComponentSerializer.deserialize(row[DB_Arenas.description])

                            val rawBytes = row[DB_Arenas.schematic].bytes

                            val centerLoc = row[DB_Arenas.centerLocation].toLocation()
                                ?: throw IllegalStateException("Center location data is corrupted for arena: $arenaId")

                            val radius = row[DB_Arenas.radius]

                            ArenaTemplate(
                                id = arenaId,
                                name = nameComponent,
                                description = descriptionComponent,
                                schematic = rawBytes,
                                center = centerLoc,
                                radius = radius
                            )
                        }
                        .singleOrNull()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    // arena database

    fun getAllArenaTemplates(): CompletableFuture<List<ArenaTemplate>> {
        return dba {
            try {
                transaction {
                    DB_Arenas.selectAll().map { row ->
                        val arenaId = row[DB_Arenas.identifier]
                        val nameComponent = ComponentSerializer.deserialize(row[DB_Arenas.name])
                        val descriptionComponent = ComponentSerializer.deserialize(row[DB_Arenas.description])
                        val rawBytes = row[DB_Arenas.schematic].bytes

                        val centerLoc = row[DB_Arenas.centerLocation].toLocation()
                            ?: throw IllegalStateException("Center location data is corrupted for arena: $arenaId")

                        val radius = row[DB_Arenas.radius]

                        ArenaTemplate(
                            id = arenaId,
                            name = nameComponent,
                            description = descriptionComponent,
                            schematic = rawBytes,
                            center = centerLoc,
                            radius = radius
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    fun getArenaLocations(arenaId: String): CompletableFuture<Map<String, Location>> {
        return dba {
            val locationsMap = mutableMapOf<String, Location>()

            transaction {
                DB_ArenaLocations.selectAll()
                    .where { DB_ArenaLocations.identifier eq arenaId }
                    .forEach { row ->
                        val key = row[DB_ArenaLocations.locationKey]
                        val locString = row[DB_ArenaLocations.locationData]

                        locString.toLocation()?.let { location ->
                            locationsMap[key] = location
                        }
                    }
            }
            locationsMap
        }
    }

    // utilities

    fun Location.toSerializedString(): String {
        return "${world.name};$x;$y;$z;$yaw;$pitch"
    }

    fun String.toLocation(): Location? {
        val parts = this.split(";")
        if (parts.size < 4) return null

        val world = Bukkit.getWorld(parts[0]) ?: return null
        val x = parts[1].toDoubleOrNull() ?: return null
        val y = parts[2].toDoubleOrNull() ?: return null
        val z = parts[3].toDoubleOrNull() ?: return null
        val yaw = parts.getOrNull(4)?.toFloatOrNull() ?: 0f
        val pitch = parts.getOrNull(5)?.toFloatOrNull() ?: 0f

        return Location(world, x, y, z, yaw, pitch)
    }

    // allowed gametypes management

    fun addAllowedGameType(arenaId: String, gameType: GameType): CompletableFuture<Boolean> {
        return dba {
            try {
                transaction {
                    val exists = DB_ArenaGametypes.selectAll()
                        .where { (DB_ArenaGametypes.arenaId eq arenaId) and (DB_ArenaGametypes.gametype eq gameType.name) }
                        .limit(1)
                        .firstOrNull() != null

                    if (!exists) {
                        DB_ArenaGametypes.insert {
                            it[DB_ArenaGametypes.arenaId] = arenaId
                            it[DB_ArenaGametypes.gametype] = gameType.name
                        }
                    }
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    fun removeAllowedGameType(arenaId: String, gameType: GameType): CompletableFuture<Boolean> {
        return dba {
            try {
                transaction {
                    DB_ArenaGametypes.deleteWhere { (DB_ArenaGametypes.arenaId eq arenaId) and (DB_ArenaGametypes.gametype eq gameType.name) }
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    fun getAllowedGameTypes(arenaId: String): CompletableFuture<List<GameType>> {
        return dba {
            try {
                transaction {
                    DB_ArenaGametypes.selectAll()
                        .where { DB_ArenaGametypes.arenaId eq arenaId }
                        .mapNotNull { row ->
                            val raw = row[DB_ArenaGametypes.gametype]
                            runCatching { GameType.valueOf(raw) }.getOrNull()
                        }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    fun setAllowedGameTypes(arenaId: String, gameTypes: List<GameType>): CompletableFuture<Boolean> {
        return dba {
            try {
                transaction {
                    DB_ArenaGametypes.deleteWhere { DB_ArenaGametypes.arenaId eq arenaId }
                    gameTypes.forEach { gt ->
                        DB_ArenaGametypes.insert {
                            it[DB_ArenaGametypes.arenaId] = arenaId
                            it[DB_ArenaGametypes.gametype] = gt.name
                        }
                    }
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    // allowed kits management

    fun addAllowedKit(arenaId: String, kitId: String): CompletableFuture<Boolean> {
        return dba {
            try {
                transaction {
                    val exists = DB_ArenaKits.selectAll()
                        .where { (DB_ArenaKits.arenaId eq arenaId) and (DB_ArenaKits.kitId eq kitId) }
                        .limit(1)
                        .firstOrNull() != null

                    if (!exists) {
                        DB_ArenaKits.insert {
                            it[DB_ArenaKits.arenaId] = arenaId
                            it[DB_ArenaKits.kitId] = kitId
                        }
                    }
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    fun removeAllowedKit(arenaId: String, kitId: String): CompletableFuture<Boolean> {
        return dba {
            try {
                transaction {
                    DB_ArenaKits.deleteWhere { (DB_ArenaKits.arenaId eq arenaId) and (DB_ArenaKits.kitId eq kitId) }
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    fun getAllowedKits(arenaId: String): CompletableFuture<List<String>> {
        return dba {
            try {
                transaction {
                    DB_ArenaKits.selectAll()
                        .where { DB_ArenaKits.arenaId eq arenaId }
                        .map { row -> row[DB_ArenaKits.kitId] }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    fun setAllowedKits(arenaId: String, kitIds: List<String>): CompletableFuture<Boolean> {
        return dba {
            try {
                transaction {
                    DB_ArenaKits.deleteWhere { DB_ArenaKits.arenaId eq arenaId }
                    kitIds.forEach { id ->
                        DB_ArenaKits.insert {
                            it[DB_ArenaKits.arenaId] = arenaId
                            it[DB_ArenaKits.kitId] = id
                        }
                    }
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

}

    fun String.toLocation(): Location? {
        val parts = this.split(";")
        if (parts.size < 4) return null

        val world = Bukkit.getWorld(parts[0]) ?: return null
        val x = parts[1].toDoubleOrNull() ?: return null
        val y = parts[2].toDoubleOrNull() ?: return null
        val z = parts[3].toDoubleOrNull() ?: return null
        val yaw = parts.getOrNull(4)?.toFloatOrNull() ?: 0f
        val pitch = parts.getOrNull(5)?.toFloatOrNull() ?: 0f

        return Location(world, x, y, z, yaw, pitch)
    }