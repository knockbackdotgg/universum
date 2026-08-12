package org.tainorazum.universum.kiteditor.editor

import net.kyori.adventure.text.Component
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.tainorazum.universum.Universum
import org.tainorazum.universum.engine.kit.Kit
import org.tainorazum.universum.engine.kit.database.DB_Kits
import org.tainorazum.universum.lobby.database.DB_Lobby
import org.tainorazum.universum.utility.player.PlayerUtility
import org.tainorazum.universum.utility.text.Colours
import java.util.concurrent.CompletableFuture

class KitEditor(
    val player: Player,
    val kit: Kit,
    val itemAccess: Boolean = false,
    val isDefault: Boolean = false
) {

    init {
        enter()
    }

    fun enter() {
        // kit editor: https://github.com/knockbackdotgg/universum/issues/21
    }

    fun cancel(quit: Boolean = false) {

    }

    fun save() {
        DB_Kits.saveKitFromPlayer(kit.id, player).thenAccept { success ->
            if (success) {
                player.sendMessage(Component.text("Your kit has been saved!", Colours.GREEN))
            } else {
                player.sendMessage(Component.text("Error! Unable to save your kit. Please contact a staff member.", Colours.RED))
            }

            player.gameMode = GameMode.SPECTATOR

            DB_Lobby.getSpawnpointLocation().thenAccept { loc ->
                Universum.instance.server.scheduler.runTask(Universum.instance, Runnable {
                    player.teleport(loc)
                    PlayerUtility.reset(player, GameMode.SURVIVAL)
                })
            }
            Universum.kitEditorManager.removeEditor(player.uniqueId)
        }
    }

}