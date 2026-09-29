package org.tainorazum.universum.cosmetic.gui.admin.utility

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.tainorazum.universum.Universum
import org.tainorazum.universum.utility.gui.GuiUtility.item
import org.tainorazum.universum.utility.other.open
import org.tainorazum.universum.utility.text.Colours
import org.tainorazum.universum.utility.text.Text
import org.tainorazum.universum.utility.text.norm
import kotlin.math.ceil

object AdminEntityTypeSelectGui {

    private val ENTITY_TYPES: List<EntityType> by lazy {
        EntityType.entries
            .filter { it.isSpawnable && it != EntityType.UNKNOWN }
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
            row.mapIndexedNotNull { colIndex, char -> if (char == 'C') (rowIndex * 9) + colIndex else null }
        }
    }

    private val PAGE_SIZE = SLOTS.size

    fun open(player: Player, page: Int = 1, onSelect: (EntityType) -> Unit) {
        val totalPages = maxOf(1, ceil(ENTITY_TYPES.size.toDouble() / PAGE_SIZE).toInt())
        val currentPage = page.coerceIn(1, totalPages)

        val gui = gg.flyte.twilight.gui.gui(Text.toSmallTextComponent("select entity type ($currentPage/$totalPages)", Colours.GREY), 54) {
            pattern(*PATTERN.toTypedArray())

            set('W', item(Material.WHITE_STAINED_GLASS_PANE, Component.empty(), "")) { isCancelled = true }
            set('B', item(Material.ARROW, Text.toSmallTextComponent("Cancel", Colours.RED), listOf(Component.text("Click to cancel.", Colours.DARK_GREY).norm()))) {
                isCancelled = true
                player.closeInventory()
            }

            val pageLore = listOf(
                Component.empty(),
                Component.text("Current Page » $currentPage / $totalPages", Colours.YELLOW).norm(),
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
            val typesForPage = ENTITY_TYPES.drop(startIndex).take(PAGE_SIZE)

            typesForPage.forEachIndexed { index, entityType ->
                val slot = SLOTS[index]
                val nameFormatted = entityType.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }

                set(slot, item(Material.SPAWNER, Component.text(nameFormatted, Colours.GREEN).norm(), listOf(Component.text("Click to select this entity type.", Colours.DARK_GREY).norm()))) {
                    isCancelled = true
                    onSelect(entityType)
                }
            }
        }

        Bukkit.getScheduler().runTask(Universum.instance, Runnable { gui.open(player) })
    }
}