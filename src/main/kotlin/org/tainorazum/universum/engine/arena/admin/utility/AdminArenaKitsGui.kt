package org.tainorazum.universum.engine.arena.gui.admin.utility

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.tainorazum.universum.Universum
import org.tainorazum.universum.engine.arena.database.ArenaDatabaseManager
import org.tainorazum.universum.utility.gui.AnvilInput
import org.tainorazum.universum.utility.gui.GuiUtility.item
import org.tainorazum.universum.utility.other.open
import org.tainorazum.universum.utility.text.Colours
import org.tainorazum.universum.utility.text.Prefix
import org.tainorazum.universum.utility.text.Text
import org.tainorazum.universum.utility.text.norm
import kotlin.math.ceil

object AdminArenaKitsGui {

    private val dbManager = ArenaDatabaseManager()
    private val contentSlots = (10..16) + (19..25) + (28..34) + (37..43) // 28 items per page

    fun open(player: Player, arenaId: String, page: Int = 1) {
        dbManager.getAllowedKits(arenaId).thenAccept { allowedKits ->
            val totalPages = maxOf(1, ceil(allowedKits.size.toDouble() / contentSlots.size).toInt())
            val currentPage = page.coerceIn(1, totalPages)

            val title = "Allowed Kits: $arenaId ($currentPage/$totalPages)"
            val gui = gg.flyte.twilight.gui.gui(Text.toSmallTextComponent(title, Colours.GREY), 54) {

                // Display active allowed kits across defined content slots
                val pageItems = allowedKits.drop((currentPage - 1) * contentSlots.size).take(contentSlots.size)
                pageItems.forEachIndexed { index, kitId ->
                    val slot = contentSlots[index]
                    val lore = listOf(
                        Component.text("Status: Allowed", Colours.GREEN).norm(),
                        Component.empty(),
                        Component.text("Click to remove kit.", Colours.RED).norm()
                    )

                    set(slot, item(Material.CHEST, Component.text(kitId, Colours.YELLOW).norm(), lore)) {
                        isCancelled = true
                        dbManager.removeAllowedKit(arenaId, kitId).thenAccept {
                            open(player, arenaId, currentPage)
                        }
                    }
                }

                // Previous Page (Slot 45)
                if (currentPage > 1) {
                    set(45, item(Material.ARROW, Component.text("Previous Page", Colours.YELLOW).norm(), listOf(Component.text("Go to page ${currentPage - 1}", Colours.DARK_GREY).norm()))) {
                        isCancelled = true
                        open(player, arenaId, currentPage - 1)
                    }
                }

                // Add Kit Button (Slot 48)
                set(48, item(Material.NETHER_STAR, Component.text("Add Kit", Colours.GREEN).norm(), listOf(Component.text("Click to enter kit ID.", Colours.DARK_GREY).norm()))) {
                    isCancelled = true
                    AnvilInput.openTextInput(player, "Enter Kit ID", "") { kitId ->
                        val trimmedId = kitId.trim()
                        if (trimmedId.isNotBlank()) {
                            dbManager.addAllowedKit(arenaId, trimmedId).thenAccept { success ->
                                Bukkit.getScheduler().runTask(Universum.instance, Runnable {
                                    if (!success) {
                                        player.sendMessage(
                                            Prefix.ADMIN()
                                                .append(Prefix.ARROW3())
                                                .append(Component.text("Kit '$trimmedId' does not exist.", Colours.RED))
                                        )
                                    }
                                    open(player, arenaId, currentPage)
                                })
                            }
                        } else {
                            open(player, arenaId, currentPage)
                        }
                    }
                }

                // Back Button (Slot 49)
                set(49, item(Material.BARRIER, Component.text("Back", Colours.RED).norm(), listOf(Component.text("Return to arena editor.", Colours.DARK_GREY).norm()))) {
                    isCancelled = true
                    AdminArenaEditGui.open(player, arenaId)
                }

                // Next Page (Slot 53)
                if (currentPage < totalPages) {
                    set(53, item(Material.ARROW, Component.text("Next Page", Colours.YELLOW).norm(), listOf(Component.text("Go to page ${currentPage + 1}", Colours.DARK_GREY).norm()))) {
                        isCancelled = true
                        open(player, arenaId, currentPage + 1)
                    }
                }
            }

            Bukkit.getScheduler().runTask(Universum.instance, Runnable { gui.open(player) })
        }
    }
}