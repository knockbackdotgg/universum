package org.tainorazum.universum.kiteditor.editor

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.tainorazum.universum.Universum
import org.tainorazum.universum.engine.game.manager.fields.GameSettings
import org.tainorazum.universum.engine.kit.Kit
import org.tainorazum.universum.engine.kit.database.DB_Kits
import org.tainorazum.universum.kiteditor.database.DB_KitEditor
import org.tainorazum.universum.kiteditor.editor.items.gui.ItemsGui
import org.tainorazum.universum.kiteditor.editor.items.gui.ItemsMenuGui
import org.tainorazum.universum.lobby.database.DB_Lobby
import org.tainorazum.universum.utility.other.open
import org.tainorazum.universum.utility.player.PlayerIsolationUtility
import org.tainorazum.universum.utility.player.PlayerUtility
import org.tainorazum.universum.utility.player.PlayerUtility.msg
import org.tainorazum.universum.utility.player.PlayerUtility.sound
import org.tainorazum.universum.utility.text.Colours

class KitEditor(
    val player: Player,
    val kit: Kit,
    val settings: GameSettings,
    val canCustomize: Boolean = true,
    val itemAccess: Boolean = false,
    val isDefault: Boolean = false
) {

    init {
        enter()
    }

    fun enter() {
        Universum.kitEditorManager.addEditor(player.uniqueId)

        DB_KitEditor.getSpawnpointLocation().thenAccept { loc ->
            val world = loc.world ?: return@thenAccept

            world.getChunkAtAsync(loc).thenAccept { chunk ->
                Bukkit.getScheduler().runTask(Universum.instance, Runnable {
                    if (!player.isOnline) return@Runnable

                    chunk.addPluginChunkTicket(Universum.instance)

                    player.teleportAsync(loc).thenAccept { success ->
                        if (!success || !player.isOnline) {
                            chunk.removePluginChunkTicket(Universum.instance)
                            return@thenAccept
                        }

                        PlayerUtility.reset(player, GameMode.SURVIVAL)
                        kit.applyToPlayer(player)

                        Bukkit.getScheduler().runTaskLater(Universum.instance, Runnable {
                            if (player.isOnline) {
                                PlayerIsolationUtility.isolatePlayer(player)
                            }
                            chunk.removePluginChunkTicket(Universum.instance)
                        }, 2L)
                    }
                })
            }
        }
    }

    fun leave() {
        DB_Lobby.getSpawnpointLocation().thenAccept { loc ->
            val world = loc.world ?: return@thenAccept

            world.getChunkAtAsync(loc).thenAccept { chunk ->
                Bukkit.getScheduler().runTask(Universum.instance, Runnable {
                    if (!player.isOnline) return@Runnable

                    chunk.addPluginChunkTicket(Universum.instance)

                    player.teleportAsync(loc).thenAccept { success ->
                        if (!success || !player.isOnline) {
                            chunk.removePluginChunkTicket(Universum.instance)
                            return@thenAccept
                        }

                        PlayerUtility.reset(player, GameMode.SURVIVAL)
                        PlayerIsolationUtility.unisolatePlayer(player)
                        Universum.kitEditorManager.removeEditor(player.uniqueId)

                        clear()

                        Bukkit.getScheduler().runTaskLater(Universum.instance, Runnable {
                            chunk.removePluginChunkTicket(Universum.instance)
                        }, 2L)
                    }
                })
            }

            ItemsGui.clear(player)
        }
    }

    fun clear() = Universum.kitEditorManager.clearKitEditor(this)

    fun cancel(quit: Boolean = false) {
        if (!quit) {
            leave()
        } else {
            PlayerIsolationUtility.unisolatePlayer(player)
            Universum.kitEditorManager.removeEditor(player.uniqueId)

            ItemsGui.clear(player)
        }
        clear()
    }

    fun save() {
        val ownerUUID = if (isDefault) null else player.uniqueId

        DB_Kits.saveKitFromPlayer(kit.id, player, ownerUUID).thenAccept { success ->
            Bukkit.getScheduler().runTask(Universum.instance, Runnable {
                if (player.isOnline) {
                    if (success) {
                        player.sendMessage(Component.text("Your kit has been saved!", Colours.GREEN))
                    } else {
                        player.sendMessage(Component.text("Error! Unable to save your kit. Please contact a staff member.", Colours.RED))
                    }
                }
                leave()
            })
        }
    }

    fun items() {
        if(itemAccess) {
            ItemsMenuGui.gui(player).open(player)
            player.sound(Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f)
        } else {
            player.msg("You are unable to access items in this kit.", Colours.RED)
        }
    }

}