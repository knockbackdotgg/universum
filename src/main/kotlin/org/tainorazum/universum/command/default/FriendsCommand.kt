package org.tainorazum.universum.command.default

import net.kyori.adventure.text.Component
import org.tainorazum.universum.Universum
import org.tainorazum.universum.friends.database.DB_Friends
import org.tainorazum.universum.network.manager.NetworkPlayerManager
import org.tainorazum.universum.security.Permission
import org.tainorazum.universum.utility.player.PlayerUtility
import org.tainorazum.universum.utility.text.Colours
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.CommandPlaceholder
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import java.util.concurrent.CompletableFuture

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Friends

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class NetworkPlayers

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
                .append(Component.text("/friends request <name> (Friend request a player.)", Colours.GREY))
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

            val lineFutures = friendUUIDs.map { uuid ->
                PlayerUtility.getName(uuid).thenApply { fetchedName ->
                    val name = fetchedName ?: "N/A (Fmk1) Please contact an admin."
                    val isOnline = NetworkPlayerManager.isNetworkOnline(uuid)

                    val statusComponent = if (isOnline) {
                        Component.text("[Online]", Colours.GREEN)
                    } else {
                        Component.text("[Offline]", Colours.RED)
                    }

                    Component.text("- ", Colours.GREY)
                        .append(Component.text(name, Colours.GREY))
                        .appendSpace()
                        .append(statusComponent)
                }
            }

            CompletableFuture.allOf(*lineFutures.toTypedArray()).thenAccept {
                val friendLines = lineFutures.map { it.join() }

                var fullMessage = Component.text("Your Friends (${friendUUIDs.size}):", Colours.ORANGE)
                friendLines.forEach { line ->
                    fullMessage = fullMessage.appendNewline().append(line)
                }

                player.sendMessage(fullMessage)
            }
        }
    }

    @Subcommand("request")
    fun request(
        actor: BukkitCommandActor,
        @NetworkPlayers targetName: String
    ) = PlayerUtility.guard(actor, Permission.CMD_DEFAULT_FRIENDS) {
        val player = actor.requirePlayer()
        Universum.friendsManager.attemptRequestFriend(player, targetName)
    }

    @Subcommand("remove")
    fun remove(
        actor: BukkitCommandActor,
        @Friends targetName: String
    ) = PlayerUtility.guard(actor, Permission.CMD_DEFAULT_FRIENDS) {
        val player = actor.requirePlayer()
        Universum.friendsManager.removeFriend(player, targetName)
    }

    @Subcommand("accept")
    fun accept(
        actor: BukkitCommandActor,
        @NetworkPlayers targetName: String
    ) = PlayerUtility.guard(actor, Permission.CMD_DEFAULT_FRIENDS) {
        val player = actor.requirePlayer()
        Universum.friendsManager.acceptFriend(player, targetName)
    }

}