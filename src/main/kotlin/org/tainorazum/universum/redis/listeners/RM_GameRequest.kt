package org.tainorazum.universum.redis.listeners

import com.google.gson.Gson
import org.tainorazum.universum.Universum
import org.tainorazum.universum.engine.game.util.GameRequest
import org.tainorazum.universum.redis.RedisSubscriber

class RM_GameRequest : RM_Channel("game_request") {

    init { // ex. game_request JSON

        RedisSubscriber.register(super.channel) { message ->
            try {
                val req = Gson().fromJson(message, GameRequest::class.java)
                Universum.gameManager.attemptCreateGame(req)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }

}