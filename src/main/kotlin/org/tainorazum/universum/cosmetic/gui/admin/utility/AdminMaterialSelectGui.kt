package org.tainorazum.universum.cosmetic.gui.admin.utility

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.tainorazum.universum.Universum
import org.tainorazum.universum.utility.gui.GuiUtility.item
import org.tainorazum.universum.utility.other.open
import org.tainorazum.universum.utility.text.Colours
import org.tainorazum.universum.utility.text.Text
import org.tainorazum.universum.utility.text.norm
import kotlin.math.ceil

object AdminMaterialSelectGui {

    private val MATERIALS: List<Material> by lazy {
        Material.entries
            .filter { it.isItem && !it.isAir }
            .sortedBy { it.name }
    }

    private val PATTERN = listOf(
        "WWWWWWWWW",
        "WCCCCCCCW",
        "WCCCCCCCW",
        "WCCCCCCCW",
        "WWWWWWWWW",
        "B#######P"
    )

    private val SLOTS: List<Int> by lazy {
        PATTERN.flatMapIndexed { rowIndex, row ->
            row.mapIndexedNotNull { colIndex, char ->
                if (char == 'C') (rowIndex * 9) + colIndex else null
            }
        }
    }

    private val PAGE_SIZE = SLOTS.size

    fun open(player: Player, page: Int = 1, onSelect: (Material) -> Unit) {
        val totalPages = maxOf(1, ceil(MATERIALS.size.toDouble() / PAGE_SIZE).toInt())
        val currentPage = page.coerceIn(1, totalPages)

        val gui = gg.flyte.twilight.gui.gui(Text.toSmallTextComponent("select material ($currentPage/$totalPages)", Colours.GREY), 54) {
            pattern(*PATTERN.toTypedArray())

            set('W', item(Material.WHITE_STAINED_GLASS_PANE, Component.empty(), "")) { isCancelled = true }

            set('B', item(Material.ARROW, Text.toSmallTextComponent("Cancel", Colours.RED), listOf(Component.text("Click to cancel selection.", Colours.DARK_GREY).norm()))) {
                isCancelled = true
                player.closeInventory()
            }

            // Pagination
            val prevText = if (currentPage > 1) "${currentPage - 1}" else "None"
            val nextText = if (currentPage < totalPages) "${currentPage + 1}" else "None"
            val pageLore = listOf(
                Component.empty(),
                Component.text("Current Page » $currentPage / $totalPages", Colours.YELLOW).norm(),
                Component.text("Previous Page » $prevText", Colours.GREY).norm(),
                Component.text("Next Page » $nextText", Colours.GREY).norm(),
                Component.empty(),
                Component.text("Left-Click for Next Page", Colours.GREEN).norm(),
                Component.text("Right-Click for Previous Page", Colours.RED).norm()
            )

            set('P', item(Material.PAPER, Text.toSmallTextComponent("Page Navigation", Colours.YELLOW), pageLore)) {
                isCancelled = true
                if (isLeftClick && currentPage < totalPages) open(player, currentPage + 1, onSelect)
                else if (isRightClick && currentPage > 1) open(player, currentPage - 1, onSelect)
            }

            val startIndex = (currentPage - 1) * PAGE_SIZE
            val materialsForPage = MATERIALS.drop(startIndex).take(PAGE_SIZE)

            materialsForPage.forEachIndexed { index, mat ->
                val slot = SLOTS[index]
                val nameFormatted = mat.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }

                set(slot, item(mat, Component.text(nameFormatted, Colours.GREEN).norm(), listOf(Component.text("Click to select this material.", Colours.DARK_GREY).norm()))) {
                    isCancelled = true
                    onSelect(mat)
                }
            }
        }

        Bukkit.getScheduler().runTask(Universum.instance, Runnable { gui.open(player) })
    }
}