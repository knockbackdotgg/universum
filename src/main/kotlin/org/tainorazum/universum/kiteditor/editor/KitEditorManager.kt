package org.tainorazum.universum.kiteditor.editor

import gg.flyte.twilight.event.event
import gg.flyte.twilight.extension.getPlayer
import gg.flyte.twilight.scheduler.delay
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.inventory.ItemStack
import org.tainorazum.universum.Universum
import org.tainorazum.universum.engine.kit.Kit
import org.tainorazum.universum.engine.kit.database.DB_GameSettings
import org.tainorazum.universum.kiteditor.database.DB_KitEditor
import org.tainorazum.universum.kiteditor.editor.attributes.AttributesGui
import org.tainorazum.universum.kiteditor.editor.items.gui.ItemsGui
import org.tainorazum.universum.kiteditor.editor.items.gui.ItemsMenuGui
import org.tainorazum.universum.utility.maps.SchematicUtil.pasteSchematicTo
import org.tainorazum.universum.utility.other.hasAdditionalAttributes
import org.tainorazum.universum.utility.other.isEnchantable
import org.tainorazum.universum.utility.player.PlayerIsolationUtility
import org.tainorazum.universum.utility.player.PlayerUtility.msg
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
        build()
            .thenAccept { result ->
                success.complete(result)
            }
            .exceptionally { ex ->
                Universum.instance.logger.severe(
                    "Failed to build kit editor schematic: ${ex.message}"
                )

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
        canCustomize: Boolean = false,
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
                        isCancelled = false
                        itemDrop.remove()
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
        // Inventory Drag Event
        // ----------------------------
        event<InventoryDragEvent>(EventPriority.HIGH) {

            val player = whoClicked as? Player
                ?: return@event

            if (!Universum.lobbyManager.isPlayerInLobby(player) ||
                !isEditor(player.uniqueId)
            ) {
                return@event
            }

            val kitEditor = getKitEditor(player)

            if (kitEditor == null) {
                isCancelled = true

                player.sendMessage(
                    Component.text(
                        "You are unable to do that. Please contact an admin. (Error: Kit Editor hl1)",
                        Colours.RED
                    )
                )

                return@event
            }

            if (inventory.type == InventoryType.CRAFTING) {
                isCancelled = true
                return@event
            }

            // ============================================================
            // ITEMS MENU GUI
            // ============================================================

            if (ItemsMenuGui.isItemsMenuGui(player)) {

                /*
                 * Never allow dragging into the menu itself.
                 */
                if (rawSlots.any { it < player.openInventory.topInventory.size }) {
                    isCancelled = true
                    return@event
                }

                /*
                 * The player inventory is still a normal inventory.
                 *
                 * canCustomize controls whether the player can modify
                 * their kit inventory.
                 */
                if (kitEditor.canCustomize) {
                    return@event
                }

                isCancelled = true
                return@event
            }
            // ============================================================
            // ITEMS GUI
            // ============================================================

            if (ItemsGui.isItemsGui(player)) {

                val touchesMaterialSlot =
                    rawSlots.any { rawSlot ->
                        ItemsGui.isMaterialSlot(player, rawSlot)
                    }

                if (touchesMaterialSlot) {

                    isCancelled = false

                    Bukkit.getScheduler().runTask(
                        Universum.instance,
                        Runnable {
                            ItemsGui.restoreMaterialSlots(player)
                        }
                    )

                    return@event
                }

                /*
                 * Don't allow a drag into any other GUI slot.
                 */
                if (rawSlots.any {
                        it < player.openInventory.topInventory.size
                    }
                ) {
                    isCancelled = true
                    return@event
                }

                /*
                 * Player inventory.
                 *
                 * canCustomize controls whether the player can modify
                 * their kit inventory.
                 */
                if (kitEditor.canCustomize) {
                    return@event
                }

                isCancelled = true
                return@event
            }

            // ============================================================
            // MODIFYING ITEMS
            // ============================================================

            if(AttributesGui.isAttributesGui(player)) {
                isCancelled = true
                return@event
            }

            // ============================================================
            // NORMAL KIT EDITOR
            // ============================================================

            if (!kitEditor.canCustomize) {

                isCancelled = true

                player.closeInventory()

                player.sendMessage(
                    Component.text(
                        "This kit does not allow customization.",
                        Colours.YELLOW
                    )
                )

                player.noSound()

                return@event
            }

            /*
             * Player is allowed to customize their inventory.
             *
             * This intentionally allows normal Bukkit drag behaviour:
             * - moving items
             * - splitting stacks
             * - spreading items across slots
             * - dragging a stack across multiple slots
             * - etc.
             */
            return@event
        }

        // ----------------------------
        // Inventory Click Event
        // ----------------------------
        event<InventoryClickEvent>(EventPriority.HIGH) {

            val player = getPlayer()

            if (!Universum.lobbyManager.isPlayerInLobby(player) ||
                !isEditor(player.uniqueId)
            ) {
                return@event
            }

            val kitEditor = getKitEditor(player)

            if (kitEditor == null) {
                isCancelled = true

                player.sendMessage(
                    Component.text(
                        "You are unable to do that. Please contact an admin. (Error: Kit Editor hl1)",
                        Colours.RED
                    )
                )

                return@event
            }

            if (clickedInventory?.type == InventoryType.CRAFTING) {
                isCancelled = true
                return@event
            }

            // ============================================================
            // ITEMS MENU GUI
            // ============================================================

            if (ItemsMenuGui.isItemsMenuGui(player)) {

                /*
                 * Menu itself is completely read-only.
                 */
                if (rawSlot < player.openInventory.topInventory.size) {
                    isCancelled = true
                    return@event
                }

                /*
                 * Player inventory.
                 *
                 * canCustomize controls whether the player can modify
                 * their kit inventory.
                 */
                if (clickedInventory?.type == InventoryType.PLAYER) {

                    if(isShiftClick) {
                        isCancelled = true
                        return@event
                    }

                    if (kitEditor.canCustomize) {
                        return@event
                    }

                    isCancelled = true
                    return@event
                }

                isCancelled = true
                return@event
            }

            // ============================================================
            // ITEMS GUI
            // ============================================================

            if (ItemsGui.isItemsGui(player)) {

                if (slotType == InventoryType.SlotType.OUTSIDE) {
                    if (kitEditor.itemAccess) {
                        setCursor(null)
                        return@event
                    }
                }

                /*
                 * --------------------------------------------------------
                 * Material source slot
                 * --------------------------------------------------------
                 */
                if (ItemsGui.isMaterialSlot(player, rawSlot)) {

                    if (!player.itemOnCursor.type.isAir) {
                        isCancelled = true
                        return@event
                    }

                    if (click == ClickType.NUMBER_KEY
                        || click == ClickType.NUMBER_KEY
                        || click == ClickType.CONTROL_DROP
                        || click == ClickType.DOUBLE_CLICK
                        || click == ClickType.SWAP_OFFHAND) {
                        isCancelled = true
                        return@event
                    }

                    val material = ItemsGui.getMaterial(player, rawSlot)

                    if (material == null) {
                        isCancelled = true
                        return@event
                    }

                    if (click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT) {
                        isCancelled = true

                        val itemToAdd = ItemStack(material)
                        val inv = player.inventory

                        val preferredSlots = (9..35) + (0..8)

                        var remaining = itemToAdd.amount

                        for (slot in preferredSlots) {
                            val currentStack = inv.getItem(slot)
                            if (currentStack != null && currentStack.isSimilar(itemToAdd) && currentStack.amount < currentStack.maxStackSize) {
                                val spaceLeft = currentStack.maxStackSize - currentStack.amount
                                val toAdd = minOf(remaining, spaceLeft)
                                currentStack.amount += toAdd
                                remaining -= toAdd
                                if (remaining <= 0) break
                            }
                        }

                        if (remaining > 0) {
                            for (slot in preferredSlots) {
                                val currentStack = inv.getItem(slot)
                                if (currentStack == null || currentStack.type.isAir) {
                                    val newStack = itemToAdd.clone()
                                    newStack.amount = remaining
                                    inv.setItem(slot, newStack)
                                    remaining = 0
                                    break
                                }
                            }
                        }

                        return@event
                    }

                    /*
                     * Normal click:
                     * Allow the normal chest-style transaction.
                     */
                    isCancelled = false

                    Bukkit.getScheduler().runTask(
                        Universum.instance,
                        Runnable {
                            /*
                             * Replace the item on the cursor with a clean material.
                             */
                            if (!player.itemOnCursor.type.isAir) {
                                setCursor(ItemStack(material))
                            }

                            /*
                             * Restore the GUI source item.
                             */
                            ItemsGui.restoreMaterialSlots(player)
                        }
                    )

                    return@event
                }


                /*
                 * --------------------------------------------------------
                 * Other GUI slots
                 * --------------------------------------------------------
                 */

                if (rawSlot < player.openInventory.topInventory.size) {
                    isCancelled = true
                    return@event
                }


                /*
                 * --------------------------------------------------------
                 * Player inventory
                 * --------------------------------------------------------
                 */

                if (clickedInventory?.type == InventoryType.PLAYER) {

                    if(isShiftClick) {
                        isCancelled = true
                        return@event
                    }

                    if (kitEditor.itemAccess) {
                        return@event
                    }

                    isCancelled = true
                    return@event
                }

                return@event
            }

            // ============================================================
            // MODIFYING ITEMS
            // ============================================================

            if(AttributesGui.isAttributesGui(player)) {
                isCancelled = true
                return@event
            }

            if (
                currentItem != null
                && currentItem?.type != Material.AIR
                && isShiftClick
                && player.inventory == clickedInventory) {

                if (!kitEditor.canCustomize || !kitEditor.itemAccess) {
                    return@event
                }

                if (currentItem?.hasAdditionalAttributes() == true) {
                    isCancelled = true

                    val itemToEdit = currentItem ?: return@event

                    AttributesGui.open(player, itemToEdit) { newItem ->
                        currentItem = newItem
                        player.updateInventory()
                    }
                }
            }

            // ============================================================
            // NORMAL KIT EDITOR
            // ============================================================

            /*
             * Click outside inventory.
             */
            if (slotType == InventoryType.SlotType.OUTSIDE) {

                if (kitEditor.itemAccess) {
                    setCursor(null)
                    return@event
                }

                isCancelled = true

                Bukkit.getScheduler().runTask(
                    Universum.instance,
                    Runnable {
                        player.closeInventory()
                    }
                )

                player.sendMessage(
                    Component.text(
                        "Please do not attempt to drop items.",
                        Colours.YELLOW
                    )
                )

                player.noSound()

                return@event
            }


            /*
             * Customization disabled.
             */
            if (!kitEditor.canCustomize) {

                isCancelled = true

                player.closeInventory()

                player.sendMessage(
                    Component.text(
                        "This kit does not allow customization.",
                        Colours.YELLOW
                    )
                )

                player.noSound()

                return@event
            }
        }

        event<InventoryCloseEvent>(EventPriority.MONITOR) {
            val player = player as Player

            if (!Universum.lobbyManager.isPlayerInLobby(player) ||
                !isEditor(player.uniqueId)
            ) {
                return@event
            }

            ItemsMenuGui.removeViewer(player)
        }

        // ----------------------------
        // Swap Main Hand / Offhand
        // ----------------------------
        event<PlayerSwapHandItemsEvent>(EventPriority.HIGHEST) {

            if (!Universum.lobbyManager.isPlayerInLobby(player) ||
                !isEditor(player.uniqueId)
            ) {
                return@event
            }

            val kitEditor = getKitEditor(player) ?: return@event

            if (kitEditor.canCustomize) {
                /*
                 * Allow the vanilla F keybind to swap the main hand
                 * and offhand.
                 */
                isCancelled = false
            }
        }

        // ----------------------------
        // Armor Equip / Swap
        // ----------------------------
        event<PlayerInteractEvent>(EventPriority.HIGHEST) {

            if (!Universum.lobbyManager.isPlayerInLobby(player) ||
                !isEditor(player.uniqueId)
            ) {
                return@event
            }

            val kitEditor = getKitEditor(player) ?: return@event

            if (!kitEditor.canCustomize) {
                return@event
            }

            if (action != org.bukkit.event.block.Action.RIGHT_CLICK_AIR &&
                action != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK
            ) {
                return@event
            }

            val item = item ?: return@event

            if (item.type.isAir) {
                return@event
            }

            val isArmor = item.type.name.endsWith("_HELMET") ||
                    item.type.name.endsWith("_CHESTPLATE") ||
                    item.type.name.endsWith("_LEGGINGS") ||
                    item.type.name.endsWith("_BOOTS") ||
                    item.type == org.bukkit.Material.TURTLE_HELMET ||
                    item.type == org.bukkit.Material.ELYTRA

            if (!isArmor) {
                return@event
            }

            /*
             * Prevent the block from being interacted with.
             *
             * This is important because the LobbyManager already cancelled
             * the event at HIGH, and we only want to re-enable the item use,
             * NOT the clicked block.
             */
            if (action == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
                setUseInteractedBlock(org.bukkit.event.Event.Result.DENY)
            }

            /*
             * Allow vanilla armor equipping/swapping.
             */
            setUseItemInHand(org.bukkit.event.Event.Result.ALLOW)
        }

        event<PlayerQuitEvent>(EventPriority.HIGHEST) {

            val uuid = player.uniqueId

            if (!isEditor(uuid)) {
                return@event
            }

            val kitEditor = getKitEditor(uuid)

            removeEditor(uuid)
            kitEditors.remove(uuid)

            if (kitEditor != null) {
                PlayerIsolationUtility.unisolatePlayer(player)
                ItemsGui.clear(player)
                AttributesGui.clear(player)
            }
        }

        event<PlayerKickEvent>(EventPriority.HIGHEST) {

            val uuid = player.uniqueId

            if (!isEditor(uuid)) {
                return@event
            }

            removeEditor(uuid)
            kitEditors.remove(uuid)

            PlayerIsolationUtility.unisolatePlayer(player)
            ItemsGui.clear(player)
            AttributesGui.clear(player)
        }

    }

    // map

    private fun build(): CompletableFuture<Boolean> {

        val targetLocationFuture = DB_KitEditor.getCenterLocation()
        val schematicBytesFuture = DB_KitEditor.getSchematicBytes()

        return CompletableFuture
            .allOf(
                targetLocationFuture,
                schematicBytesFuture
            )
            .thenCompose {

                val targetLocation = targetLocationFuture.join()
                val schematicBytes = schematicBytesFuture.join()

                Universum.instance.logger.info(
                    "[KitEditor] Build requested: " +
                            "target=${targetLocation.world?.name} " +
                            "${targetLocation.x}, " +
                            "${targetLocation.y}, " +
                            "${targetLocation.z}, " +
                            "schematicBytes=${schematicBytes?.size ?: 0}"
                )

                if (schematicBytes == null || schematicBytes.isEmpty()) {

                    Universum.instance.logger.warning(
                        "[KitEditor] No schematic found in database."
                    )

                    return@thenCompose CompletableFuture.completedFuture(false)
                }

                val world = targetLocation.world

                if (world == null) {

                    Universum.instance.logger.severe(
                        "[KitEditor] Target world is null."
                    )

                    return@thenCompose CompletableFuture.completedFuture(false)
                }

                pasteSchematicTo(
                    schematicBytes = schematicBytes,
                    targetLocation = targetLocation,
                    ignoreAir = false
                )
            }
            .exceptionally { ex ->

                Universum.instance.logger.severe(
                    "[KitEditor] Build failed: " +
                            "${ex.cause?.message ?: ex.message}"
                )

                ex.printStackTrace()

                false
            }
    }

}