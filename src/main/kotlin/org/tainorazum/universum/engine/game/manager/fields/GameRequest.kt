package org.tainorazum.universum.engine.game.manager.fields

import org.tainorazum.universum.engine.kit.Kit
import org.tainorazum.universum.redis.RedisManager

data class GameRequest(

    val id: String = RedisManager.generateRequestId(),

    val kit: Kit? = null,

    val gameType: GameType,
    val gameSettings: GameSettings,

    val teams: List<GameTeam>,

    val arenaName: String,
    )