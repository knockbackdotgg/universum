package org.tainorazum.universum.game.gametypes

import org.bukkit.entity.Player
import org.tainorazum.universum.game.Game
import org.tainorazum.universum.game.GameType
import org.tainorazum.universum.game.arena.Arena
import java.util.UUID

class FFA(
    arena: Arena
): Game(GameType.FFA, arena) {

    private val players: MutableList<Player> = mutableListOf()

    override fun start() {
        arena.reset()
    }

    init {

    }

}