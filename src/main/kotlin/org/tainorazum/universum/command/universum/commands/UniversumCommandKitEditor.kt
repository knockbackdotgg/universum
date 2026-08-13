package org.tainorazum.universum.command.universum.commands

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.tainorazum.universum.command.universum.UniversumCommandSub
import org.tainorazum.universum.kiteditor.database.DB_KitEditor
import org.tainorazum.universum.security.Permission
import org.tainorazum.universum.utility.player.PlayerUtility
import org.tainorazum.universum.utility.text.Colours
import org.tainorazum.universum.utility.text.Prefix
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.bukkit.actor.BukkitCommandActor

@Command("universum", "uni", "u")
class UniversumCommandKitEditor : UniversumCommandGeneral(UniversumCommandSub.KITEDITOR) {

    override val tutorial : List<Pair<String, String>> = mutableListOf(
        Pair("/universum kitEditor", "Receive this help message."),
        Pair("/universum kitEditor setSpawn <worldName> <x> <y> <z> <yaw> <pitch>", " Set the server/kit editor's spawn to said location."),
        Pair("/universum kitEditor setSchematic", " Set the kit editor's schematic to your current FAWE clipboard schematic.")
    )

    @Subcommand("kitEditor setSchematic")
    fun setSchematic(
        actor: BukkitCommandActor
    ) = PlayerUtility.guard(actor, Permission.CMD_UNIVERSUM_KITEDITOR) {

        val player = actor.requirePlayer()
        DB_KitEditor.setSchematic(player).thenAccept { success ->
            if(success) {
                player.sendMessage(Prefix.UNIVERSUM().append(Prefix.ARROW3()).append(Component.text("Successfully saved kit editor schematic.", Colours.GREEN)))
            } else {
                player.sendMessage(Prefix.UNIVERSUM().append(Prefix.ARROW3()).append(Component.text("Failed to save schematic. You don't have a schematic copied.", Colours.RED)))
            }
        }

    }

    @Subcommand("kitEditor setSpawn")
    fun setSpawn(
        actor: BukkitCommandActor,
        worldName: String,
        x: Double,
        y: Double,
        z: Double,
        yaw: Float,
        pitch: Float,
    ) = PlayerUtility.guard(actor, Permission.CMD_UNIVERSUM_KITEDITOR) {

        val player = actor.requirePlayer()
        var location: Location? = null
        try {
            location = Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch)
        } catch (e: Exception) {
            player.sendMessage(Prefix.UNIVERSUM().append(Prefix.ARROW3()).append(Component.text("Invalid location, please try again.", Colours.RED)))
        }

        if(location != null) {
            DB_KitEditor.setSpawnLocation(location)
            player.sendMessage(Prefix.UNIVERSUM().append(Prefix.ARROW3()).append(Component.text("Kit editor spawn location has been set.", Colours.GREEN)))
        }

    }

}