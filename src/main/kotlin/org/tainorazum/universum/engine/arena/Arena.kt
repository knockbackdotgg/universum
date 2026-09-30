package org.tainorazum.universum.engine.arena

import org.bukkit.Location
import org.tainorazum.universum.Universum
import org.tainorazum.universum.utility.maps.SchematicUtil
import java.util.concurrent.CompletableFuture

class Arena(
    val identifier: String,
    val templateIdentifier: String,
    val schematic: ByteArray,
    val centerLocation: Location,
    val radius: Double,
    val xOffset: Double = 0.0
) {

    private var locations = mapOf<String, Location>()

    /**
     * Unspoken rules of arena location names:
     * Example:
     * Name: 1.1
     *       ^ ^
     *   Team   player of team
     * So, 1.1 = First player of team 1
     */

    fun getLocations(): CompletableFuture<Map<String, Location>> {
        val future = CompletableFuture<Map<String, Location>>()

        if (locations.isNotEmpty()) {
            future.complete(locations)
            return future
        }

        Universum.adbm.getArenaLocations(templateIdentifier).thenAccept { locMap ->
            if (locMap != null) {
                // Apply the arena's X offset and ensure world matches instance world
                locations = locMap.mapValues { (_, loc) ->
                    loc.clone().apply {
                        x += xOffset
                        world = centerLocation.world
                    }
                }
                future.complete(locations)
            } else {
                future.complete(emptyMap())
            }
        }.exceptionally { ex ->
            Universum.instance.logger.severe("[Arena] Failed to load locations for $templateIdentifier:")
            ex.printStackTrace()
            future.complete(emptyMap())
            null
        }

        return future
    }

    fun reset(): CompletableFuture<Boolean> {
        return SchematicUtil.pasteSchematicTo(schematic, centerLocation)
    }

    fun isInBounds(loc: Location): Boolean {
        if (loc.world != centerLocation.world) return false
        val dx = loc.x - centerLocation.x
        val dy = loc.y - centerLocation.y
        val dz = loc.z - centerLocation.z
        return (dx * dx + dy * dy + dz * dz) <= (radius * radius)
    }
}