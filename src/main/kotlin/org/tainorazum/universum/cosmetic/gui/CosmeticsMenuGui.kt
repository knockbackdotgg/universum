package org.tainorazum.universum.cosmetic.gui

import gg.flyte.twilight.gui.GUI
import gg.flyte.twilight.gui.gui
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.tainorazum.universum.cosmetic.util.CosmeticType
import org.tainorazum.universum.utility.gui.GuiUtility.item
import org.tainorazum.universum.utility.other.open
import org.tainorazum.universum.utility.text.Colours
import org.tainorazum.universum.utility.text.Text

object CosmeticsMenuGui {

    fun gui(player: Player, previous: GUI? = null): GUI {
        val titleText = "cosmetics menu"

        val pattern = listOf(
            "#########",
            "#E##P##C#",
            "##T#G#A##",
            "##R###M##",
            "#########",
            "B########"
        )

        /**
         * E = Chat tag
         * C = Chat color
         * P = Pet
         * T = Trail
         * G = Glow
         * A = Aura
         * R = Armor trims
         * M = Armor trim colors
         */

        return gui(Text.toSmallTextComponent(titleText, Colours.GREY), 54) {
            pattern(*pattern.toTypedArray())

            set(
                'E', item(
                Material.NAME_TAG, Component.text("Chat Tags", Colours.YELLOW), "See all chat tags."
            )) {
                isCancelled = true
                CosmeticsGui.openGUI(player, CosmeticType.CHAT_TAG, 1, this@gui)
            }

            set(
                'C', item(
                    Material.RED_DYE, Component.text("Chat Color", Colours.YELLOW), "See all chat colors."
                )) {
                isCancelled = true
                CosmeticsGui.openGUI(player, CosmeticType.CHAT_COLOR, 1, this@gui)
            }

            set(
                'P', item(
                    Material.COOKED_CHICKEN, Component.text("Pets", Colours.YELLOW), "See all pets. Only applies in lobby."
                )) {
                isCancelled = true
                CosmeticsGui.openGUI(player, CosmeticType.PET, 1, this@gui)
            }

            set(
                'T', item(
                    Material.BLAZE_POWDER, Component.text("Trails", Colours.YELLOW), "See all trails. Only applies in lobby."
                )) {
                isCancelled = true
                CosmeticsGui.openGUI(player, CosmeticType.TRAIL, 1, this@gui)
            }

            set(
                'G', item(
                    Material.NETHER_STAR, Component.text("Glows", Colours.YELLOW), "See all glows. Only applies in lobby."
                )) {
                isCancelled = true
                CosmeticsGui.openGUI(player, CosmeticType.GLOW, 1, this@gui)
            }

            set(
                'A', item(
                    Material.COPPER_HELMET, Component.text("Auras", Colours.YELLOW), "See all auras. Only applies in lobby."
                )) {
                isCancelled = true
                CosmeticsGui.openGUI(player, CosmeticType.AURA, 1, this@gui)
            }

            set(
                'R', item(
                    Material.WILD_ARMOR_TRIM_SMITHING_TEMPLATE, Component.text("Armor Trims", Colours.YELLOW), "See all armor trims. Applies to all your armor in games."
                )) {
                isCancelled = true
                CosmeticsGui.openGUI(player, CosmeticType.ARMOR_TRIMS, 1, this@gui)
            }

            set(
                'M', item(
                    Material.IRON_INGOT, Component.text("Armor Trim Colors", Colours.YELLOW), "See all armor trim colors. Applies to all your armor in games."
                )) {
                isCancelled = true
                CosmeticsGui.openGUI(player, CosmeticType.ARMOR_TRIM_COLOR, 1, this@gui)
            }

            // navigation
            if (previous != null) {
                set(
                    'B',
                    item(
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
                    item(
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