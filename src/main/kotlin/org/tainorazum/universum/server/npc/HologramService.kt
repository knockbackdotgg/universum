package org.tainorazum.universum.server.npc

import org.bukkit.Location
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.tainorazum.universum.Universum

object HologramService {
    /**
     * Spawns a vertical stack of armor-stand holograms at the given base location.
     * Returns the spawned entities so the caller can track them.
     */
    fun spawnHologram(base: Location, lines: List<HologramLine>): List<Entity> {
        val spawned = mutableListOf<Entity>()
        var index = 0
        for (line in lines.sortedBy { it.lineIndex }) {
            val loc = base.clone().add(0.0, line.offsetY + 0.25 * index, 0.0)
            val asEntity = base.world.spawnEntity(loc, EntityType.ARMOR_STAND) as ArmorStand
            asEntity.isVisible = false
            asEntity.isMarker = true
            asEntity.isCustomNameVisible = true
            asEntity.customName = line.content
            spawned.add(asEntity)
            index++
        }
        return spawned
    }
}
