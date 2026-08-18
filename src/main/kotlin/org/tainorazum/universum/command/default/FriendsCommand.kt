package org.tainorazum.universum.command.default

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.tainorazum.universum.Universum
import org.tainorazum.universum.friends.database.DB_Friends
import org.tainorazum.universum.network.manager.NetworkPlayerManager
import org.tainorazum.universum.security.Permission
import org.tainorazum.universum.utility.player.PlayerUtility
import org.tainorazum.universum.utility.player.PlayerUtility.noSound
import org.tainorazum.universum.utility.text.Colours
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.CommandPlaceholder
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.bukkit.actor.BukkitCommandActor

@Command("friends", "friend", "f")
class FriendsCommand {

    @CommandPlaceholder
    fun friends(
        actor: BukkitCommandActor
    ) = PlayerUtility.guard(actor, Permission.CMD_DEFAULT_FRIENDS) {
        val player = actor.requirePlayer()

        player.sendMessage(
            Component.text("Friend commands:", Colours.ORANGE)
                .appendNewline()
                .append(Component.text("/friends list (See all your friends.)", Colours.GREY))
                .appendNewline()
                .append(Component.text("/friends request|add <name> (Friend request a player.)", Colours.GREY))
                .appendNewline()
                .append(Component.text("/friends remove <name> (Friend remove a player.)", Colours.GREY))
                .appendNewline()
                .append(Component.text("/friends accept <name> (Accept a friend request.)", Colours.GREY))
        )
    }

    @Subcommand("list")
    fun list(
        actor: BukkitCommandActor
    ) = PlayerUtility.guard(actor, Permission.CMD_DEFAULT_FRIENDS) {
        val player = actor.requirePlayer()

        DB_Friends.getFriends(player.uniqueId).thenAccept { friendUUIDs ->
            if (friendUUIDs.isEmpty()) {
                player.sendMessage(Component.text("You don't have any friends added yet.", Colours.GREY))
                return@thenAccept
            }

            val friendNames = friendUUIDs.map { uuid ->
                val onlineName = Bukkit.getPlayer(uuid)?.name
                val status = if (NetworkPlayerManager.isNetworkOnline(uuid)) "&a[Online]" else "&c[Offline]"
                "&7- ${onlineName ?: uuid.toString().take(8)} $status"
            }

            val header = Component.text("Your Friends (${friendUUIDs.size}):", Colours.ORANGE)
            val listMessage = friendNames.joinToString("\n")

            player.sendMessage(header.appendNewline().append(Component.text(listMessage)))
        }
    }

    @Subcommand("request", "add")
    fun request(
        actor: BukkitCommandActor,
        targetName: String
    ) = PlayerUtility.guard(actor, Permission.CMD_DEFAULT_FRIENDS) {
        val player = actor.requirePlayer()
        Universum.friendsManager.attemptRequestFriend(player, targetName)
    }

    @Subcommand("remove")
    fun remove(
        actor: BukkitCommandActor,
        targetName: String
    ) = PlayerUtility.guard(actor, Permission.CMD_DEFAULT_FRIENDS) {
        val player = actor.requirePlayer()
        Universum.friendsManager.removeFriend(player, targetName)
    }

    @Subcommand("accept")
    fun accept(
        actor: BukkitCommandActor,
        targetName: String
    ) = PlayerUtility.guard(actor, Permission.CMD_DEFAULT_FRIENDS) {
        val player = actor.requirePlayer()
        Universum.friendsManager.acceptFriend(player, targetName)
    }
}