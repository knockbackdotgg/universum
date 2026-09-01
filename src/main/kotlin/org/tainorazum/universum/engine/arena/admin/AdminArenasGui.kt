package org.tainorazum.universum.engine.arena.admin

import gg.flyte.twilight.gui.GUI
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.tainorazum.universum.Universum
import org.tainorazum.universum.engine.arena.database.ArenaDatabaseManager
import org.tainorazum.universum.engine.arena.database.ArenaTemplate
import org.tainorazum.universum.engine.arena.gui.admin.utility.AdminArenaEditGui
import org.tainorazum.universum.utility.gui.GuiUtility.item
import org.tainorazum.universum.utility.other.open
import org.tainorazum.universum.utility.text.Colours
import org.tainorazum.universum.utility.text.Text
import org.tainorazum.universum.utility.text.norm
import java.util.concurrent.CompletableFuture
import kotlin.math.ceil

object AdminArenasGui {

    private val dbManager = ArenaDatabaseManager()

    private val PATTERN = listOf(
        "WWWWWWWWW",
        "WAAAAAAAW",
        "WAAAAAAAW",
        "WAAAAAAAW",
        "WWWWWWWWW",
        "B#######P"
    )

    private val ARENA_SLOTS: List<Int> by lazy {
        PATTERN.flatMapIndexed { rowIndex, row ->
            row.mapIndexedNotNull { colIndex, char ->
                if (char == 'A') (rowIndex * 9) + colIndex else null
            }
        }
    }

    private val PAGE_SIZE: Int get() = ARENA_SLOTS.size

    fun gui(player: Player, page: Int = 1, previous: GUI? = null): CompletableFuture<GUI> {
        return dbManager.getAllArenaTemplates().thenApply { allArenas ->
            val totalPages = maxOf(1, ceil(allArenas.size.toDouble() / PAGE_SIZE).toInt())
            val currentPage = page.coerceIn(1, totalPages)

            val titleText = "admin arenas » management"

            gg.flyte.twilight.gui.gui(Text.toSmallTextComponent(titleText, Colours.GREY), 54) {
                pattern(*PATTERN.toTypedArray())

                set('W', item(Material.WHITE_STAINED_GLASS_PANE, Component.empty(), "")) { isCancelled = true }

                val backLore = listOf(
                    Component.text(if (previous != null) "Click to return to previous menu." else "Click to close this menu.", Colours.DARK_GREY).norm()
                )
                set('B', item(Material.ARROW, Text.toSmallTextComponent(if (previous != null) "Go Back" else "Close", Colours.RED), backLore)) {
                    isCancelled = true
                    if (previous != null) previous.open(player) else player.closeInventory()
                }

                val prevPageText = if (currentPage > 1) "${currentPage - 1}" else "None"
                val nextPageText = if (currentPage < totalPages) "${currentPage + 1}" else "None"
                val pageLore = listOf(
                    Component.empty(),
                    Component.text("Current Page » $currentPage / $totalPages", Colours.YELLOW).norm(),
                    Component.text("Previous Page » $prevPageText", Colours.GREY).norm(),
                    Component.text("Next Page » $nextPageText", Colours.GREY).norm(),
                    Component.empty(),
                    Component.text("Left-Click for Next Page", Colours.GREEN).norm(),
                    Component.text("Right-Click for Previous Page", Colours.RED).norm()
                )

                set('P', item(Material.PAPER, Text.toSmallTextComponent("Page Navigation", Colours.YELLOW), pageLore)) {
                    isCancelled = true
                    if (isLeftClick && currentPage < totalPages) {
                        openGUI(player, currentPage + 1, previous)
                    } else if (isRightClick && currentPage > 1) {
                        openGUI(player, currentPage - 1, previous)
                    }
                }

                val startIndex = (currentPage - 1) * PAGE_SIZE
                val arenasForPage = allArenas.drop(startIndex).take(PAGE_SIZE)

                arenasForPage.forEachIndexed { index, arenaTemplate ->
                    val slotIndex = ARENA_SLOTS[index]

                    set(slotIndex, createAdminArenaItem(arenaTemplate)) {
                        isCancelled = true
                        AdminArenaEditGui.open(player, arenaTemplate.id)
                    }
                }
            }
        }
    }

    fun openGUI(player: Player, page: Int = 1, previous: GUI? = null) {
        gui(player, page, previous).thenAccept { gui ->
            Bukkit.getScheduler().runTask(Universum.instance, Runnable {
                gui.open(player)
            })
        }
    }

    private fun createAdminArenaItem(template: ArenaTemplate): ItemStack {
        val loreList = mutableListOf<Component>()

        loreList.add(Component.text("ID » ${template.id}", Colours.GREY).norm())
        loreList.add(Component.text("Radius » ${template.radius}", Colours.GOLD).norm())
        loreList.add(Component.text("Center » ${template.center.blockX}, ${template.center.blockY}, ${template.center.blockZ}", Colours.GREY).norm())
        loreList.add(Component.empty())
        loreList.add(Component.text("Click to edit arena settings!", Colours.YELLOW).norm())

        return item(
            Material.GRASS_BLOCK,
            template.name,
            loreList
        )
    }
}