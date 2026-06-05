package org.tainorazum.universum.engine.game.util

import org.tainorazum.universum.engine.game.Game

data class OngoingGame(
    val game: Game,
    val serverIdentifier: String,

    )