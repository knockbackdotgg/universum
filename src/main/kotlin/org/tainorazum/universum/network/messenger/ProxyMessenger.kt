package org.tainorazum.universum.network.messenger

import com.google.gson.Gson
import org.tainorazum.universum.redis.RedisManager
import java.util.UUID

object ProxyMessenger {

    private val gson = Gson()

    fun sendPlayerToServer(playerUuid: UUID, targetServer: String) {
        val payload = mapOf(
            "uuid" to playerUuid.toString(),
            "targetServer" to targetServer
        )
        RedisManager.publish("velocity_send_player", gson.toJson(payload))
    }
}