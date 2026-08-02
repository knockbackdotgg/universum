package org.tainorazum.universum.network.tab

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.tainorazum.universum.Universum
import org.tainorazum.universum.network.manager.NetworkPlayerManager
import org.tainorazum.universum.utility.permissions.LuckpermsUtil
import org.tainorazum.universum.utility.text.Colours
import org.tainorazum.universum.utility.text.Icon
import org.tainorazum.universum.utility.text.Text
import java.util.UUID
import java.util.concurrent.CompletableFuture

object TabManager {

    private val mm = MiniMessage.miniMessage()
    private val plainSerializer = PlainTextComponentSerializer.plainText()
    private val legacySerializer = LegacyComponentSerializer.legacyAmpersand()

    private const val MAX_WEIGHT = 999
    private const val TEAM_NAME_PREFIX = "tab_"

    /**
     * Triggers a completely asynchronous Tab update for all online players on this server.
     */
    fun update() {
        Bukkit.getScheduler().runTaskAsynchronously(Universum.instance, Runnable {
            updateAsync()
        })
    }

    private fun updateAsync() {
        val networkPlayers = NetworkPlayerManager.onlinePlayers.values.toList()
        val localOnlinePlayers = Bukkit.getOnlinePlayers()

        val header = DefaultTabStyle.header(networkPlayers.size)
        val footer = DefaultTabStyle.footer()

        val playerFutures = mutableListOf<CompletableFuture<Triple<String, Int, Component>>>()

        // Helper function to deserialize prefix properly
        fun parsePrefix(prefixRaw: String?): Component {
            val raw = prefixRaw ?: return Component.empty()
            // Parses '&' color codes like &7, &l, &f, etc.
            return legacySerializer.deserialize(raw)
        }

        // 1. Process local online players asynchronously
        localOnlinePlayers.forEach { player ->
            val future = LuckpermsUtil.getWeight(player).thenCombine(LuckpermsUtil.getPrefix(player)) { weight, prefixRaw ->
                Triple(player.name, weight, parsePrefix(prefixRaw))
            }
            playerFutures.add(future)
        }

        // 2. Process remote network players asynchronously
        networkPlayers
            .filter { np -> localOnlinePlayers.none { it.name == np.name } }
            .forEach { np ->
                val future = LuckpermsUtil.getWeight(np.uuid).thenCombine(LuckpermsUtil.getPrefix(np.uuid)) { weight, prefixRaw ->
                    Triple(np.name, weight, parsePrefix(prefixRaw))
                }
                playerFutures.add(future)
            }

        // 3. Wait for all player LuckPerms futures to finish asynchronously before applying to scoreboard
        CompletableFuture.allOf(*playerFutures.toTypedArray()).thenAccept {
            val combinedData = playerFutures.map { it.join() }

            // Switch to Bukkit main thread for rendering scoreboard teams & headers
            Bukkit.getScheduler().runTask(Universum.instance, Runnable {
                for (viewer in localOnlinePlayers) {
                    viewer.sendPlayerListHeaderAndFooter(header, footer)

                    for (data in combinedData) {
                        updateTeamEntry(viewer, data.first, data.second, data.third)
                    }
                }
            })
        }
    }

    private fun updateTeamEntry(viewer: Player, entryName: String, weight: Int, prefix: Component) {
        val scoreboard = viewer.scoreboard
        val sortOrder = (MAX_WEIGHT - weight).coerceIn(0, MAX_WEIGHT).toString().padStart(3, '0')
        val teamName = "$TEAM_NAME_PREFIX${sortOrder}_$entryName"

        scoreboard.getEntryTeam(entryName)?.let { oldTeam ->
            if (oldTeam.name != teamName) {
                oldTeam.removeEntry(entryName)
                if (oldTeam.entries.isEmpty()) oldTeam.unregister()
            }
        }

        val team = scoreboard.getTeam(teamName) ?: scoreboard.registerNewTeam(teamName)

        val hasText = plainSerializer.serialize(prefix).isNotBlank()
        val finalPrefix = if (hasText) prefix.append(Component.text(" ")) else prefix

        if (team.prefix() != finalPrefix) {
            team.prefix(finalPrefix)
        }

        team.color(NamedTextColor.GRAY)

        if (!team.hasEntry(entryName)) {
            team.addEntry(entryName)
        }
    }

    /**
     * Asynchronously cleans up a player's scoreboard team entry on quit.
     */
    fun leave(player: Player) {
        LuckpermsUtil.getWeight(player).thenAccept { weight ->
            val teamName = getTeamName(weight, player.name)
            Bukkit.getScheduler().runTask(Universum.instance, Runnable {
                Bukkit.getScoreboardManager().mainScoreboard.getTeam(teamName)?.removeEntry(player.name)
            })
        }
    }

    private fun getTeamName(weight: Int, identifier: String): String {
        val sortOrder = (MAX_WEIGHT - weight).coerceIn(0, MAX_WEIGHT).toString().padStart(3, '0')
        return "$TEAM_NAME_PREFIX${sortOrder}_$identifier"
    }
}

private object DefaultTabStyle {
    fun header(totalPlayers: Int): Component {
        return Component.text("")
            .append(Text.NEXT_LINE())
            .append(Text.toSmallTextComponent("knockback.gg ", Colours.GREEN))
            .append(Text.NEXT_LINE())
            .append(Text.NEXT_LINE())
            .append(Component.text(totalPlayers, Colours.GREY))
            .append(Text.toSmallTextComponent(if (totalPlayers == 1) " player" else " players", Colours.GREY))
    }

    fun footer(): Component {
        return Component.text("")
            .append(Text.NEXT_LINE())
            .append(Component.text("🗡", Colours.GREEN))
            .append(Text.NEXT_LINE())
    }
}