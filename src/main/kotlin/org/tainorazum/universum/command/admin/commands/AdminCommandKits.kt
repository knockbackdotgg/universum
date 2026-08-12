package org.tainorazum.universum.command.admin.commands

import org.tainorazum.universum.command.admin.AdminCommandSub
import org.tainorazum.universum.kiteditor.gui.DefaultKitGui
import org.tainorazum.universum.security.Permission
import org.tainorazum.universum.utility.player.PlayerUtility
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.bukkit.actor.BukkitCommandActor

@Command("administrator", "admin", "a")
class AdminCommandKits : AdminCommandGeneral(AdminCommandSub.PUNISHMENTS) {

    override val tutorial : List<Pair<String, String>> = mutableListOf(
        Pair("/admin kits", "Opens the kit management gui.")
    )

    @Subcommand("kits")
    fun kits(
        actor: BukkitCommandActor,
    ) = PlayerUtility.guard(actor, Permission.CMD_ADMIN_KITS, true) {

        val player = actor.requirePlayer()
        DefaultKitGui.openGUI(player)

    }

}