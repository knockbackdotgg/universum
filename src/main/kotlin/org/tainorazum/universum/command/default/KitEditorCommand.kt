package org.tainorazum.universum.command.default

import net.kyori.adventure.text.Component
import org.tainorazum.universum.Universum
import org.tainorazum.universum.kiteditor.gui.KitEditorGui
import org.tainorazum.universum.security.Permission
import org.tainorazum.universum.utility.player.PlayerUtility
import org.tainorazum.universum.utility.text.Colours
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.CommandPlaceholder
import revxrsal.commands.annotation.Subcommand
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

    @Subcommand("save")
    fun save(
        actor: BukkitCommandActor
    ) = PlayerUtility.guard(actor, Permission.CMD_DEFAULT_KITEDITOR) {

        val player = actor.requirePlayer()
        val kitEditor = Universum.kitEditorManager.getKitEditor(player)

        if(kitEditor != null) {
            kitEditor.save()
        } else {
            player.sendMessage(Component.text("You are unable to do that right now.", Colours.RED))
        }

    }

    @Subcommand("cancel")
    fun cancel(
        actor: BukkitCommandActor
    ) = PlayerUtility.guard(actor, Permission.CMD_DEFAULT_KITEDITOR) {

        val player = actor.requirePlayer()
        val kitEditor = Universum.kitEditorManager.getKitEditor(player)

        if(kitEditor != null) {
            kitEditor.cancel()
        } else {
            player.sendMessage(Component.text("You are unable to do that right now.", Colours.RED))
        }

    }

}