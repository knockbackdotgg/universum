package org.tainorazum.universum.kiteditor.editor.items.gui

import gg.flyte.twilight.gui.GUI
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.tainorazum.universum.cosmetic.gui.CosmeticsGui
import org.tainorazum.universum.cosmetic.util.CosmeticType
import org.tainorazum.universum.kiteditor.editor.items.KitEditorItemsType
import org.tainorazum.universum.utility.gui.GuiUtility
import org.tainorazum.universum.utility.other.open
import org.tainorazum.universum.utility.player.PlayerUtility.sound
import org.tainorazum.universum.utility.text.Colours
import org.tainorazum.universum.utility.text.Text

object ItemsMenuGui {

    fun gui(player: Player, previous: GUI? = null): GUI {
        val titleText = "Kit Editor (items)"

        val pattern = listOf(
            "#########",
            "###123###",
            "#########",
            "###456###",
            "#########"
        )

        /**
         * 1 = ARMOR
         * 2 = WEAPONS
         * 3 = TOOLS
         * 4 = BLOCKS
         * 5 = MISCELLANEOUS
         * 6 =
         */

        return gg.flyte.twilight.gui.gui(Text.toSmallTextComponent(titleText, Colours.GREY), 45) {
            pattern(*pattern.toTypedArray())

            set(
                'E', GuiUtility.item(
                    Material.NAME_TAG, Component.text("Chat Tags", Colours.YELLOW), "See all chat tags."
                )
            ) {
                isCancelled = true
                CosmeticsGui.openGUI(player, CosmeticType.CHAT_TAG, 1, this@gui)
            }

            set(
                '1',
                GuiUtility.item(
                    Material.NETHERITE_HELMET,
                    Component.text("Armor", Colours.GREEN),
                    "All armor options."
                )
            ) {
                isCancelled = true
                ItemsGui.openGUI(player, KitEditorItemsType.ARMOR, previous = this@gui)
                player.sound(Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f)
            }

            // navigation
            if (previous != null) {
                set(
                    'B',
                    GuiUtility.item(
                        Material.ARROW,
                        Text.toSmallTextComponent("Go Back", Colours.RED),
                        "Click to return to the previous menu."
                    )
                ) {
                    isCancelled = true
                    previous.open(player)
                }
            } else {
                set(
                    'B',
                    GuiUtility.item(
                        Material.ARROW,
                        Text.toSmallTextComponent("Close", Colours.RED),
                        "Click to close this menu."
                    )
                ) {
                    isCancelled = true
                    player.closeInventory()
                }
            }


        }

    }

}