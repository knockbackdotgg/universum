package org.tainorazum.universum.engine.game.gametypes

import gg.flyte.twilight.Twilight.Companion.plugin
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.entity.PlayerDeathEvent
import org.tainorazum.universum.engine.game.Game
import org.tainorazum.universum.engine.arena.Arena
import org.tainorazum.universum.engine.kit.Kit

class FFA(
    arena: Arena,
    kit: Kit
): Game(GameType.FFA, arena) {

    private val players: MutableList<Player> = mutableListOf()

    // game

    override fun start() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, Runnable {
            arena.reset()
        }, 50, 5 * 60 * 20L)
    }

    override fun isPlayerInGame(player: Player): Boolean {
        return players.contains(player)
    }

    // manager

    fun addPlayer(player: Player) {
        players.add(player)

    }

    fun removePlayer(player: Player) {
        players.remove(player)
    }

    // events

    override fun PLAYER_DEATH_EVENT(event: PlayerDeathEvent) {
        super.PLAYER_DEATH_EVENT(event)
    }



}
