package org.tainorazum.universum.command.admin.commands

import net.kyori.adventure.text.Component
import org.tainorazum.universum.Universum
import org.tainorazum.universum.command.admin.AdminCommandSub
import org.tainorazum.universum.security.Permission
import org.tainorazum.universum.utility.player.PlayerUtility
import org.tainorazum.universum.utility.player.PlayerUtility.noSound
import org.tainorazum.universum.utility.text.Colours
import org.tainorazum.universum.utility.text.Prefix
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.bukkit.actor.BukkitCommandActor

@Command("administrator", "admin", "a")
class AdminCommandCoins : AdminCommandGeneral(AdminCommandSub.COINS) {

    override val tutorial: List<Pair<String, String>> = mutableListOf(
        Pair("/admin coins get <name>", "Get a player's coins."),
        Pair("/admin coins set <name> <amount>", "Set a player's coins."),
        Pair("/admin coins add <name> <amount>", "Give a player coins.")
    )

    @Subcommand("coins add")
    fun add(
        actor: BukkitCommandActor,
        name: String,
        amount: Int,
    ) = PlayerUtility.guard(actor, Permission.CMD_ADMIN_COINS) {

        if(amount < 0) {
            actor.reply(
                Prefix.ADMIN()
                    .append(Prefix.ARROW3())
                    .append(Component.text("You must provide a value higher than 0.", Colours.RED))
            )
            return@guard
        }

        PlayerUtility.getUUID(name).thenAccept { uuid ->
            if(uuid != null) {
                Universum.coinsManager.addCoins(uuid, amount).thenAccept { newAmount ->
                    actor.reply(
                        Prefix.ADMIN()
                            .append(Prefix.ARROW3())
                            .append(Component.text("$name now has $newAmount coins.", Colours.GREEN))
                    )
                }
            } else {
                actor.reply(
                    Prefix.ADMIN()
                        .append(Prefix.ARROW3())
                        .append(Component.text("That player does not exist.", Colours.RED))
                )
            }
        }

    }

    @Subcommand("coins set")
    fun set(
        actor: BukkitCommandActor,
        name: String,
        amount: Int,
    ) = PlayerUtility.guard(actor, Permission.CMD_ADMIN_COINS) {

        if(amount < 0) {
            actor.reply(
                Prefix.ADMIN()
                    .append(Prefix.ARROW3())
                    .append(Component.text("You must provide a value higher than 0.", Colours.RED))
            )
            return@guard
        }

        PlayerUtility.getUUID(name).thenAccept { uuid ->
            if(uuid != null) {
                Universum.coinsManager.setCoins(uuid, amount).thenAccept { success ->
                    if(success) {
                        actor.reply(
                            Prefix.ADMIN()
                                .append(Prefix.ARROW3())
                                .append(Component.text("$name now has $amount coins.", Colours.GREEN))
                        )
                    } else {
                        actor.reply(
                            Prefix.ADMIN()
                                .append(Prefix.ARROW3())
                                .append(Component.text("Failed to modify $name's coins.", Colours.RED))
                        )
                    }
                }
            } else {
                actor.reply(
                    Prefix.ADMIN()
                        .append(Prefix.ARROW3())
                        .append(Component.text("That player does not exist.", Colours.RED))
                )
            }
        }

    }

    @Subcommand("coins get")
    fun get(
        actor: BukkitCommandActor,
        name: String
    ) = PlayerUtility.guard(actor, Permission.CMD_ADMIN_COINS) {
        PlayerUtility.getUUID(name).thenAccept { uuid ->
            if(uuid != null) {
                val coins = Universum.coinsManager.getCoins(uuid)
                if(coins > 0) {
                    actor.reply(
                        Prefix.ADMIN()
                            .append(Prefix.ARROW3())
                            .append(Component.text("$name has $coins coins.", Colours.GREY))
                    )
                } else {
                    actor.reply(
                        Prefix.ADMIN()
                            .append(Prefix.ARROW3())
                            .append(Component.text("$name either has 0 coins or has not yet played.", Colours.GREY))
                    )
                }
            } else {
                actor.reply(
                    Prefix.ADMIN()
                        .append(Prefix.ARROW3())
                        .append(Component.text("That player does not exist.", Colours.RED))
                )
            }
        }

    }

}