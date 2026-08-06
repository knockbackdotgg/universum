package org.tainorazum.universum.engine.arena

import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.tainorazum.universum.Universum
import org.tainorazum.universum.engine.arena.database.ArenaDatabaseManager
import org.tainorazum.universum.engine.arena.database.ArenaTemplate
import java.io.File
import java.util.concurrent.CompletableFuture

class ArenaManager {

    var success: CompletableFuture<Boolean> = CompletableFuture<Boolean>()
        private set

    private val adbm = Universum.adbm

    private val arenas = ArrayList<Arena>()
    private val availableArenas = ArrayList<Arena>()

    private val ARENA_GAP = 10.000;

    // templates

    private val templateCache = HashMap<String, ArenaTemplate>()

    init {
        loadAllTemplates().thenAccept { success ->
            if(success) {
                this.success.complete(true)
            }
        }
    }

    fun loadAllTemplates(identifiers: List<String>) : CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()

        identifiers.forEach { id ->
            adbm.getArenaTemplate(id).thenAccept { template ->
                if (template != null) {
                    templateCache[id] = template
                    future.complete(true)
                } else {
                    future.complete(false)
                }
            }
        }
        return future
    }

    fun loadAllTemplates(): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()

        adbm.getAllArenaTemplates().thenAccept { allTemplates ->
            templateCache.clear()

            try {
                for (template in allTemplates) {
                    templateCache[template.id] = template
                }
                org.bukkit.Bukkit.getLogger().info("[ArenaManager] Successfully cached ${templateCache.size} arena templates into RAM!")

                future.complete(true)
            } catch (e: Exception) {
                future.complete(false)
            }
        }
        return future
    }

    // game usable


    fun retrieveArena(templateId: String) : CompletableFuture<Arena?> {
        val future = CompletableFuture<Arena?>()

        if(availableArenas.firstOrNull() != null) {
            future.complete(availableArenas.firstOrNull())
            return future
        }

        createArena(generateNextId(templateId)).thenAccept { arena ->
            if(arena != null) {
                future.complete(arena)
            } else {
                future.complete(null)
            }
        }
        return future
    }

    fun releaseArena(arena: Arena) : Boolean {
        return availableArenas.add(arena)
    }

    private fun createArena(id: String): CompletableFuture<Arena?> {
        val future = CompletableFuture<Arena?>()

        val at = templateCache[id]

        if(at == null) {
            future.complete(null)
            return future
        }

        val xOffset = arenas.size * ARENA_GAP
        val shiftedCenter = at.center.clone().apply {
            x += xOffset
        }

        val arena = Arena(id, at.id, at.schematic, shiftedCenter, at.radius)
        arena.create().thenAccept { success ->
            if(success) {
                arenas.add(arena)
                future.complete(arena)
            }
        }

        return future
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
