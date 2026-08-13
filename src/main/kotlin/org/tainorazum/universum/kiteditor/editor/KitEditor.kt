package org.tainorazum.universum.kiteditor.editor

import net.kyori.adventure.text.Component
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.tainorazum.universum.Universum
import org.tainorazum.universum.engine.kit.Kit
import org.tainorazum.universum.engine.kit.database.DB_Kits
import org.tainorazum.universum.kiteditor.database.DB_KitEditor
import org.tainorazum.universum.lobby.database.DB_Lobby
import org.tainorazum.universum.utility.player.PlayerIsolationUtility
import org.tainorazum.universum.utility.player.PlayerUtility
import org.tainorazum.universum.utility.text.Colours
import java.util.UUID
import java.util.concurrent.CompletableFuture

class KitEditor(
    val player: Player,
    val kit: Kit,
    val canCustomize: Boolean = true,
    val itemAccess: Boolean = false,
    val isDefault: Boolean = false
) {

    init {
        enter()
    }

    fun enter() {
        // kit editor: https://github.com/knockbackdotgg/universum/issues/21

        Universum.kitEditorManager.addEditor(player.uniqueId)

        DB_KitEditor.getSpawnpointLocation().thenAccept { loc ->
            Universum.instance.server.scheduler.runTask(Universum.instance, Runnable {
                PlayerIsolationUtility.isolatePlayer(player)
                player.teleport(loc)
                PlayerUtility.reset(player, GameMode.SURVIVAL)

                kit.applyToPlayer(player)
            })
        }

    }

    fun leave() {
        DB_Lobby.getSpawnpointLocation().thenAccept { loc ->
            Universum.instance.server.scheduler.runTask(Universum.instance, Runnable {
                player.teleport(loc)
                PlayerUtility.reset(player, GameMode.SURVIVAL)

                PlayerIsolationUtility.unisolatePlayer(player)
                Universum.kitEditorManager.removeEditor(player.uniqueId)

                clear()
            })
        }
    }

    fun clear() = Universum.kitEditorManager.clearKitEditor(this)

    fun cancel(quit: Boolean = false) {
        if(!quit) {
            leave()
        } else {
            PlayerIsolationUtility.unisolatePlayer(player)
            Universum.kitEditorManager.removeEditor(player.uniqueId)
        }
        clear()
    }

    fun save() {
        val ownerUUID = if (isDefault) null else player.uniqueId

        DB_Kits.saveKitFromPlayer(kit.id, player, ownerUUID).thenAccept { success ->
            if (success) {
                player.sendMessage(Component.text("Your kit has been saved!", Colours.GREEN))
            } else {
                player.sendMessage(Component.text("Error! Unable to save your kit. Please contact a staff member.", Colours.RED))
            }
            leave()
        }
    }

}