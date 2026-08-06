package org.tainorazum.universum.command.universum.commands

import net.kyori.adventure.text.Component
import org.tainorazum.universum.command.universum.UniversumCommandSub
import org.tainorazum.universum.security.Permission
import org.tainorazum.universum.utility.player.PlayerUtility
import org.tainorazum.universum.utility.text.Colours
import org.tainorazum.universum.utility.text.Prefix
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.bukkit.actor.BukkitCommandActor

@Command("universum", "uni", "u")
class UniversumCommandTest : UniversumCommandGeneral(UniversumCommandSub.TEST) {

    override val tutorial : List<Pair<String, String>> = mutableListOf(
        Pair("/universum test", " Receive this help message."),
        Pair("/universum test blackout <duration> <fadeOut> <title> <subTitle>", " Give yourself a temporary screen blackout (feature test)."),
        Pair("/universum test statistic <", "Set your statistics in order to test.")
    )

    @Subcommand("test blackout")
    fun blackout(
        actor: BukkitCommandActor,
        duration: Int,
        fadeOut: Int,
        titleRaw: String,
        subTitleRaw: String,
    ) = PlayerUtility.guard(actor, Permission.CMD_UNIVERSUM_TEST) {

        val player = actor.requirePlayer()
        player.sendMessage(Component.newline())
        player.sendMessage(Prefix.UNIVERSUM().append(Prefix.ARROW3()).append(Component.text("Attempting screen blackout...", Colours.GREEN)))
        player.sendMessage(Prefix.UNIVERSUM().append(Prefix.ARROW3()).append(Component.text("Duration: $duration ms", Colours.GREEN)))
        player.sendMessage(Prefix.UNIVERSUM().append(Prefix.ARROW3()).append(Component.text("Fade out: $fadeOut ms", Colours.GREEN)))
        player.sendMessage(Prefix.UNIVERSUM().append(Prefix.ARROW3()).append(Component.text("Title text: $titleRaw", Colours.GREEN)))
        player.sendMessage(Prefix.UNIVERSUM().append(Prefix.ARROW3()).append(Component.text("Subtitle text: $subTitleRaw", Colours.GREEN)))
        player.sendMessage(Component.newline())

        val title = Component.text(titleRaw, Colours.GREEN)
        val subTitle = Component.text(subTitleRaw, Colours.GREY)
        PlayerUtility.blackout(player, duration, fadeOut, title, subTitle)

        player.sendMessage(Prefix.UNIVERSUM().append(Prefix.ARROW3()).append(Component.text("Screen blackout sent.", Colours.GREEN)))

    }

}