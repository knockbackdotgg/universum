package org.tainorazum.universum.engine.game.util

import org.tainorazum.universum.engine.game.gametypes.GameType
import kotlin.uuid.Uuid

data class GameRequest(
    val identifier: String,
    val gameType: GameType,
)
