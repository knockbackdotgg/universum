package org.tainorazum.universum.engine.game.util

import org.tainorazum.universum.engine.game.Game
import org.tainorazum.universum.server.server.Server

data class OngoingGame(
    val game: Game,
    val server: Server
)