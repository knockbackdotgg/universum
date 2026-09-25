package org.tainorazum.universum.engine.game.manager

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import org.bukkit.Bukkit
import org.tainorazum.universum.Universum
import org.tainorazum.universum.engine.game.games.child.Duel
import org.tainorazum.universum.engine.game.games.child.Ffa
import org.tainorazum.universum.engine.game.games.parent.Game
import org.tainorazum.universum.engine.game.manager.fields.GameRequest
import org.tainorazum.universum.engine.game.manager.fields.GameType
import org.tainorazum.universum.redis.RedisManager
import java.util.UUID
import java.util.concurrent.CompletableFuture

class GameManager {

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(UUID::class.java, JsonSerializer<UUID> { src, _, _ ->
            JsonPrimitive(src.toString())
        })
        .registerTypeAdapter(UUID::class.java, JsonDeserializer { json, _, _ ->
            UUID.fromString(json.asString)
        })
        .create()
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
        return Universum.arenaManager.retrieveArena(request.arenaName).thenApplyAsync({ arena ->
            if (arena == null) {
                Universum.instance.logger.warning("[GameManager] Could not retrieve arena: ${request.arenaName}")
                return@thenApplyAsync null
            }

            try {
                val game: Game? = when (request.gameType) {
                    GameType.KIT_FFA -> Ffa(arena, request.teams, request.gameType, request.gameSettings)
                    GameType.KIT_DUEL -> Duel(arena, request.teams, request.gameType, request.gameSettings)
                    GameType.SPLEEF -> null
                    GameType.BRIDGE -> null
                    GameType.KOTH -> null
                }

                if (game != null) {
                    games.add(game)
                }
                game
            } catch (e: Exception) {
                Universum.instance.logger.severe("[GameManager] Exception while instantiating game:")
                e.printStackTrace()
                null
            }
        }, { runnable -> Bukkit.getScheduler().runTask(Universum.instance, runnable) })
            .exceptionally { ex ->
                Universum.instance.logger.severe("[GameManager] Error creating game future:")
                ex.printStackTrace()
                null
            }
    }

}