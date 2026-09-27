package org.tainorazum.universum.kiteditor.editor.items.gui

import gg.flyte.twilight.gui.GUI
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.tainorazum.universum.Universum
import org.tainorazum.universum.kiteditor.editor.items.KitEditorItemsType
import org.tainorazum.universum.kiteditor.editor.items.database.DB_KE_Items
import org.tainorazum.universum.utility.gui.GuiUtility.item
import org.tainorazum.universum.utility.other.open
import org.tainorazum.universum.utility.text.Colours
import org.tainorazum.universum.utility.text.Text
import org.tainorazum.universum.utility.text.norm
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil

object ItemsGui {

    private val PATTERN = listOf(
        "IIIIIIIII",
        "IIIIIIIII",
        "IIIIIIIII",
        "IIIIIIIII",
        "#########",
        "B#######P"
    )

    /**
     * The actual Bukkit inventory currently being used by each player.
     *
     * This lets us identify ItemsGui without relying on its title.
     */
    private val openInventories =
        ConcurrentHashMap<UUID, Inventory>()

    /**
     * Material represented by each source slot for each player.
     *
     * Example:
     *
     * slot 0 -> DIAMOND_SWORD
     * slot 1 -> NETHERITE_SWORD
     */
    private val openMaterials =
        ConcurrentHashMap<UUID, Map<Int, Material>>()

    /**
     * Pre-calculated inventory slots containing item entries.
     */
    private val ITEM_SLOTS: List<Int> by lazy {
        PATTERN.flatMapIndexed { rowIndex, row ->
            row.mapIndexedNotNull { colIndex, char ->
                if (char == 'I') {
                    (rowIndex * 9) + colIndex
                } else {
                    null
                }
            }
        }
    }

    private val PAGE_SIZE: Int
        get() = ITEM_SLOTS.size


    /**
     * Returns true if the player's currently open top inventory
     * is an ItemsGui inventory.
     */
    fun isItemsGui(player: Player): Boolean {
        val registeredInventory = openInventories[player.uniqueId]
            ?: return false

        return player.openInventory.topInventory === registeredInventory
    }


    /**
     * Returns true if the supplied inventory is the ItemsGui
     * currently registered for this player.
     */
    fun isItemsGui(
        player: Player,
        inventory: Inventory
    ): Boolean {
        return openInventories[player.uniqueId] === inventory
    }


    /**
     * Returns true if this raw slot is one of the material source
     * slots in the player's currently open ItemsGui.
     */
    fun isMaterialSlot(
        player: Player,
        rawSlot: Int
    ): Boolean {

        if (!isItemsGui(player)) {
            return false
        }

        val materialSlots =
            openMaterials[player.uniqueId]
                ?: return false

        return rawSlot in materialSlots
    }


    /**
     * Gets the material represented by a source slot.
     */
    fun getMaterial(
        player: Player,
        rawSlot: Int
    ): Material? {
        return openMaterials[player.uniqueId]
            ?.get(rawSlot)
    }


    /**
     * Restores all material source slots.
     *
     * The source item always remains in the GUI even after the
     * player takes it.
     */
    fun restoreMaterialSlots(player: Player) {

        if (!player.isOnline || !isItemsGui(player)) {
            return
        }

        val materials =
            openMaterials[player.uniqueId]
                ?: return

        val inventory = player.openInventory.topInventory

        materials.forEach { (slot, material) ->
            inventory.setItem(
                slot,
                createMaterialItem(material)
            )
        }
    }


    /**
     * Registers the actual Bukkit inventory currently being shown.
     */
    private fun registerInventory(
        player: Player
    ) {
        val inventory = player.openInventory.topInventory

        openInventories[player.uniqueId] = inventory
    }


    /**
     * Clears all ItemsGui state for a player.
     */
    fun clear(player: Player) {
        openInventories.remove(player.uniqueId)
        openMaterials.remove(player.uniqueId)
    }


