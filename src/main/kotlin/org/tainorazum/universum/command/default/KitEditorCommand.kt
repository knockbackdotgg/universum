package org.tainorazum.universum.command.default

import org.tainorazum.universum.kiteditor.gui.KitEditorGui
import org.tainorazum.universum.security.Permission
import org.tainorazum.universum.utility.player.PlayerUtility
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.CommandPlaceholder
import revxrsal.commands.bukkit.actor.BukkitCommandActor

@Command("kiteditor", "kit", "kits")
class KitEditorCommand {

    @CommandPlaceholder
    fun kiteditor(
        actor: BukkitCommandActor
    ) = PlayerUtility.guard(actor, Permission.CMD_DEFAULT_KITEDITOR, true) {

        val player = actor.requirePlayer()
        KitEditorGui.openGUI(player)

    }

}