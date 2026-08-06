package org.tainorazum.universum.command.admin.commands

import net.kyori.adventure.text.Component
import org.tainorazum.universum.Universum
import org.tainorazum.universum.command.admin.AdminCommandSub
import org.tainorazum.universum.security.Permission
import org.tainorazum.universum.utility.other.parseDuration
import org.tainorazum.universum.utility.other.toHumanReadable
import org.tainorazum.universum.utility.permissions.LuckpermsUtil
import org.tainorazum.universum.utility.player.PlayerUtility
import org.tainorazum.universum.utility.text.Colours
import org.tainorazum.universum.utility.text.Prefix
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.annotation.Suggest
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import java.time.Duration
import java.util.concurrent.CompletableFuture

@Command("administrator", "admin", "a")
class AdminCommandWhitelist : AdminCommandGeneral(AdminCommandSub.WHITELIST) {

    override val tutorial : List<Pair<String, String>> = mutableListOf(
        Pair("/admin whitelist", "Receive this help message."),
        Pair("/admin whitelist on", "Turn on the whitelist."),
        Pair("/admin whitelist off", "Turn off the whitelist."),
        Pair("/admin whitelist toggle", "Toggle the whitelist."),
        Pair("/admin whitelist status", "See whether or not the whitelist is enabled."),
        Pair("/admin whitelist add <name> <duration>", " Add a player to the whitelist."),
        Pair("/admin whitelist remove <name>", " Remove a player from the whitelist."),
        Pair("/admin whitelist list", " See all whitelisted players.")
    )

    @Subcommand("whitelist toggle")
    fun toggleWhitelist(
        actor: BukkitCommandActor
    ) = PlayerUtility.guard(actor, Permission.CMD_ADMIN_WHITELIST) {

        val player = actor.requirePlayer()

        Universum.server.public.thenAccept { isWhitelisted ->
            Universum.whitelistManager.setWhitelisted(!isWhitelisted).thenAccept { success ->
                if(success) {
                    player.sendMessage(Prefix.ADMIN()
                        .append(Prefix.ARROW3())
                        .append(Component.text("This server's whitelist has been toggled.", Colours.GREEN)))
                    whitelisted(actor)
                } else {
                    player.sendMessage(Prefix.ADMIN()
                        .append(Prefix.ARROW3())
                        .append(Component.text("Unable to toggle the server's whitelist.", Colours.RED)))
                    whitelisted(actor)
                }
            }
        }
    }

    @Subcommand("whitelist off")
    fun disableWhitelist(
        actor: BukkitCommandActor
    ) = PlayerUtility.guard(actor, Permission.CMD_ADMIN_WHITELIST) {

        val player = actor.requirePlayer()

        Universum.whitelistManager.setWhitelisted(false).thenAccept { success ->
            if(success) {
                player.sendMessage(Prefix.ADMIN()
                    .append(Prefix.ARROW3())
                    .append(Component.text("This server's whitelist has been disabled.", Colours.GREEN)))
            } else {
                player.sendMessage(Prefix.ADMIN()
                    .append(Prefix.ARROW3())
                    .append(Component.text("Unable to disable the server's whitelist.", Colours.RED)))
            }
        }

    }

    @Subcommand("whitelist on")
    fun enableWhitelist(
        actor: BukkitCommandActor
    ) = PlayerUtility.guard(actor, Permission.CMD_ADMIN_WHITELIST) {

        val player = actor.requirePlayer()

        Universum.whitelistManager.setWhitelisted(true).thenAccept { success ->
            if(success) {
                player.sendMessage(Prefix.ADMIN()
                    .append(Prefix.ARROW3())
                    .append(Component.text("This server's whitelist has been enabled.", Colours.GREEN)))
            } else {
                player.sendMessage(Prefix.ADMIN()
                    .append(Prefix.ARROW3())
                    .append(Component.text("Unable to enable the server's whitelist.", Colours.RED)))
            }
        }

    }

    @Subcommand("whitelist status")
    fun whitelisted(
        actor: BukkitCommandActor
    ) = PlayerUtility.guard(actor, Permission.CMD_ADMIN_WHITELIST) {

        val player = actor.requirePlayer()

        Universum.whitelistManager.whitelisted().thenAccept { isWhitelisted ->
            player.sendMessage(Prefix.ADMIN()
                .append(Prefix.ARROW3())
                .append(Component.text("This server's whitelist is currently set to: ${isWhitelisted.toString().uppercase()}.", Colours.GREEN)))
        }

    }

