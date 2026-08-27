package org.tainorazum.universum.engine.game.manager

import com.google.gson.Gson
import org.tainorazum.universum.Universum
import org.tainorazum.universum.engine.arena.ArenaManager
import org.tainorazum.universum.engine.game.games.parent.Game
import org.tainorazum.universum.engine.game.manager.fields.GameRequest
import org.tainorazum.universum.redis.RedisManager
import java.util.concurrent.CompletableFuture

class GameManager {

    private val gson = Gson()
    private val games = mutableListOf<Game>()

    fun requestGame(request: GameRequest): CompletableFuture<Boolean> {
        val key = "game_request:${request.id}"
        val json = gson.toJson(request)

        return RedisManager.set(key, json, 10L).thenCompose { setSuccess ->
            if (setSuccess) {
                RedisManager.publish("game_request", request.id).thenApply { true }
            } else {
                CompletableFuture.completedFuture(false)
            }
        }
    }

    fun claimRequest(requestId: String): CompletableFuture<GameRequest?> {
        val key = "game_request:$requestId"

        return RedisManager.getdel(key).thenApply { json ->
            if (json != null) gson.fromJson(json, GameRequest::class.java) else null
        }
    }

    fun createGame(request: GameRequest): CompletableFuture<Game?> {
        val future = CompletableFuture<Game?>()

        val arenaFuture = Universum.arenaManager.retrieveArena(request.arenaName)

        CompletableFuture.allOf(arenaFuture).thenAccept {
            val arena = arenaFuture.get()

            if(arena == null) {
                future.complete(null)
                return@thenAccept
            }

            val game = Game(
                arena, request.teams, request.gameType, request.gameSettings
            )

            games.add(game)
            future.complete(game)
        }
        return future
    }

}