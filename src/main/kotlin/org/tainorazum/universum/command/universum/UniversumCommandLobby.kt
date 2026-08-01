package org.tainorazum.universum.command.universum

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.tainorazum.universum.Universum
import org.tainorazum.universum.lobby.database.DB_Lobby
import org.tainorazum.universum.utility.player.PlayerUtility
import org.tainorazum.universum.utility.text.Colours
import org.tainorazum.universum.utility.text.Prefix
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Optional
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.bukkit.actor.BukkitCommandActor

@Command("universum", "uni", "u")
class UniversumCommandLobby : UniversumCommandGeneral(UniversumCommandSub.TEST) {

    override val tutorial : List<String> = mutableListOf(
        "/universum lobby - Receive this help message.",
        "/universum lobby setSpawn <worldName> <x> <y> <z> <yaw> <pitch> - Set the server/lobby's spawn to said location.",
        "/universum lobby setSchematic - Set the lobby's schematic to your current FAWE clipboard schematic."
    )

    @Subcommand("setSchematic")
    fun setSchematic(
        actor: BukkitCommandActor
    ) {
        if(!allowed(actor)) return

        val player = actor.requirePlayer()
        DB_Lobby.setSchematic(player).thenAccept { success ->
            if(success) {
                player.sendMessage(Prefix.UNIVERSUM().append(Prefix.ARROW3()).append(Component.text("Successfully saved lobby schematic.", Colours.GREEN)))
            } else {
                player.sendMessage(Prefix.UNIVERSUM().append(Prefix.ARROW3()).append(Component.text("Failed to save schematic. You don't have a schematic copied.", Colours.RED)))
            }
        }

    }

    @Subcommand("setSpawn")
    fun setSpawn(
        actor: BukkitCommandActor,
        worldName: String,
        x: Double,
        y: Double,
        z: Double,
        yaw: Float,
        pitch: Float,
    ) {
        if(!allowed(actor)) return

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