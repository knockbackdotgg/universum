package org.tainorazum.universum.kiteditor.gui

import gg.flyte.twilight.gui.GUI
import gg.flyte.twilight.gui.gui
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.tainorazum.universum.Universum
import org.tainorazum.universum.engine.kit.Kit
import org.tainorazum.universum.engine.kit.database.DB_Kits
import org.tainorazum.universum.utility.gui.GuiUtility.item
import org.tainorazum.universum.utility.other.open
import org.tainorazum.universum.utility.text.Colours
import org.tainorazum.universum.utility.text.Text
import org.tainorazum.universum.utility.text.norm
import java.util.concurrent.CompletableFuture

object KitEditorGui {

    fun gui(player: Player, previous: GUI? = null): CompletableFuture<GUI> {
        val titleText = "Kit Editor (temp gui)"

        val pattern = listOf(
            "#########",
            "#KKKKKKK#",
            "#KKKKKKK#",
            "#KKKKKKK#",
            "#########",
            "B########"
        )

        val kitSlots = pattern.flatMapIndexed { rowIndex, row ->
            row.mapIndexedNotNull { colIndex, char ->
                if (char == 'K') (rowIndex * 9) + colIndex else null
            }
        }

        return DB_Kits.getAllDefaultKits().thenApply { kits ->
            gui(Text.toSmallTextComponent(titleText, Colours.GREY), 54) {
                pattern(*pattern.toTypedArray())

                if (previous != null) {
                    set('B', item(Material.ARROW, Text.toSmallTextComponent("Go Back", Colours.RED), "Click to return to the previous menu.")) {
                        isCancelled = true
                        previous.open(player)
                    }
                } else {
                    set('B', item(Material.ARROW, Text.toSmallTextComponent("Close", Colours.RED), "Click to close this menu.")) {
                        isCancelled = true
                        player.closeInventory()
                    }
                }

                kitSlots.forEachIndexed { index, slotIndex ->
                    if (index < kits.size) {
                        val kit = kits[index]

                        set(slotIndex, createKitItem(kit)) {
                            isCancelled = true
                            player.closeInventory()

                            Universum.kitEditorManager.editKit(kit, null)
                        }
                    } else {
                        set(slotIndex, item(Material.BARRIER, Text.toSmallTextComponent("No Kit Available", Colours.DARK_GREY).norm(), emptyList())) {
                            isCancelled = true
                        }
                    }
                }
            }
        }
    }

    /**
     * Utility method to open the async GUI safely on the Bukkit main thread.
     */
    fun openGUI(player: Player, previous: GUI? = null) {
        gui(player, previous).thenAccept { gui ->
            Bukkit.getScheduler().runTask(Universum.instance, Runnable {
                gui.open(player)
            })
        }
    }

    private fun createKitItem(kit: Kit): ItemStack {
        val loreList = mutableListOf<Component>()

        loreList.add(Component.empty())
        loreList.add(Component.text("Click to edit this kit.", Colours.DARK_GREY).norm())
        loreList.add(Component.empty())

        return item(
            kit.material,
            Component.text(kit.id.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }, Colours.YELLOW).norm(),
            loreList
        )
    }
}