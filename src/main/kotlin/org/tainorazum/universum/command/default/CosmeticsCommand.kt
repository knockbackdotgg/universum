package org.tainorazum.universum.command.default

import org.tainorazum.universum.Universum
import org.tainorazum.universum.security.Permission
import org.tainorazum.universum.utility.player.PlayerUtility
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.CommandPlaceholder
import revxrsal.commands.bukkit.actor.BukkitCommandActor

@Command("cosmetics", "cosmetic")
class CosmeticsCommand {

    @CommandPlaceholder
    fun cosmetics(
        actor: BukkitCommandActor
    ) = PlayerUtility.guard(actor, Permission.CMD_DEFAULT_COSMETICS, true) {

        val player = actor.requirePlayer()
        //Universum.cosmeticManager.openGUI(player)

    }

}