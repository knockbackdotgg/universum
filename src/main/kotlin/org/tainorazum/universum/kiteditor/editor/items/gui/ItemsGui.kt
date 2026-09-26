package org.tainorazum.universum.kiteditor.editor.items.gui

import gg.flyte.twilight.gui.GUI
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.tainorazum.universum.Universum
import org.tainorazum.universum.kiteditor.editor.items.KitEditorItemsType
import org.tainorazum.universum.kiteditor.editor.items.database.DB_KE_Items
import org.tainorazum.universum.utility.gui.GuiUtility.item
import org.tainorazum.universum.utility.other.open
import org.tainorazum.universum.utility.text.Colours
import org.tainorazum.universum.utility.text.Text
import org.tainorazum.universum.utility.text.norm
import java.util.concurrent.CompletableFuture
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
                    isCancelled = true

                    if (previous != null) {
                        previous.open(player)
                    } else {
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

                /*
                 * Page button.
                 *
                 * Using the bottom-center slot directly because the
                 * pattern currently has no 'P' character.
                 */
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
                        openGUI(
                            player,
                            type,
                            currentPage + 1,
                            previous
                        )
                    } else if (isRightClick && currentPage > 1) {
                        openGUI(
                            player,
                            type,
                            currentPage - 1,
                            previous
                        )
                    }
                }


                /*
                 * Render materials for the current page.
                 */
                val startIndex =
                    (currentPage - 1) * PAGE_SIZE

                val materialsForPage =
                    materials
                        .drop(startIndex)
                        .take(PAGE_SIZE)

                materialsForPage.forEachIndexed { index, material ->

                    val slotIndex = ITEM_SLOTS[index]

                    set(
                        slotIndex,
                        createMaterialItem(material)
                    ) {
                        isCancelled = false

                        // TODO:
                        // Handle material selection here.
                    }
                }


                /*
                 * Fill unused item slots.
                 *
                 * This prevents stale GUI contents when changing pages
                 * and makes the empty slots visually obvious.
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


    /**
     * Utility method to open the GUI asynchronously and safely switch
     * back to the Bukkit main thread before opening the inventory.
     */
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
                    gui.open(player)
                }
            )
        }
    }


    private fun createMaterialItem(
        material: Material
    ): ItemStack {

        val displayName = material.name
            .lowercase()
            .replace('_', ' ')
            .replaceFirstChar { it.uppercase() }

        val lore = listOf(
            Component.empty(),
            Component.text(
                "Click to select this material.",
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
}