package org.tainorazum.universum.engine.arena

import org.bukkit.Location
import org.tainorazum.universum.utility.maps.SchematicUtil
import java.util.concurrent.CompletableFuture

open class Arena(
    val identifier: String,
    val templateIdentifier: String,
    val schematic: ByteArray,
    val centerLocation: Location,
    val radius: Double
) {

    fun isInBounds(loc: Location): Boolean {
        if (loc.world != centerLocation.world) return false
        val dx = loc.x - centerLocation.x
        val dy = loc.y - centerLocation.y
        val dz = loc.z - centerLocation.z
        return (dx * dx + dy * dy + dz * dz) <= (radius * radius)
    }

    fun reset(): CompletableFuture<Boolean> {
        return SchematicUtil.pasteSchematicTo(schematic, centerLocation)
    }
}