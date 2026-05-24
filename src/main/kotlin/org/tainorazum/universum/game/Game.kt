package org.tainorazum.universum.game

import gg.flyte.twilight.event.event
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.event.entity.PlayerDeathEvent
import org.tainorazum.universum.game.arena.Arena
import java.util.UUID

open class Game(
    val gameType: GameType,
    val arena: Arena
) {

    open fun start() {

    }

    fun updateStatistics(uuid: UUID) {

    }

    init {

    }

}