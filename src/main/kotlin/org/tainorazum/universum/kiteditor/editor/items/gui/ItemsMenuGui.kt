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
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object ItemsMenuGui {

    private val viewers = ConcurrentHashMap.newKeySet<UUID>()

    private const val TITLE = "Kit Editor (items)"

    private val PATTERN = listOf(
        "#########",
        "#123#456#",
        "#########",
    )

    /**
     * 1 = ARMOR
     * 2 = WEAPONS
     * 3 = TOOLS
     * 4 = BLOCKS
     * 5 = MISCELLANEOUS
     * 6 = UNUSED
     */

    fun isItemsMenuGui(player: Player): Boolean {
        return player.uniqueId in viewers
    }

    fun addViewer(player: Player) {
        viewers.add(player.uniqueId)
    }

    fun removeViewer(player: Player) {
        viewers.remove(player.uniqueId)
    }

    fun removeViewer(uuid: UUID) {
        viewers.remove(uuid)
    }

    fun gui(player: Player, previous: GUI? = null): GUI {
        addViewer(player)

        return gg.flyte.twilight.gui.gui(
            Text.toSmallTextComponent(TITLE, Colours.GREY),
            27
        ) {
            pattern(*PATTERN.toTypedArray())

            set(
                'E',
                GuiUtility.item(
                    Material.NAME_TAG,
                    Component.text("Chat Tags", Colours.YELLOW),
                    "See all chat tags."
                )
            ) {
                isCancelled = true

                removeViewer(player)

                CosmeticsGui.openGUI(
                    player,
                    CosmeticType.CHAT_TAG,
                    1,
                    this@gui
                )
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

                removeViewer(player)

                ItemsGui.openGUI(
                    player,
                    KitEditorItemsType.ARMOR,
                    previous = this@gui
                )

                player.sound(
                    Sound.BLOCK_CHEST_OPEN,
                    1.0f,
                    1.0f
                )
            }

            set(
                '2',
                GuiUtility.item(
                    Material.IRON_SWORD,
                    Component.text("Weapons", Colours.GREEN),
                    "All weapon options."
                )
            ) {
                isCancelled = true

                removeViewer(player)

                ItemsGui.openGUI(
                    player,
                    KitEditorItemsType.WEAPONS,
                    previous = this@gui
                )

                player.sound(
                    Sound.BLOCK_CHEST_OPEN,
                    1.0f,
                    1.0f
                )
            }

            set(
                '3',
                GuiUtility.item(
                    Material.DIAMOND_PICKAXE,
                    Component.text("Tools", Colours.GREEN),
                    "All tool options."
                )
            ) {
                isCancelled = true

                removeViewer(player)

                ItemsGui.openGUI(
                    player,
                    KitEditorItemsType.TOOLS,
                    previous = this@gui
                )

                player.sound(
                    Sound.BLOCK_CHEST_OPEN,
                    1.0f,
                    1.0f
                )
            }

            set(
                '4',
                GuiUtility.item(
                    Material.GRASS_BLOCK,
                    Component.text("Blocks", Colours.GREEN),
                    "All block options."
                )
            ) {
                isCancelled = true

                removeViewer(player)

                ItemsGui.openGUI(
                    player,
                    KitEditorItemsType.BLOCKS,
                    previous = this@gui
                )

                player.sound(
                    Sound.BLOCK_CHEST_OPEN,
                    1.0f,
                    1.0f
                )
            }

            set(
                '5',
                GuiUtility.item(
                    Material.FIREWORK_ROCKET,
                    Component.text("Miscellaneous", Colours.GREEN),
                    "All miscellaneous options."
                )
            ) {
                isCancelled = true

                removeViewer(player)

                ItemsGui.openGUI(
                    player,
                    KitEditorItemsType.MISCELLANEOUS,
                    previous = this@gui
                )

                player.sound(
                    Sound.BLOCK_CHEST_OPEN,
                    1.0f,
                    1.0f
                )
            }

            set(
                '6',
                GuiUtility.item(
                    Material.RED_BUNDLE,
                    Component.text("Previews", Colours.GREEN),
                    "More Knockback.gg server previews????"
                )
            ) {
                isCancelled = true

                removeViewer(player)

                ItemsGui.openGUI(
                    player,
                    KitEditorItemsType.BLOCKS,
                    previous = this@gui
                )

                player.sound(
                    Sound.BLOCK_CHEST_OPEN,
                    1.0f,
                    1.0f
                )
            }

            if (previous != null) {
                set(
                    'B',
                    GuiUtility.item(
                        Material.ARROW,
                        Text.toSmallTextComponent(
                            "Go Back",
                            Colours.RED
                        ),
                        "Click to return to the previous menu."
                    )
                ) {
                    isCancelled = true

                    removeViewer(player)

                    previous.open(player)
                }
            } else {
                set(
                    'B',
                    GuiUtility.item(
                        Material.ARROW,
                        Text.toSmallTextComponent(
                            "Close",
                            Colours.RED
                        ),
                        "Click to close this menu."
                    )
                ) {
                    isCancelled = true

                    removeViewer(player)

                    player.closeInventory()
                }
            }
        }
    }

    fun openGUI(
        player: Player,
        previous: GUI? = null
    ) {
        gui(player, previous).open(player)
    }
}