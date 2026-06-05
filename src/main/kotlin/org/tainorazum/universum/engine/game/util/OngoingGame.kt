package org.tainorazum.universum.engine.game.util

import com.sun.jmx.remote.protocol.rmi.ServerProvider
import org.tainorazum.universum.engine.game.Game
import org.tainorazum.universum.server.Server

data class OngoingGame(
    val game: Game,
    val server: Server
)