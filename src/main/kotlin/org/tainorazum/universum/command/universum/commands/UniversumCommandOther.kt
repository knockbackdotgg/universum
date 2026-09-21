package org.tainorazum.universum.command.universum.commands

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Team
import org.tainorazum.universum.Universum
import org.tainorazum.universum.command.universum.UniversumCommandSub
import org.tainorazum.universum.engine.game.manager.fields.GameRequest
import org.tainorazum.universum.engine.game.manager.fields.GameSettings
import org.tainorazum.universum.engine.game.manager.fields.GameTeam
import org.tainorazum.universum.engine.game.manager.fields.GameType
import org.tainorazum.universum.engine.kit.Kit
import org.tainorazum.universum.engine.statistics.database.DB_Stats
import org.tainorazum.universum.network.manager.NetworkPlayerManager
import org.tainorazum.universum.redis.RedisManager
import org.tainorazum.universum.security.Permission
import org.tainorazum.universum.utility.player.PlayerUtility
import org.tainorazum.universum.utility.text.Colours
import org.tainorazum.universum.utility.text.Prefix
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Optional
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.annotation.Suggest
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import java.util.concurrent.CompletableFuture

@Command("universum", "uni", "u")
class UniversumCommandOther : UniversumCommandGeneral(UniversumCommandSub.OTHER) {

    override val tutorial : List<Pair<String, String>> = mutableListOf(
        Pair("/universum other", " Receive this help message."),
        Pair("/universum other blackout <duration> <fadeOut> <title> <subTitle>", " Give yourself a temporary screen blackout (feature test)."),
        Pair("/universum other statistic <id> <gameType> <ranked> <value>", "Set ayour own statistics in order to test."),
        Pair("/universum other statistic <id> <gameType> <ranked> <value> <player>", "Set a player's statistics in order to test."),
        Pair("/universum other playerCount reset", "Reset Redis' player count."),
        Pair("/universum other game request <player1> <player2> <gameType> <kit> <map> <rounds> <isRanked> <canModifyBlocks", "Force request a game.")
    )

    @Subcommand("other game request")
    fun gameRequest(
        actor: BukkitCommandActor,
        player1: Player,
        player2: Player,
        type: GameType,
        kit: Kit,
        @Suggest("<map>")map: String,
        @Suggest("<rounds>", "1", "3", "5", "10")rounds: Int,
        @Suggest("<isRanked>", "true", "false")isRanked: Boolean,
        @Suggest("<canModifyBlocks>", "true", "false")canModifyBlocks: Boolean,
        @Suggest("<canModifyMap>", "true", "false")canModifyMap: Boolean,
    ) = PlayerUtility.guard(actor, Permission.CMD_UNIVERSUM_OTHER) {

        if(!player1.isOnline || !player2.isOnline) {
            actor.reply(Prefix.UNIVERSUM().append(Prefix.ARROW3()).append(Component.text("One of or both of the supplied players are not online on this server.", Colours.RED)))
            return@guard
        }

        val settings = GameSettings(
            kit, rounds, isRanked, canModifyBlocks, canModifyMap,
        )

        val teams = listOf(
            GameTeam("1", mutableListOf(player1.uniqueId)),
            GameTeam("2", mutableListOf(player2.uniqueId))
        )

        val request = GameRequest(
            gameType = type,
            gameSettings = settings,
            teams = teams,
            arenaName = map
        )

        Universum.gameManager.requestGame(request).thenAccept { success ->
            if(success) {
                actor.reply(Prefix.UNIVERSUM().append(Prefix.ARROW3().append(Component.text("Game has been successfully requested",
                    Colours.GREEN))))
            }
            else {
                actor.reply(Prefix.UNIVERSUM().append(Prefix.ARROW3().append(Component.text("Failed to request game",
                    Colours.RED))))
            }
        }

    }

