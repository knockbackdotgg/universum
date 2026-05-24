package org.tainorazum.universum.game.arena.database

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.tainorazum.universum.game.arena.Arena
import org.tainorazum.universum.utility.ComponentSerializer
import java.io.File
import java.util.concurrent.CompletableFuture
import javax.sql.rowset.serial.SerialBlob
import kotlin.collections.map

data class ArenaTemplate(
    val id: String,
    val name: Component,
    val description: Component,
    val schematic: ByteArray,
    val center: Location
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

        return CompletableFuture.supplyAsync {
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
                            it[arenaIdentifier] = arenaId
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
        return CompletableFuture.supplyAsync {
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

                            ArenaTemplate(
                                id = arenaId,
                                name = nameComponent,
                                description = descriptionComponent,
                                schematic = rawBytes,
                                center = centerLoc
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
        return CompletableFuture.supplyAsync {
            try {
                transaction {
                    DB_Arenas.selectAll().map { row ->
                        val arenaId = row[DB_Arenas.identifier]
                        val nameComponent = ComponentSerializer.deserialize(row[DB_Arenas.name])
                        val descriptionComponent = ComponentSerializer.deserialize(row[DB_Arenas.description])
                        val rawBytes = row[DB_Arenas.schematic].bytes

                        val centerLoc = row[DB_Arenas.centerLocation].toLocation()
                            ?: throw IllegalStateException("Center location data is corrupted for arena: $arenaId")

                        ArenaTemplate(
                            id = arenaId,
                            name = nameComponent,
                            description = descriptionComponent,
                            schematic = rawBytes,
                            center = centerLoc
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
        return CompletableFuture.supplyAsync {
            val locationsMap = mutableMapOf<String, Location>()

            transaction {
                DB_ArenaLocations.selectAll()
                    .where { DB_ArenaLocations.arenaIdentifier eq arenaId }
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

}