    @Subcommand("whitelist list")
    fun whitelistList(
        actor: BukkitCommandActor
    ) = PlayerUtility.guard(actor, Permission.CMD_ADMIN_WHITELIST) {

        val player = actor.requirePlayer()

        Universum.whitelistManager.getWhitelisted().thenAccept { whitelisted ->
            player.sendMessage(Prefix.ADMIN()
                .append(Prefix.ARROW3())
                .append(Component.text("All whitelisted players: (${whitelisted.size})", Colours.GREEN)))
            for(uuid in whitelisted) {
                val nameFuture = PlayerUtility.getName(uuid)
                val durationFuture = LuckpermsUtil.getExpiry(uuid, Universum.whitelistManager.perm)
                CompletableFuture.allOf(nameFuture, durationFuture).thenAccept {
                    val name : String? = nameFuture.join()
                    val duration : Duration? = durationFuture.join()

                    if(name != null) {
                        player.sendMessage(Component.text("- ", Colours.GREEN)
                            .append(Component.text("$name ${duration?.let { " (${duration.toHumanReadable()})" } ?: "(FOREVER)"}", Colours.GREY)))
                    }

                }
            }
        }

    }

    @Subcommand("whitelist remove")
    fun whitelistRemove(
        actor: BukkitCommandActor,
        targetName: String
    ) = PlayerUtility.guard(actor, Permission.CMD_ADMIN_WHITELIST){

        val player = actor.requirePlayer()

        PlayerUtility.getUUID(targetName).thenAccept { uuid ->
            if (uuid == null) {
                player.sendMessage(
                    Prefix.ADMIN()
                        .append(Prefix.ARROW3())
                        .append(Component.text("That player does not exist.", Colours.RED))
                )
                return@thenAccept
            }

            Universum.whitelistManager.unWhitelist(uuid).thenAccept { success ->
                if(success) {
                    player.sendMessage(Prefix.ADMIN()
                        .append(Prefix.ARROW3())
                        .append(Component.text("Player $targetName has been removed from the whitelist.", Colours.GREEN)))
                } else {
                    player.sendMessage(Prefix.ADMIN()
                        .append(Prefix.ARROW3())
                        .append(Component.text("Player $targetName is not whitelisted.", Colours.RED)))
                }
            }
        }

    }

    @Subcommand("whitelist add")
    fun whitelistAdd(
        actor: BukkitCommandActor,
        targetName: String,
        @Suggest("perm", "1d", "1h", "10m", "10s") durationString: String ?= null
    ) = PlayerUtility.guard(actor, Permission.CMD_ADMIN_WHITELIST) {

        val player = actor.requirePlayer()

        PlayerUtility.getUUID(targetName).thenAccept { uuid ->
            if (uuid == null) {
                player.sendMessage(
                    Prefix.ADMIN()
                        .append(Prefix.ARROW3())
                        .append(Component.text("That player does not exist.", Colours.RED))
                )
                return@thenAccept
            }

            val duration = parseDuration(durationString ?: "perm")

            Universum.whitelistManager.isWhitelisted(uuid).thenAccept { isWhitelisted ->
                if(isWhitelisted) {
                    LuckpermsUtil.getExpiry(uuid, Universum.whitelistManager.perm).thenAccept { remaining ->
                        player.sendMessage(
                            Prefix.ADMIN()
                                .append(Prefix.ARROW3())
                                .append(
                                    Component.text(
                                        "Player $targetName is already whitelisted${remaining?.let { ". Remaining: ${it.toHumanReadable()}" } ?: ""}.",
                                        Colours.GREEN
                                    )
                                )
                        )
                    }
                    return@thenAccept
                }
            }

            Universum.whitelistManager.whitelist(uuid, duration).thenAccept { success ->
                if (success) {
                    player.sendMessage(
                        Prefix.ADMIN()
                            .append(Prefix.ARROW3())
                            .append(
                                Component.text(
                                    "Player $targetName has been whitelisted${duration?.let { " for ${it.toHumanReadable()}" } ?: ""}.",
                                    Colours.GREEN
                                )
                            )
                    )
                } else {
                    LuckpermsUtil.getExpiry(uuid, Universum.whitelistManager.perm).thenAccept { remaining ->
                        player.sendMessage(
                            Prefix.ADMIN()
                                .append(Prefix.ARROW3())
                                .append(
                                    Component.text(
                                        "Player $targetName is already whitelisted${remaining?.let { ". Remaining: ${it.toHumanReadable()}" } ?: ""}.",
                                        Colours.GREEN
                                    )
                                )
                        )
                    }

                }
            }
        }
    }

}