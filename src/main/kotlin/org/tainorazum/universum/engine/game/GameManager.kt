package org.tainorazum.universum.engine.game

import com.google.gson.Gson
import org.tainorazum.universum.Universum
import org.tainorazum.universum.engine.game.util.GameRequest
import org.tainorazum.universum.engine.game.util.OngoingGame
import org.tainorazum.universum.redis.RedisManager
import org.tainorazum.universum.server.server.Server
import java.util.concurrent.CompletableFuture

class GameManager {

    val ongoingGames = mutableMapOf<Server, MutableList<OngoingGame>>()

    fun requestGame(request: GameRequest) {
        val json = Gson().toJson(request)
        RedisManager.publish("game_request", json)
    }

    fun attemptCreateGame(request: GameRequest) : Boolean {
        try {
            createGame(request)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

    }

    fun createGame(request: GameRequest) : CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()

        Universum.arenaManager.retrieveArena(request.arenaName).thenAccept { arena ->
            if(arena != null) {
                val game = Game(request.gameType, arena)
                val ongoingGame = OngoingGame(game, Universum.server)

                registerOngoingGame(Universum.server, ongoingGame)
                future.complete(true)
            } else {
                future.complete(false)
            }
        }

        return future
    }

    private fun registerOngoingGame(server: Server, game: OngoingGame) : Boolean {
        return ongoingGames[server]?.add(game) ?: false
    }

    /**
    * @return True if game already exists and removes, false if game does not exist.
     */
    private fun unregisterOngoingGame(server: Server, game: OngoingGame) : Boolean {
        return if(ongoingGames[server]?.contains(game) ?: false) {
            ongoingGames[server]?.remove(game)
            true
        } else false
    }

}