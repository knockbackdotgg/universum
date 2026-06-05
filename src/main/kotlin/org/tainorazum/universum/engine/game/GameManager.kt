package org.tainorazum.universum.engine.game

import org.tainorazum.universum.engine.game.util.GameRequest
import org.tainorazum.universum.engine.game.util.OngoingGame

class GameManager {

    val ongoingGames = mutableListOf<OngoingGame>()

    fun requestGame(request: GameRequest) {

    }

    fun attemptCreateGame(request: GameRequest) : Boolean {

        try {

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

    }

    fun createGame(request: GameRequest) {

    }

}