package org.tainorazum.universum.redis.listeners.velocity

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.tainorazum.universum.network.manager.NetworkPlayerManager
import org.tainorazum.universum.redis.RedisSubscriber
import org.tainorazum.universum.redis.listeners.RM_Channel
import java.util.UUID

class RM_VelocityPlayerDisconnect : RM_Channel("velocity_player_disconnect") {

    private val gson = Gson()

    init {
        RedisSubscriber.register("velocity_player_disconnect") { message ->
            try {
                val json = gson.fromJson(message, JsonObject::class.java)
                val uuid = UUID.fromString(json.get("uuid").asString)

                NetworkPlayerManager.handleTrueNetworkQuit(uuid)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}