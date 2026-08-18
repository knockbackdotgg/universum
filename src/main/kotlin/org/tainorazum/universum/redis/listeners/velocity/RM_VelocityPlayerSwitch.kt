package org.tainorazum.universum.redis.listeners.velocity

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.tainorazum.universum.network.manager.NetworkPlayerManager
import org.tainorazum.universum.redis.RedisSubscriber
import org.tainorazum.universum.redis.listeners.RM_Channel
import java.util.UUID

class RM_VelocityPlayerSwitch : RM_Channel("velocity_player_switch") {

    private val gson = Gson()

    init {
        RedisSubscriber.register("velocity_player_switch") { message ->
            try {
                val json = gson.fromJson(message, JsonObject::class.java)
                val uuid = UUID.fromString(json.get("uuid").asString)
                val name = json.get("name").asString
                val currentServer = json.get("currentServer").asString

                NetworkPlayerManager.handleVelocitySwitch(uuid, name, currentServer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}