package org.tainorazum.universum.kiteditor.editor

import gg.flyte.twilight.event.event
import gg.flyte.twilight.extension.getPlayer
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerDropItemEvent
import org.tainorazum.universum.Universum
import org.tainorazum.universum.engine.kit.Kit
import org.tainorazum.universum.engine.kit.database.DB_GameSettings
import org.tainorazum.universum.kiteditor.database.DB_KitEditor
import org.tainorazum.universum.utility.maps.SchematicUtil.pasteSchematicTo
import org.tainorazum.universum.utility.player.PlayerUtility.noSound
import org.tainorazum.universum.utility.text.Colours
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.isEmpty

class KitEditorManager {

    var success: CompletableFuture<Boolean> = CompletableFuture<Boolean>()
        private set

    init {
        build().thenAccept {
            if (it) success.complete(true)
        }.exceptionally { ex ->
            Universum.instance.logger.severe("Failed to build kit editor schematic: ${ex.message}")
            ex.printStackTrace()
            success.complete(false)
            null
        }

        registerEvents()
    }

    // kit editor

    private val editors = ConcurrentHashMap.newKeySet<UUID>()
    private val kitEditors = ConcurrentHashMap<UUID, KitEditor>()

    fun clearKitEditor(kitEditor: KitEditor): Boolean {
        return kitEditors.remove(kitEditor.player.uniqueId, kitEditor)
    }

    fun getKitEditor(player: Player): KitEditor? {
        return kitEditors[player.uniqueId]
    }

    fun getKitEditor(uuid: UUID): KitEditor? {
        return kitEditors[uuid]
    }

    fun addEditor(uuid: UUID): Boolean {
        return editors.add(uuid)
    }

    fun removeEditor(uuid: UUID): Boolean {
        return editors.remove(uuid)
    }

    fun isEditor(uuid: UUID): Boolean {
        return uuid in editors
    }

    fun editKit(
        kit: Kit,
        player: Player,
        canCustomize: Boolean = true,
        itemAccess: Boolean = false,
        isDefault: Boolean = false
    ): CompletableFuture<Boolean> {

        if (kitEditors.containsKey(player.uniqueId)) {
            return CompletableFuture.completedFuture(false)
        }

        return DB_GameSettings.get(kit.id)
            .thenApply { settings ->
                val editor = KitEditor(
                    player = player,
                    kit = kit,
                    settings = settings,
                    canCustomize = canCustomize,
                    itemAccess = itemAccess,
                    isDefault = isDefault
                )

                kitEditors[player.uniqueId] = editor
                true
            }
            .exceptionally { ex ->
                Universum.instance.logger.severe(
                    "Failed to create kit editor for ${player.name}: ${ex.message}"
                )
                false
            }
    }

    // events

    fun registerEvents() {

        // ----------------------------
        // Player Drop Item Event
        // ----------------------------
        event<PlayerDropItemEvent>(EventPriority.HIGH) {

            if (Universum.lobbyManager.isPlayerInLobby(player) && isEditor(player.uniqueId)) { // Player in Kit Editor

                val kitEditor = getKitEditor(player)
                if (kitEditor != null) {

                    if (kitEditor.itemAccess) {
                        itemDrop.remove()
                        player.noSound()
                    } else {
                        // Block dropping entirely if they don't have itemAccess
                        isCancelled = true
                    }

                } else {
                    isCancelled = true
                    player.sendMessage(Component.text("You are unable to do that. Please contact an admin. (Error: Kit Editor hl1)", Colours.RED))
                }
            }
        }

        // ----------------------------
        // Inventory Click Event
        // ----------------------------
        event<InventoryClickEvent>(EventPriority.HIGH) {
            val player = getPlayer()

            if (!Universum.lobbyManager.isPlayerInLobby(player) || !isEditor(player.uniqueId)) {
                return@event
            }

            val kitEditor = getKitEditor(player)
            if (kitEditor == null) {
                isCancelled = true
                player.sendMessage(Component.text("You are unable to do that. Please contact an admin. (Error: Kit Editor hl1)", Colours.RED))
                return@event
            }

            // 1. Handle Clicks Outside the Inventory Window
            if (slotType == InventoryType.SlotType.OUTSIDE) {
                if(kitEditor.itemAccess) {
                    setCursor(null)
                    return@event
                }

                isCancelled = true

                // Optional: If you want to DELETE the item on their cursor when dragged outside instead of closing GUI:
                // event.cursor = null

                Bukkit.getScheduler().runTask(Universum.instance, Runnable {
                    player.closeInventory()
                })

                player.sendMessage(Component.text("Please do not attempt to drop items.", Colours.YELLOW))
                player.noSound()
                return@event
            }

            // 2. Handle Customization Restrictions
            if (!kitEditor.canCustomize) {
                isCancelled = true

                Bukkit.getScheduler().runTask(Universum.instance, Runnable {
                    player.closeInventory()
                })

                player.sendMessage(Component.text("This kit does not allow customization.", Colours.YELLOW))
                player.noSound()
                return@event
            }

            // 3. Handle Player Inventory Clicks vs Container Clicks
            if (clickedInventory?.type == InventoryType.PLAYER) {
                if (kitEditor.itemAccess) {
                    // Allow item interactions inside player inventory if itemAccess is enabled
                    return@event
                }
            } else {
                // Container inventory interactions logic or restriction fallback
                isCancelled = true
            }
        }
    }

    // map

    private fun build(): CompletableFuture<Boolean> {
        val targetLocationFuture = DB_KitEditor.getCenterLocation()
        val schematicBytesFuture = DB_KitEditor.getSchematicBytes()

        return CompletableFuture.allOf(targetLocationFuture, schematicBytesFuture).thenCompose {
            val targetLocation = targetLocationFuture.join()
            val schematicBytes = schematicBytesFuture.join()

            if (schematicBytes == null || schematicBytes.isEmpty()) {
                Universum.instance.logger.warning("Kit editor schematic bytes are empty or null! Skipping paste.")
                return@thenCompose CompletableFuture.completedFuture(false)
            }

            if (targetLocation.world == null) {
                Universum.instance.logger.severe("Target world for kit editor schematic paste is null!")
                return@thenCompose CompletableFuture.completedFuture(false)
            }

            pasteSchematicTo(schematicBytes, targetLocation, ignoreAir = false)
        }.exceptionally { ex ->
            Universum.instance.logger.severe("Failed to paste kit editor schematic: ${ex.cause?.message ?: ex.message}")
            ex.printStackTrace()
            false
        }
    }

}