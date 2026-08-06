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
        Pair("/universum database", "Receive this help message."),
        Pair("/universum database delete <name>", "Delete a database's table by name."),
    )

    private val count = mutableMapOf<UUID, Int>()

    @OptIn(ExperimentalStdlibApi::class)
    @Subcommand("database delete")
    fun delete(
        actor: BukkitCommandActor,
        name: String
    ) = PlayerUtility.guard(actor, Permission.CMD_UNIVERSUM_DATABASE) {

        if(count.getOrPutIfNull(actor.uniqueId()) { 0 } == 0) {
            actor.reply(Prefix.UNIVERSUM().append(Prefix.ARROW3()).append(Component.text("To delete the table: $name, execute the command again to confirm.", Colours.GREEN)))
            count[actor.uniqueId()] = 1
        } else if(count[actor.uniqueId()] == 1) {
            count.remove(actor.uniqueId())
            actor.reply(Prefix.UNIVERSUM().append(Prefix.ARROW3()).append(Component.text("Attempting to delete table: $name.", Colours.GREEN)))

            DatabaseUtil.dropTableByName(name).thenAccept { success ->
                if(success) {
                    actor.reply(Prefix.UNIVERSUM().append(Prefix.ARROW3()).append(Component.text("Successfully deleted table: $name. Please restart the server so it can recreate the table if necessary.", Colours.GREEN)))
                } else {
                    actor.reply(Prefix.UNIVERSUM().append(Prefix.ARROW3()).append(Component.text("Unable to delete table: $name. See console for details.", Colours.RED)))
                }
            }

        }

    }

}