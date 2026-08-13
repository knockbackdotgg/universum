package org.tainorazum.universum.command.admin.commands

import net.kyori.adventure.text.Component
import org.tainorazum.universum.command.admin.AdminCommandSub
import org.tainorazum.universum.engine.kit.database.DB_Kits
import org.tainorazum.universum.kiteditor.gui.DefaultKitGui
import org.tainorazum.universum.security.Permission
import org.tainorazum.universum.utility.player.PlayerUtility
import org.tainorazum.universum.utility.text.Colours
import org.tainorazum.universum.utility.text.Prefix
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.bukkit.actor.BukkitCommandActor

@Command("administrator", "admin", "a")
class AdminCommandKits : AdminCommandGeneral(AdminCommandSub.PUNISHMENTS) {

    override val tutorial : List<Pair<String, String>> = mutableListOf(
        Pair("/admin kits", "Opens the kit management gui."),
        Pair("/admin kits create <identifier>", "Create a kit with its identifier.")
    )

    @Subcommand("kits")
    fun kits(
        actor: BukkitCommandActor,
    ) = PlayerUtility.guard(actor, Permission.CMD_ADMIN_KITS) {

        val player = actor.requirePlayer()
        DefaultKitGui.openGUI(player)

    }

    @Subcommand("kits create")
    fun create(
        actor: BukkitCommandActor,
        identifier: String
    ) = PlayerUtility.guard(actor, Permission.CMD_ADMIN_KITS) {

        val player = actor.requirePlayer()
        DB_Kits.createKit(identifier).thenAccept { success ->
            if(success) {
                player.sendMessage(Prefix.ADMIN()
                    .append(Prefix.ARROW3())
                    .append(Component.text("Kit '$identifier' has been successfully made. You may now modify it.", Colours.GREEN)))
            } else {
                player.sendMessage(Prefix.ADMIN()
                    .append(Prefix.ARROW3())
                    .append(Component.text("Failed to make kit. Either it already exists or there is an error. Look in console for details.", Colours.RED)))
            }
        }

    }

}