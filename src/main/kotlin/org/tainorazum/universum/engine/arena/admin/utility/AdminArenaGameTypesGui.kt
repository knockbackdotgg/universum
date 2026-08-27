package org.tainorazum.universum.engine.arena.admin.utility

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.tainorazum.universum.Universum
import org.tainorazum.universum.engine.arena.database.ArenaDatabaseManager
import org.tainorazum.universum.engine.arena.gui.admin.utility.AdminArenaEditGui
import org.tainorazum.universum.engine.game.manager.fields.GameType
import org.tainorazum.universum.utility.gui.GuiUtility.item
import org.tainorazum.universum.utility.other.open
import org.tainorazum.universum.utility.text.Colours
import org.tainorazum.universum.utility.text.Text
import org.tainorazum.universum.utility.text.norm

object AdminArenaGameTypesGui {

    private val dbManager = ArenaDatabaseManager()

    fun open(player: Player, arenaId: String) {
        dbManager.getAllowedGameTypes(arenaId).thenAccept { allowedTypes ->
            val title = "Game Types: $arenaId"
            val gui = gg.flyte.twilight.gui.gui(Text.toSmallTextComponent(title, Colours.GREY), 36) {

                GameType.entries.forEachIndexed { index, type ->
                    val isAllowed = allowedTypes.contains(type)
                    val statusColor = if (isAllowed) Colours.GREEN else Colours.RED
                    val statusText = if (isAllowed) "Enabled" else "Disabled"
                    val mat = if (isAllowed) Material.LIME_DYE else Material.GRAY_DYE

                    val lore = listOf(
                        Component.text("Status: $statusText", statusColor).norm(),
                        Component.text("Click to toggle.", Colours.DARK_GREY).norm()
                    )

                    set(index + 10, item(mat, Component.text(type.name, Colours.YELLOW).norm(), lore)) {
                        isCancelled = true
                        val action = if (isAllowed) {
                            dbManager.removeAllowedGameType(arenaId, type)
                        } else {
                            dbManager.addAllowedGameType(arenaId, type)
                        }

                        action.thenAccept {
                            open(player, arenaId)
                        }
                    }
                }

                // Back Button (Slot 27)
                set(27, item(Material.ARROW, Component.text("Back", Colours.RED).norm(), listOf(Component.text("Return to arena editor.", Colours.DARK_GREY).norm()))) {
                    isCancelled = true
                    AdminArenaEditGui.open(player, arenaId)
                }
            }

            Bukkit.getScheduler().runTask(Universum.instance, Runnable { gui.open(player) })
        }
    }
}