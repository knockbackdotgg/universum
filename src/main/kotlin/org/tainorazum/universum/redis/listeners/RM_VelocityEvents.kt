package org.tainorazum.universum.redis.listeners

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.tainorazum.universum.network.manager.NetworkPlayerManager
import org.tainorazum.universum.redis.RedisSubscriber
import java.util.UUID

class RM_VelocityEvents : RM_Channel("velocity_player_disconnect") {

    private val gson = Gson()

    init {
        // Handle network disconnects
        RedisSubscriber.register("velocity_player_disconnect") { message ->
            try {
                val json = gson.fromJson(message, JsonObject::class.java)
                val uuid = UUID.fromString(json.get("uuid").asString)

                NetworkPlayerManager.handleTrueNetworkQuit(uuid)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Handle server switches / proxy joins
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