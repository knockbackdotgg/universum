package org.tainorazum.universum.command.default

import org.tainorazum.universum.security.Permission
import org.tainorazum.universum.setting.manager.gui.PlayerSettingGui
import org.tainorazum.universum.utility.player.PlayerUtility
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.CommandPlaceholder
import revxrsal.commands.bukkit.actor.BukkitCommandActor

@Command("settings", "setting", "set")
class SettingsCommand {

    @CommandPlaceholder
    fun settings(
        actor: BukkitCommandActor
    ) = PlayerUtility.guard(actor, Permission.CMD_DEFAULT_SETTINGS, true) {

        val player = actor.requirePlayer()
        PlayerSettingGui.openGUI(player)

    }

}