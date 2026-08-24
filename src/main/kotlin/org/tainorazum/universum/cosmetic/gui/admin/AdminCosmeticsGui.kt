package org.tainorazum.universum.cosmetic.gui.admin

import gg.flyte.twilight.gui.GUI
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.tainorazum.universum.Universum
import org.tainorazum.universum.cosmetic.database.CosmeticEntry
import org.tainorazum.universum.cosmetic.database.DB_Cosmetics
import org.tainorazum.universum.cosmetic.gui.admin.utility.AdminCosmeticEditGui
import org.tainorazum.universum.cosmetic.util.CosmeticType
import org.tainorazum.universum.utility.gui.GuiUtility.item
import org.tainorazum.universum.utility.other.open
import org.tainorazum.universum.utility.text.Colours
import org.tainorazum.universum.utility.text.Text
import org.tainorazum.universum.utility.text.norm
import java.util.concurrent.CompletableFuture
import kotlin.math.ceil

object AdminCosmeticsGui {

    private val PATTERN = listOf(
        "WWWWWWWWW",
        "WCCCCCCCW",
        "WCCCCCCCW",
        "WCCCCCCCW",
        "WWWWWWWWW",
        "B#######P"
    )

    private val COSMETIC_SLOTS: List<Int> by lazy {
        PATTERN.flatMapIndexed { rowIndex, row ->
            row.mapIndexedNotNull { colIndex, char ->
                if (char == 'C') (rowIndex * 9) + colIndex else null
            }
        }
    }

    private val PAGE_SIZE: Int get() = COSMETIC_SLOTS.size

    fun gui(player: Player, cosmeticType: CosmeticType, page: Int = 1, previous: GUI? = null): CompletableFuture<GUI> {
        return DB_Cosmetics.getCosmeticsByType(cosmeticType).thenApply { allCosmetics ->
            val totalPages = maxOf(1, ceil(allCosmetics.size.toDouble() / PAGE_SIZE).toInt())
            val currentPage = page.coerceIn(1, totalPages)

            val titleText = "admin cosmetics » ${cosmeticType.name.lowercase().replace('_', ' ')}"

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
                        openGUI(player, cosmeticType, currentPage + 1, previous)
                    } else if (isRightClick && currentPage > 1) {
                        openGUI(player, cosmeticType, currentPage - 1, previous)
                    }
                }

                val startIndex = (currentPage - 1) * PAGE_SIZE
                val cosmeticsForPage = allCosmetics.drop(startIndex).take(PAGE_SIZE)

                cosmeticsForPage.forEachIndexed { index, cosmeticEntry ->
                    val slotIndex = COSMETIC_SLOTS[index]

                    set(slotIndex, createAdminCosmeticItem(cosmeticEntry)) {
                        isCancelled = true
                        AdminCosmeticEditGui.open(player, cosmeticType, cosmeticEntry.name)
                    }
                }
            }
        }
    }

    fun openGUI(player: Player, cosmeticType: CosmeticType, page: Int = 1, previous: GUI? = null) {
        gui(player, cosmeticType, page, previous).thenAccept { gui ->
            Bukkit.getScheduler().runTask(Universum.instance, Runnable {
                gui.open(player)
            })
        }
    }

    private fun createAdminCosmeticItem(entry: CosmeticEntry): ItemStack {
        val loreList = mutableListOf<Component>()

        if (!entry.description.isNullOrBlank()) {
            loreList.add(Component.text(entry.description, Colours.GREY).norm())
            loreList.add(Component.empty())
        }

        loreList.add(Component.text("Price » ${entry.price} Coins", Colours.GOLD).norm())
        loreList.add(Component.text("Material » ${entry.material.name}", Colours.GREY).norm())
        loreList.add(Component.empty())
        loreList.add(Component.text("Click to edit or delete!", Colours.YELLOW).norm())

        val displayName = entry.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }

        return item(
            entry.material,
            Component.text(displayName, Colours.GREEN).norm(),
            loreList
        )
    }
}