package org.tainorazum.universum.engine.arena

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.tainorazum.universum.Universum
import org.tainorazum.universum.engine.arena.database.ArenaTemplate
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class ArenaManager {

    val success = CompletableFuture<Boolean>()

    private val adbm = Universum.adbm

    private val activeArenas = CopyOnWriteArrayList<Arena>()
    private val availableArenas = CopyOnWriteArrayList<Arena>()

    private val arenaGap: Double = 1000.0

    private val templateCache = ConcurrentHashMap<String, ArenaTemplate>()

    init {
        loadAllTemplates().thenAccept { loaded ->
            success.complete(loaded)
        }
    }

    fun loadAllTemplates(identifiers: List<String>): CompletableFuture<Boolean> {
        val futures = identifiers.map { id ->
            adbm.getArenaTemplate(id).thenAccept { template ->
                if (template != null) {
                    templateCache[id] = template
                }
            }
        }.toTypedArray()

        return CompletableFuture.allOf(*futures).thenApply {
            templateCache.isNotEmpty()
        }
    }

    fun loadAllTemplates(): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()

        adbm.getAllArenaTemplates().thenAccept { allTemplates ->
            templateCache.clear()

            try {
                for (template in allTemplates) {
                    templateCache[template.id] = template
                }
                Bukkit.getLogger().info("[ArenaManager] Cached ${templateCache.size} arena templates into memory.")
                future.complete(true)
            } catch (e: Exception) {
                Bukkit.getLogger().severe("[ArenaManager] Failed to cache arena templates: ${e.message}")
                future.complete(false)
            }
        }
        return future
    }

    fun retrieveArena(templateId: String): CompletableFuture<Arena?> {
        val future = CompletableFuture<Arena?>()

        val available = availableArenas.firstOrNull { it.templateIdentifier == templateId }
        if (available != null) {
            availableArenas.remove(available)
            future.complete(available)
            return future
        }

        val instanceId = generateNextId(templateId)
        createArenaInstance(instanceId, templateId).thenAccept { arena ->
            future.complete(arena)
        }

        return future
    }

    fun releaseArena(arena: Arena): CompletableFuture<Boolean> {
        return arena.reset().thenApply {
            if (!availableArenas.contains(arena)) {
                availableArenas.add(arena)
            }
            true
        }
    }

    private fun createArenaInstance(instanceId: String, templateId: String): CompletableFuture<Arena?> {
        val future = CompletableFuture<Arena?>()

        val template = templateCache[templateId]
        if (template == null) {
            Bukkit.getLogger().warning("[ArenaManager] Cannot create arena instance. Template '$templateId' not found.")
            future.complete(null)
            return future
        }

        val xOffset = activeArenas.size * arenaGap
        val shiftedCenter = template.center.clone().apply {
            x += xOffset
        }

        val arena = Arena(instanceId, template.id, template.schematic, shiftedCenter, template.radius)

        arena.reset().thenAccept { created ->
            if (created) {
                activeArenas.add(arena)
                future.complete(arena)
            } else {
                future.complete(null)
            }
        }

        return future
    }

    fun registerArena(
        identifier: String,
        name: Component,
        description: Component,
        schematic: File,
        centerLocation: Location,
        locations: Map<String, Location>
    ): CompletableFuture<Boolean> {
        return adbm.saveArena(identifier, name, description, schematic, centerLocation, locations)
            .thenCompose { saved ->
                if (saved) {
                    loadAllTemplates()
                } else {
                    CompletableFuture.completedFuture(false)
                }
            }
    }

    fun generateNextId(templateId: String): String {
        val count = activeArenas.count { it.templateIdentifier == templateId } + 1
        return "$templateId-$count"
    }

    fun getTemplate(templateId: String): ArenaTemplate? = templateCache[templateId]

    fun shutdown() {
        activeArenas.forEach { arena ->
            arena.reset()
        }
        activeArenas.clear()
        availableArenas.clear()
        templateCache.clear()
    }
}