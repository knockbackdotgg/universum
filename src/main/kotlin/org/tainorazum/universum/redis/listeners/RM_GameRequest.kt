package org.tainorazum.universum.redis.listeners

import com.google.gson.Gson
import org.bukkit.Bukkit
import org.tainorazum.universum.Universum
import org.tainorazum.universum.engine.game.util.GameRequest
import org.tainorazum.universum.redis.RedisSubscriber

class RM_GameRequest : RM_Channel("game_request") {

    private val gson = Gson()

    init {
        RedisSubscriber.register(channel) { message ->
            try {
                // 1. Deserialize on the async worker thread (Fast)
                val req = gson.fromJson(message, GameRequest::class.java)

                // 2. Schedule game creation / Bukkit mutations on the main thread
                Bukkit.getScheduler().runTask(Universum.instance, Runnable {
                    Universum.gameManager.attemptCreateGame(req)
                })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}