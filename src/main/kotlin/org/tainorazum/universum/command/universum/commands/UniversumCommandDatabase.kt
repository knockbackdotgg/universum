package org.tainorazum.universum.command.universum.commands

import net.kyori.adventure.text.Component
import org.tainorazum.universum.command.universum.UniversumCommandSub
import org.tainorazum.universum.security.Permission
import org.tainorazum.universum.utility.database.DatabaseUtil
import org.tainorazum.universum.utility.player.PlayerUtility
import org.tainorazum.universum.utility.text.Colours
import org.tainorazum.universum.utility.text.Prefix
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import java.util.UUID

@Command("universum", "uni", "u")
class UniversumCommandDatabase : UniversumCommandGeneral(UniversumCommandSub.DATABASE) {

    override val tutorial : List<Pair<String, String>> = mutableListOf(
        Pair("/universum whitelist", "Receive this help message."),
        Pair("/universum whitelist add <name>", " Add a player to the whitelist."),
        Pair("/universum whitelist remove <name>", " Remove a player from the whitelist."),
        Pair("/universum whitelist list", " See all whitelisted players.")
    )

    private val count = mutableMapOf<UUID, Int>()

    @OptIn(ExperimentalStdlibApi::class)
    @Subcommand("database delete")
    fun delete(
        actor: BukkitCommandActor,
        name: String
    ) = PlayerUtility.guard(actor, Permission.CMD_UNIVERSUM_DATABASE) {

        val player = actor.requirePlayer()
        if(count.getOrPutIfNull(player.uniqueId) { 0 } == 0) {
            player.sendMessage(Prefix.UNIVERSUM().append(Prefix.ARROW3()).append(Component.text("To delete the table: $name, execute the command again to confirm.", Colours.GREEN)))
            count[player.uniqueId] = 1
        } else if(count[player.uniqueId] == 1) {
            count.remove(player.uniqueId)
            player.sendMessage(Prefix.UNIVERSUM().append(Prefix.ARROW3()).append(Component.text("Attempting to delete table: $name.", Colours.GREEN)))

            DatabaseUtil.dropTableByName(name).thenAccept { success ->
                if(success) {
                    player.sendMessage(Prefix.UNIVERSUM().append(Prefix.ARROW3()).append(Component.text("Successfully deleted table: $name. Please restart the server so it can recreate the table if necessary.", Colours.GREEN)))
                } else {
                    player.sendMessage(Prefix.UNIVERSUM().append(Prefix.ARROW3()).append(Component.text("Unable to delete table: $name. See console for details.", Colours.RED)))

                }
            }

        }

    }

}