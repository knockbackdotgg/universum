package org.tainorazum.universum.command.admin.commands

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.tainorazum.universum.Universum
import org.tainorazum.universum.command.admin.AdminCommandSub
import org.tainorazum.universum.engine.arena.admin.AdminArenasGui
import org.tainorazum.universum.engine.arena.database.ArenaDatabaseManager
import org.tainorazum.universum.engine.arena.gui.admin.utility.AdminArenaEditGui
import org.tainorazum.universum.security.Permission
import org.tainorazum.universum.utility.player.PlayerUtility
import org.tainorazum.universum.utility.text.Colours
import org.tainorazum.universum.utility.text.Prefix
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.annotation.Suggest
import revxrsal.commands.bukkit.actor.BukkitCommandActor

@Command("administrator", "admin", "a")
class AdminCommandGames : AdminCommandGeneral(AdminCommandSub.GAMES) {

    private val dbManager = ArenaDatabaseManager()

    override val tutorial: List<Pair<String, String>> = mutableListOf(
        Pair("/admin games", "Opens the games management gui."),
        Pair("/admin games game list", "Show all games and their play status."),
        Pair("/admin games game enable", "Enable a game."),
        Pair("/admin games game disable", "Disable a game."),
        Pair("/admin games arena", "Open the arena management gui"),
        Pair("/admin games arena create <identifier>", "Create a new arena template."),
        Pair("/admin games arena setSchematic <identifier>", "Set arena schematic from FAWE clipboard."),
        Pair("/admin games arena list", "Show all arenas and their attributes."),
    )

    @Subcommand("games")
    fun games(
        actor: BukkitCommandActor,
    ) = PlayerUtility.guard(actor, Permission.CMD_ADMIN_GAMES) {
        val player = actor.requirePlayer()
        // AdminGamesGui.openGui(player)
    }

    @Subcommand("games game list")
    fun gameList(
        actor: BukkitCommandActor,
    ) = PlayerUtility.guard(actor, Permission.CMD_ADMIN_GAMES) {
    }

    @Subcommand("games game enable")
    fun gameEnable(
        actor: BukkitCommandActor,
    ) = PlayerUtility.guard(actor, Permission.CMD_ADMIN_GAMES) {
    }

    @Subcommand("games game disable")
    fun gameDisable(
        actor: BukkitCommandActor,
    ) = PlayerUtility.guard(actor, Permission.CMD_ADMIN_GAMES) {
    }

    @Subcommand("games arena")
    fun arena(
        actor: BukkitCommandActor,
    ) = PlayerUtility.guard(actor, Permission.CMD_ADMIN_GAMES) {
        val player = actor.requirePlayer()
        AdminArenasGui.openGUI(player)
    }

    @Subcommand("games arena create")
    fun arenaCreate(
        actor: BukkitCommandActor,
        @Suggest("<identifier>") identifier: String,
    ) = PlayerUtility.guard(actor, Permission.CMD_ADMIN_GAMES) {
        val player = actor.requirePlayer()
        val cleanedId = identifier.lowercase().trim()

        dbManager.createArena(cleanedId).thenAccept { success ->
            Bukkit.getScheduler().runTask(Universum.instance, Runnable {
                if (success) {
                    player.sendMessage(Prefix.ADMIN()
                        .append(Prefix.ARROW3())
                        .append(Component.text("Created arena template '$cleanedId'!", Colours.GREEN)))
                    AdminArenaEditGui.open(player, cleanedId)
                } else {
                    player.sendMessage(Prefix.ADMIN()
                        .append(Prefix.ARROW3())
                        .append(Component.text("Arena with ID '$cleanedId' already exists or failed to create!", Colours.RED)))
                }
            })
        }
    }

    @Subcommand("games arena setSchematic")
    fun arenaSetSchematic(
        actor: BukkitCommandActor,
        @Suggest("<identifier>") identifier: String,
    ) = PlayerUtility.guard(actor, Permission.CMD_ADMIN_GAMES) {
        val player = actor.requirePlayer()
        val cleanedId = identifier.lowercase().trim()

        dbManager.saveClipboardToArena(player, cleanedId).thenAccept { success ->
            Bukkit.getScheduler().runTask(Universum.instance, Runnable {
                if (success) {
                    player.sendMessage(Prefix.ADMIN()
                        .append(Prefix.ARROW3())
                        .append(Component.text("Saved FAWE clipboard as schematic for arena '$cleanedId'!", Colours.GREEN)))
                } else {
                    player.sendMessage(Prefix.ADMIN()
                        .append(Prefix.ARROW3())
                        .append(Component.text("Failed to save schematic. Make sure you copied an area with WorldEdit first!", Colours.GREEN)))
                }
            })
        }
    }

    @Subcommand("games arena list")
    fun arenaList(
        actor: BukkitCommandActor,
    ) = PlayerUtility.guard(actor, Permission.CMD_ADMIN_GAMES) {
        val player = actor.requirePlayer()
        AdminArenasGui.openGUI(player)
    }
}