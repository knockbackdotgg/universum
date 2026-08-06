package org.tainorazum.universum.command.universum.commands

import net.kyori.adventure.text.Component
import org.tainorazum.universum.Universum
import org.tainorazum.universum.command.universum.UniversumCommandSub
import org.tainorazum.universum.security.Permission
import org.tainorazum.universum.utility.player.PlayerUtility
import org.tainorazum.universum.utility.text.Colours
import org.tainorazum.universum.utility.text.Prefix
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Single
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.bukkit.actor.BukkitCommandActor

@Command("universum", "uni", "u")
class UniversumCommandServerList : UniversumCommandGeneral(UniversumCommandSub.SERVERLIST) {

    override val tutorial: List<Pair<String, String>> = mutableListOf(
        Pair("/universum serverList", "Receive this help message."),
        Pair("/universum serverList current set <identifier>", "Set the active server visual configuration identifier."),
        Pair("/universum serverList motd set <identifier> <motd>", "Set the MOTD for an identifier. (Copy motd from RGBirdflop)"),
        Pair("/universum serverList favicon set <identifier> <url>", "Set the favicon for an identifier. (64x64, .PNG)")
    )

    // Set active server configuration identifier
    @Subcommand("serverList current set")
    fun setCurrent(
        actor: BukkitCommandActor,
        identifier: String
    ) = PlayerUtility.guard(actor, Permission.CMD_UNIVERSUM_SERVERLIST) {

        actor.reply(Prefix.UNIVERSUM().append(Prefix.ARROW3()).append(Component.text("Attempting to update current server visual identifier to: $identifier...", Colours.GREEN)))

        Universum.visualServerManager.setCurrentIdentifier(identifier).thenAccept { success ->
            if (success) {
                actor.reply(Prefix.UNIVERSUM().append(Prefix.ARROW3()).append(Component.text("Successfully updated current visual identifier to: $identifier.", Colours.GREEN)))
            } else {
                actor.reply(Prefix.UNIVERSUM().append(Prefix.ARROW3()).append(Component.text("Unable to update current visual identifier to: $identifier. See console for details.", Colours.RED)))
            }
        }.exceptionally { ex ->
            actor.reply(Prefix.UNIVERSUM().append(Prefix.ARROW3()).append(Component.text("An error occurred while updating active identifier: ${ex.message}", Colours.RED)))
            null
        }
    }

    // Set MOTD for a specific identifier
    @Subcommand("serverList motd set")
    fun setMotd(
        actor: BukkitCommandActor,
        identifier: String,
        motd: String
    ) = PlayerUtility.guard(actor, Permission.CMD_UNIVERSUM_SERVERLIST) {

        actor.reply(Prefix.UNIVERSUM().append(Prefix.ARROW3()).append(Component.text("Attempting to update MOTD for identifier: $identifier...", Colours.GREEN)))

        Universum.visualServerManager.setMOTD(identifier, motd).thenAccept { success ->
            if (success) {
                actor.reply(Prefix.UNIVERSUM().append(Prefix.ARROW3()).append(Component.text("Successfully updated MOTD for identifier: $identifier.", Colours.GREEN)))
            } else {
                actor.reply(Prefix.UNIVERSUM().append(Prefix.ARROW3()).append(Component.text("Unable to update MOTD for identifier: $identifier. See console for details.", Colours.RED)))
            }
        }.exceptionally { ex ->
            actor.reply(Prefix.UNIVERSUM().append(Prefix.ARROW3()).append(Component.text("An error occurred while setting MOTD: ${ex.message}", Colours.RED)))
            null
        }
    }

    // Set favicon from URL for a specific identifier
    @Subcommand("serverList favicon set")
    fun setFavicon(
        actor: BukkitCommandActor,
        identifier: String,
        url: String
    ) = PlayerUtility.guard(actor, Permission.CMD_UNIVERSUM_SERVERLIST) {

        actor.reply(Prefix.UNIVERSUM().append(Prefix.ARROW3()).append(Component.text("Downloading and validating favicon from URL for identifier: $identifier...", Colours.GREEN)))

        Universum.visualServerManager.setFavicon(identifier, url).thenAccept { success ->
            if (success) {
                actor.reply(Prefix.UNIVERSUM().append(Prefix.ARROW3()).append(Component.text("Successfully updated favicon for identifier: $identifier.", Colours.GREEN)))
            } else {
                actor.reply(Prefix.UNIVERSUM().append(Prefix.ARROW3()).append(Component.text("Unable to update favicon for identifier: $identifier. See console for details.", Colours.RED)))
            }
        }.exceptionally { ex ->
            val cause = ex.cause?.message ?: ex.message ?: "Unknown error"
            actor.reply(Prefix.UNIVERSUM().append(Prefix.ARROW3()).append(Component.text("Failed to set favicon: $cause", Colours.RED)))
            null
        }
    }
}