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
    val radius: Double
) {

    private val locations = mutableListOf<Location>()

    init {

    }

    fun getLocations(): CompletableFuture<List<Location>> {
        val future = CompletableFuture<List<Location>>()

        if (locations.isNotEmpty()) {
            future.complete(locations)
            return future
        }

        Universum.adbm.getArenaLocations(templateIdentifier).thenAccept { locMap ->
            if (locMap != null) {
                val fetchedLocations = locMap.values.toList()
                locations.addAll(fetchedLocations)
                future.complete(locations)
            } else {
                future.complete(emptyList())
            }
        }.exceptionally { ex ->
            Universum.instance.logger.severe("[Arena] Failed to load locations for $templateIdentifier:")
            ex.printStackTrace()
            future.complete(emptyList())
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