    @Subcommand("other playerCount reset")
    fun playerCount(
        actor: BukkitCommandActor
    ) = PlayerUtility.guard(actor, Permission.CMD_UNIVERSUM_OTHER) {

        actor.reply(Prefix.UNIVERSUM().append(Prefix.ARROW3()).append(Component.text("Attempting to reset the player count saved in Redis...", Colours.GREY)))

        RedisManager.del("network:players").thenAccept { result ->
            NetworkPlayerManager.onlinePlayers.clear()

            actor.reply(Prefix.UNIVERSUM().append(Prefix.ARROW3()).append(Component.text("Successfully reset the player count saved in Redis.", Colours.GREEN)))
        }
    }

    @Subcommand("other statistic")
    fun statistic(
        actor: BukkitCommandActor,
        id: String,
        gameType: GameType,
        @Suggest("true", "false") ranked: Boolean,
        @Suggest("1", "2", "3", "50", "100", "500") value: Double,
        @Optional targetName: String? = null
    ) = PlayerUtility.guard(actor, Permission.CMD_UNIVERSUM_OTHER) {

        val uuid = actor.uniqueId()
        if(targetName != null) {
            PlayerUtility.getUUID(targetName).thenAccept { targetUUID ->
                DB_Stats.set(id, gameType, targetUUID.toString(), value, ranked).thenAccept { success ->
                    if (success) {
                        actor.reply(Prefix.UNIVERSUM().append(Prefix.ARROW3()).append(Component.text("Player $targetName's statistics have been successfully updated.", Colours.GREEN)))
                    } else {
                        actor.reply(Prefix.UNIVERSUM().append(Prefix.ARROW3()).append(Component.text("Failed to update player $targetName's statistics.", Colours.RED)))
                    }
                }
            }
        } else {
            DB_Stats.set(id, gameType, uuid.toString(), value, ranked).thenAccept { success ->
                if (success) {
                    actor.reply(Prefix.UNIVERSUM().append(Prefix.ARROW3()).append(Component.text("Your statistics have been successfully updated.", Colours.GREEN)))
                } else {
                    actor.reply(Prefix.UNIVERSUM().append(Prefix.ARROW3()).append(Component.text("Failed to update your statistics.", Colours.RED)))
                }
            }
        }



    }

    @Subcommand("other blackout")
    fun blackout(
        actor: BukkitCommandActor,
        duration: Int,
        fadeOut: Int,
        titleRaw: String,
        subTitleRaw: String,
    ) = PlayerUtility.guard(actor, Permission.CMD_UNIVERSUM_OTHER) {

        val player = actor.requirePlayer()
        player.sendMessage(Component.newline())
        player.sendMessage(Prefix.UNIVERSUM().append(Prefix.ARROW3()).append(Component.text("Attempting screen blackout...", Colours.GREEN)))
        player.sendMessage(Prefix.UNIVERSUM().append(Prefix.ARROW3()).append(Component.text("Duration: $duration ms", Colours.GREEN)))
        player.sendMessage(Prefix.UNIVERSUM().append(Prefix.ARROW3()).append(Component.text("Fade out: $fadeOut ms", Colours.GREEN)))
        player.sendMessage(Prefix.UNIVERSUM().append(Prefix.ARROW3()).append(Component.text("Title text: $titleRaw", Colours.GREEN)))
        player.sendMessage(Prefix.UNIVERSUM().append(Prefix.ARROW3()).append(Component.text("Subtitle text: $subTitleRaw", Colours.GREEN)))
        player.sendMessage(Component.newline())

        val title = Component.text(titleRaw, Colours.GREEN)
        val subTitle = Component.text(subTitleRaw, Colours.GREY)
        PlayerUtility.blackout(player, duration, fadeOut, title, subTitle)

        player.sendMessage(Prefix.UNIVERSUM().append(Prefix.ARROW3()).append(Component.text("Screen blackout sent.", Colours.GREEN)))

    }

}