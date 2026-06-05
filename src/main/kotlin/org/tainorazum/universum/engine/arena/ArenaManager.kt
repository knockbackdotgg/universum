package org.tainorazum.universum.engine.arena

import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.tainorazum.universum.engine.arena.database.ArenaDatabaseManager
import org.tainorazum.universum.engine.arena.database.ArenaTemplate
import java.io.File
import java.util.concurrent.CompletableFuture

class ArenaManager {

    init {
        loadAllTemplates()
    }

    private val adbm = ArenaDatabaseManager()

    private val arenas = ArrayList<Arena>()
    private val availableArenas = ArrayList<Arena>()

    private val ARENA_GAP = 10.000;

    // templates

    private val templateCache = HashMap<String, ArenaTemplate>()

    fun loadAllTemplates(identifiers: List<String>) {
        identifiers.forEach { id ->
            adbm.getArenaTemplate(id).thenAccept { template ->
                if (template != null) {
                    templateCache[id] = template
                }
            }
        }
    }

    fun loadAllTemplates(): CompletableFuture<Void> {
        return adbm.getAllArenaTemplates().thenAccept { allTemplates ->
            templateCache.clear()

            for (template in allTemplates) {
                templateCache[template.id] = template
            }

            org.bukkit.Bukkit.getLogger().info("[ArenaManager] Successfully cached ${templateCache.size} arena templates into RAM!")
        }
    }

    // game usable

    fun retrieveArena(templateId: String) : Arena? {
        return availableArenas.firstOrNull() ?: createArena(generateNextId(templateId))
    }

    fun releaseArena(arena: Arena) {
        availableArenas.add(arena)
    }

    private fun createArena(id: String): Arena? {
        val at = templateCache[id] ?: return null

        val xOffset = arenas.size * ARENA_GAP
        val shiftedCenter = at.center.clone().apply {
            x += xOffset
        }

        val arena = Arena(id, at.id, at.schematic, shiftedCenter, at.radius)
        arenas.add(arena)

        return arena
    }

    // registration & management

    fun registerArena(
        identifier: String,
        name: Component,
        description: Component,
        schematic: File,
        centerLocation: Location,
        locations: Map<String, Location>
    ): CompletableFuture<Boolean> {
        return adbm.saveArena(identifier, name, description, schematic, centerLocation, locations)
    }

    // utilities

    fun generateNextId(templateId: String): String {
        return "$templateId-${arenas.count{ it.templateIdentifier == templateId }+1}"
    }

}
