package org.tainorazum.universum.engine.game.manager.fields

import org.tainorazum.universum.redis.RedisManager

data class GameRequest(

    val id: String = RedisManager.generateRequestId(),

    val gameType: GameType,
    val gameSettings: GameSettings,

    val teams: List<GameTeam>,

    val arenaName: String,
    )