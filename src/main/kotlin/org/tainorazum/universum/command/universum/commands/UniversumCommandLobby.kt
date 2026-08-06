package org.tainorazum.universum.command.universum.commands

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.tainorazum.universum.command.universum.UniversumCommandSub
import org.tainorazum.universum.lobby.database.DB_Lobby
import org.tainorazum.universum.security.Permission
import org.tainorazum.universum.utility.player.PlayerUtility
import org.tainorazum.universum.utility.text.Colours
import org.tainorazum.universum.utility.text.Prefix
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.bukkit.actor.BukkitCommandActor

@Command("universum", "uni", "u")
class UniversumCommandLobby : UniversumCommandGeneral(UniversumCommandSub.LOBBY) {

    override val tutorial : List<Pair<String, String>> = mutableListOf(
        Pair("/universum lobby", " Receive this help message."),
        Pair("/universum lobby setSpawn <worldName> <x> <y> <z> <yaw> <pitch>", " Set the server/lobby's spawn to said location."),
        Pair("/universum lobby setSchematic", " Set the lobby's schematic to your current FAWE clipboard schematic.")
    )

    @Subcommand("lobby setSchematic")
    fun setSchematic(
        actor: BukkitCommandActor
    ) = PlayerUtility.guard(actor, Permission.CMD_UNIVERSUM_LOBBY) {

        val player = actor.requirePlayer()
        DB_Lobby.setSchematic(player).thenAccept { success ->
            if(success) {
                player.sendMessage(Prefix.UNIVERSUM().append(Prefix.ARROW3()).append(Component.text("Successfully saved lobby schematic.", Colours.GREEN)))
            } else {
                player.sendMessage(Prefix.UNIVERSUM().append(Prefix.ARROW3()).append(Component.text("Failed to save schematic. You don't have a schematic copied.", Colours.RED)))
            }
        }

    }

    @Subcommand("lobby setSpawn")
    fun setSpawn(
        actor: BukkitCommandActor,
        worldName: String,
        x: Double,
        y: Double,
        z: Double,
        yaw: Float,
        pitch: Float,
    ) = PlayerUtility.guard(actor, Permission.CMD_UNIVERSUM_LOBBY) {

        val player = actor.requirePlayer()
        var location: Location? = null
        try {
            location = Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch)
        } catch (e: Exception) {
            player.sendMessage(Prefix.UNIVERSUM().append(Prefix.ARROW3()).append(Component.text("Invalid location, please try again.", Colours.RED)))
        }

        if(location != null) {
            DB_Lobby.setSpawnLocation(location)
            player.sendMessage(Prefix.UNIVERSUM().append(Prefix.ARROW3()).append(Component.text("Lobby spawn location has been set.", Colours.GREEN)))
        }

    }

}