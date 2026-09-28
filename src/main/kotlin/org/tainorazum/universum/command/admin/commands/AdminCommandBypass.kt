package org.tainorazum.universum.command.admin.commands

import net.kyori.adventure.text.Component
import org.tainorazum.universum.Universum
import org.tainorazum.universum.command.admin.AdminCommandSub
import org.tainorazum.universum.security.Permission
import org.tainorazum.universum.utility.player.PlayerUtility
import org.tainorazum.universum.utility.text.Colours
import org.tainorazum.universum.utility.text.Prefix
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Optional
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.annotation.Suggest
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import java.util.UUID
import java.util.concurrent.CompletableFuture

@Command("administrator", "admin", "a")
class AdminCommandBypass : AdminCommandGeneral(AdminCommandSub.BYPASS) {

    override val tutorial : List<Pair<String, String>> = mutableListOf(
        Pair("/admin bypass", "Receive this help message."),
        Pair("/admin bypass <bypass> true|false", "Enable or disable a bypass for you."),
        Pair("/admin bypass <bypass> true|false <name>", "Enable or disable a bypass for another player."),
    )

    @Subcommand("bypass")
    fun bypass(
        actor: BukkitCommandActor,
        @Suggest("lobby", "chatfilter") bypass: String,
        bool: Boolean,
        @Optional name: String?
    ) = PlayerUtility.guard(actor, Permission.CMD_ADMIN_BYPASS) {

        val player = actor.requirePlayer()

        val targetUuidFuture: CompletableFuture<UUID?> = if (name != null) {
            PlayerUtility.getUUID(name)
        } else {
            CompletableFuture.completedFuture(player.uniqueId)
        }

        targetUuidFuture.thenAccept { uuid ->
            if (uuid == null) {
                player.sendMessage(Prefix.ADMIN()
                    .append(Prefix.ARROW3())
                    .append(Component.text("Player $name not found.", Colours.RED)))
                return@thenAccept
            }

            when (bypass.lowercase()) {
                "lobby" -> {
                    Universum.playerManager.allowBypassLobby(uuid, bool).thenAccept { success ->
                        if (success) {
                            val targetName = name ?: player.name
                            val state = if (bool) "enabled" else "disabled"
                            player.sendMessage(Prefix.ADMIN()
                                .append(Prefix.ARROW3())
                                .append(Component.text("Lobby bypass $state for $targetName", Colours.GREEN)))
                        } else {
                            player.sendMessage(Prefix.ADMIN()
                                .append(Prefix.ARROW3())
                                .append(Component.text("Your bypass is already set to this value.", Colours.GREEN)))
                        }
                    }
                }
                "chatfilter" -> {
                    Universum.playerManager.allowBypassChatFilter(uuid, bool).thenAccept { success ->
                        if (success) {
                            val targetName = name ?: player.name
                            val state = if (bool) "enabled" else "disabled"
                            player.sendMessage(Prefix.ADMIN()
                                .append(Prefix.ARROW3())
                                .append(Component.text("Chat filter bypass $state for $targetName", Colours.GREEN)))
                        } else {
                            player.sendMessage(Prefix.ADMIN()
                                .append(Prefix.ARROW3())
                                .append(Component.text("Your bypass is already set to this value.", Colours.GREEN)))
                        }
                    }
                }
                else -> player.sendMessage(Prefix.ADMIN()
                    .append(Prefix.ARROW3())
                    .append(Component.text("Unknown bypass option: $bypass", Colours.GREEN)))
            }
        }
    }

}