    fun gui(
        player: Player,
        type: KitEditorItemsType,
        page: Int = 1,
        previous: GUI? = null
    ): CompletableFuture<GUI> {

        return DB_KE_Items.getMaterials(type).thenApply { materials ->

            val totalPages = maxOf(
                1,
                ceil(materials.size.toDouble() / PAGE_SIZE).toInt()
            )

            val currentPage = page.coerceIn(1, totalPages)

            val titleText =
                "Kit Editor » ${type.name.lowercase().replace('_', ' ')}"

            /*
             * Calculate the materials shown on this page.
             */
            val startIndex =
                (currentPage - 1) * PAGE_SIZE

            val materialsForPage =
                materials
                    .drop(startIndex)
                    .take(PAGE_SIZE)

            /*
             * Remember which material belongs to each source slot.
             */
            openMaterials[player.uniqueId] =
                materialsForPage
                    .mapIndexed { index, material ->
                        ITEM_SLOTS[index] to material
                    }
                    .toMap()

            gg.flyte.twilight.gui.gui(
                Text.toSmallTextComponent(titleText, Colours.GREY),
                54
            ) {

                pattern(*PATTERN.toTypedArray())


                /*
                 * Back / Close button
                 */
                set(
                    'B',
                    item(
                        Material.ARROW,
                        Text.toSmallTextComponent(
                            if (previous != null) "Go Back" else "Close",
                            Colours.RED
                        ),
                        listOf(
                            Component.text(
                                if (previous != null)
                                    "Click to return to the previous menu."
                                else
                                    "Click to close this menu.",
                                Colours.DARK_GREY
                            ).norm()
                        )
                    )
                ) {
                    setCursor(null)
                    isCancelled = true

                    if (previous != null) {
                        clear(player)
                        previous.open(player)
                    } else {
                        clear(player)
                        player.closeInventory()
                    }
                }


                /*
                 * Page navigation
                 */
                val previousPage =
                    if (currentPage > 1) currentPage - 1 else "None"

                val nextPage =
                    if (currentPage < totalPages) currentPage + 1 else "None"

                val pageLore = listOf(
                    Component.empty(),

                    Component.text(
                        "Current Page » $currentPage / $totalPages",
                        Colours.YELLOW
                    ).norm(),

                    Component.text(
                        "Previous Page » $previousPage",
                        Colours.GREY
                    ).norm(),

                    Component.text(
                        "Next Page » $nextPage",
                        Colours.GREY
                    ).norm(),

                    Component.empty(),

                    Component.text(
                        "Left-Click for Next Page",
                        Colours.GREEN
                    ).norm(),

                    Component.text(
                        "Right-Click for Previous Page",
                        Colours.RED
                    ).norm()
                )

                set(
                    'P',
                    item(
                        Material.PAPER,
                        Text.toSmallTextComponent(
                            "Page Navigation",
                            Colours.YELLOW
                        ),
                        pageLore
                    )
                ) {
                    isCancelled = true

                    if (isLeftClick && currentPage < totalPages) {
                        clear(player)

                        openGUI(
                            player,
                            type,
                            currentPage + 1,
                            previous
                        )
                    } else if (isRightClick && currentPage > 1) {
                        clear(player)

                        openGUI(
                            player,
                            type,
                            currentPage - 1,
                            previous
                        )
                    }
                }


                /*
                 * Material source items.
                 */
                materialsForPage.forEachIndexed { index, material ->

                    val slotIndex = ITEM_SLOTS[index]

                    set(
                        slotIndex,
                        createMaterialItem(material)
                    ) {
                        /*
                         * Let Bukkit perform the normal inventory
                         * transaction.
                         */
                        isCancelled = false
                    }
                }


                /*
                 * Empty slots.
                 */
                for (index in materialsForPage.size until ITEM_SLOTS.size) {

                    val slotIndex = ITEM_SLOTS[index]

                    set(
                        slotIndex,
                        item(
                            Material.GRAY_STAINED_GLASS_PANE,
                            Text.toSmallTextComponent(
                                "No Item Available",
                                Colours.DARK_GREY
                            ).norm(),
                            emptyList()
                        )
                    ) {
                        isCancelled = true
                    }
                }
            }
        }
    }


    fun openGUI(
        player: Player,
        type: KitEditorItemsType,
        page: Int = 1,
        previous: GUI? = null
    ) {

        gui(
            player,
            type,
            page,
            previous
        ).thenAccept { gui ->

            Bukkit.getScheduler().runTask(
                Universum.instance,
                Runnable {

                    if (!player.isOnline) {
                        clear(player)
                        return@Runnable
                    }

                    gui.open(player)

                    /*
                     * Register the actual Bukkit inventory after
                     * Twilight has opened it.
                     */
                    registerInventory(player)
                }
            )
        }
    }


    private fun createMaterialItem(
        material: Material
    ): ItemStack {

        val displayName = material.name
            .lowercase()
            .split('_')
            .joinToString(" ") {
                it.replaceFirstChar(Char::uppercaseChar)
            }

        val lore = listOf(
            Component.empty(),
            Component.text(
                "Take this item into your inventory.",
                Colours.DARK_GREY
            ).norm()
        )

        return item(
            material,
            Component.text(
                displayName,
                Colours.YELLOW
            ).norm(),
            lore
        )
    }

    fun isRegistered(player: Player): Boolean {
        return openInventories.containsKey(player.uniqueId)
    }

}