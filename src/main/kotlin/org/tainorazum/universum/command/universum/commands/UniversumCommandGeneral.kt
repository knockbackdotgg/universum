package org.tainorazum.universum.command.universum.commands

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.HoverEvent
import org.tainorazum.universum.command.universum.UniversumCommandSub
import org.tainorazum.universum.security.Permission
import org.tainorazum.universum.utility.player.PlayerUtility
import org.tainorazum.universum.utility.text.Colours
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Optional
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.bukkit.actor.BukkitCommandActor

@Command("universum", "uni", "u")
open class UniversumCommandGeneral(
    sub: UniversumCommandSub = UniversumCommandSub.GENERAL
) {

    open val tutorial : List<Pair<String, String>> = mutableListOf(
        Pair("/universum help <subject>", "Receive help on a specific command."),
    )

    init {
        sub.register(this)
    }

    @Subcommand("help")
    open fun help(
        actor: BukkitCommandActor,
        @Optional subArg: UniversumCommandSub?
    ) = PlayerUtility.guard(actor, Permission.CMD_UNIVERSUM_GENERAL) {

        val tutorialList = subArg?.getCommand()?.tutorial ?: tutorial
        val subjectName = subArg?.name ?: "GENERAL"

        var component = Component.empty()
            .append(Component.text("Universum command help ($subjectName): (Hover for details)", Colours.GREEN))
            .append(Component.newline())

        for (line in tutorialList) {
            val lineComponent = Component.text("- ", Colours.GREEN)
                .append(
                    Component.text(line.first, Colours.GREY)
                        .hoverEvent(HoverEvent.showText(Component.text(line.second, Colours.GREEN)))
                )

            component = component
                .append(lineComponent)
                .append(Component.newline())
        }

        actor.reply(component)
    }

}
